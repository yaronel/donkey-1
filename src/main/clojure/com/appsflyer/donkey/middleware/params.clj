(ns com.appsflyer.donkey.middleware.params
  (:import (com.appsflyer.donkey.middleware MiddlewareProvider)))

(defn keywordize-query-params [handler]
  (fn
    ([request]
     (handler (MiddlewareProvider/keywordizeQueryParams request)))
    ([request respond raise]
     (respond
       (handler (MiddlewareProvider/keywordizeQueryParams request) respond raise)))))