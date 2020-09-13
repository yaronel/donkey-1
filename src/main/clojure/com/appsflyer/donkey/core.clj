(ns com.appsflyer.donkey.core
  (:require [clojure.set]
            [clojure.spec.alpha :as spec]
            [com.appsflyer.donkey.server :as server]
            [com.appsflyer.donkey.client :as client]
            [com.appsflyer.donkey.metrics :as metrics]
            [com.appsflyer.donkey.middleware.params :as middleware]
            [com.appsflyer.donkey.donkey-spec :as donkey-spec])
  (:import (com.appsflyer.donkey.server Server DonkeyServer)
           (com.appsflyer.donkey.client.ring RingClient)
           (io.vertx.core Vertx VertxOptions)
           (io.vertx.core.impl.cpu CpuCoreSensor)))

(comment
  ;;; Server API
  {:port                 8080
   :compression          false
   :host                 "0.0.0.0"
   :metrics-enabled      true
   :metric-registry      nil
   :metrics-prefix       "donkey"
   :worker-threads       20
   :event-loops          16
   :debug                false
   :idle-timeout-seconds 0
   :middleware           []
   :routes               []}

  ;;; Route API
  {:methods      [:get :post]
   :consumes     ["application/json" "application/x-www-form-urlencoded" "text/plain"]
   :produces     ["application/json" "text/plain"]
   :handler-mode :non-blocking
   :handler      (fn [req respond raise] (respond {:status 200}))
   :middleware   [(fn [handler] (fn [req respond raise]
                                  (-> req handler identity respond)))]
   :path         "/foo"
   :match-type   :simple})

(comment
  ;;; Client API
  {:keep-alive                 false
   :keep-alive-timeout-seconds 60
   :debug                      false
   :idle-timeout-seconds       0
   :connect-timeout-seconds    60
   :default-port               80
   :default-host               "localhost"
   :max-redirects              16
   :proxy-type                 {:host "localhost"
                                :port 3128
                                :type :http|:sock4|:sock5}
   :compression                false
   :ssl                        false}
  )

(defprotocol IDonkey
  (create-server [_this opts]
    "Create an instance of DonkeyServer with the supplied options.
    `opts` map description (all values are optional unless stated otherwise):

    :port [int] Required. The port the server will listen to.

    :instances [int] The number of server instances to deploy. Each instance
      is assigned to an event loop. Therefore there is a direct relation between
      the number of :event-loops set when creating a Donkey instance, and the
      number of server instances. If the number of server instances is greater
      than the number of event loops then some will have to share.
      Also note that the function returns a *single* DonkeyServer. The number
      of instances only determines the amount of concurrency that will be used
      to serve requests.

    :middleware [fn [,fn]*] Sequence of functions that will be applied before
      a request is processed by a handler. Each function should accept a handler
      function as its only argument, and return a function that accepts 1 or 3
      arguments (blocking vs. non blocking mode). The middleware is responsible
      calling the handler before or after the handler processes the request.

    :compression [boolean] Include support for gzip  request / response inflate /
      deflate. Defaults to false.

    :host [string] The hostname or ip the server will listen to incoming
      connections. Defaults to '0.0.0.0'.

    :idle-timeout-seconds [int] Set the duration of time in seconds where a
      connection will timeout and be closed if no data is received. Defaults to
      never.

    :debug [boolean] Enable debug mode. Debug mode is not suitable for
      production use since it outputs a large amount of logs. Use with
      discretion. Defaults to false.

    :date-header [boolean] Include the 'Date' header in the response. Defaults
      to false.

    :server-header [boolean] Include the 'Server' header in the response.
      Defaults to false.

    :content-type-header [boolean] Sets the response content type automatically
      according to the best 'Accept' header match. Defaults to false.
    ")
  (create-client [_this] [_this opts]
    "Create an instance of DonkeyClient with the supplied options"))

(deftype Donkey [^Vertx vertx]
  IDonkey
  (create-server [_this opts]
    (-> (spec/assert ::donkey-spec/server-config opts)
        (assoc :vertx vertx)
        server/get-server-config
        Server/create
        server/->DonkeyServer))
  (create-client [this]
    (create-client this {}))
  (create-client [_this opts]
    (-> (spec/assert ::donkey-spec/client-config opts)
        (assoc :vertx vertx)
        client/get-client-config
        RingClient/create
        client/->DonkeyClient)))

(defn- ^VertxOptions get-vertx-options
  "Creates and returns a VertxOptions object from the opts map.
  The vertx options are used to initialise the Vertx object which is an
  integral part of the server and client. It allows configuring thread pools
  and metrics."
  [opts]
  (let [vertx-options (VertxOptions.)]
    (.setPreferNativeTransport vertx-options true)
    (.setEventLoopPoolSize vertx-options (int (:event-loops opts (CpuCoreSensor/availableProcessors))))
    (when-let [worker-threads (:worker-threads opts)]
      (.setWorkerPoolSize vertx-options (int worker-threads)))
    (when (:metrics-enabled opts)
      (.setMetricsOptions vertx-options (metrics/get-metrics-options opts)))
    vertx-options))

(defn ^Donkey create-donkey
  "Create a Donkey factory. Use the factory to create an HTTP server or client.
  `opts` map description (all values are optional unless stated otherwise):

  :event-loops [int] The number of event loops that will be used by this
    instance. An event loop corresponds to a single OS thread. Every server and
    client created by this Donkey instance will share its event loops. It is
    recommended to have at least one event loop per available CPU core, which is
    also the default setting.

  :worker-threads [int] The number of worker threads that will be used when
    :handler-mode is :blocking. In blocking mode all user code will be executed
    off the event loop by a worker thread. It is not recommended to run blocking
    handlers, unless absolutely necessary. It is necessary to experiment
    with the size of :worker-threads until you reach the desired application
    requirements.

  :metrics-enabled [boolean] Enable metrics collection. Disabled by default.

  :metrics-prefix [string] A prefix that will be added to all metrics. Can be used
    to differentiate between different projects. Defaults to 'donkey'.

  :metric-registry [MetricRegistry] By default a new MetricRegistry is created
    that can be used during development. In production you should implement
    the reporting logic and supply an instance of the registry.
  "
  ([] create-donkey {})
  ([opts]
   (-> (spec/assert ::donkey-spec/donkey-config opts)
       get-vertx-options
       Vertx/vertx
       ->Donkey)))

;;;;;;;;;; Example ;;;;;;;;;;

(comment
  (defn- print-query-params-and-headers [req respond _raise]
    (respond
      {:body (format "Query parameters: %s. Headers: %s"
                     (apply str (seq (:query-params req)))
                     (apply str (seq (:headers req))))}))

  (defn- add-headers [handler]
    (fn [req respond raise]
      (handler
        (update req :headers assoc "DNT" 1 "Cache-Control" "no-cache")
        respond
        raise)))

  (defn- ^DonkeyServer new-server [^Donkey donkey]
    (create-server
      donkey
      {:port                8080
       :middleware          [middleware/keywordize-query-params add-headers]
       :debug               false
       :date-header         true
       :content-type-header true
       :server-header       true
       :routes              [{:path     "/plaintext"
                              :methods  [:get]
                              :produces ["text/plain"]
                              :handler  (fn [_req res _err] (res {:body "Hello, world!"}))}
                             {:path    "/benchmark"
                              :methods [:get]
                              :handler print-query-params-and-headers}]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

