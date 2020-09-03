package com.appsflyer.donkey.route.ring.middleware;

import clojure.lang.*;

import java.util.Objects;

import static com.appsflyer.donkey.route.handler.ring.RingRequestField.QUERY_PARAMS;

public final class MiddlewareProvider {
  
  private MiddlewareProvider() {}
  
  public static IPersistentMap keywordizeQueryParams(IPersistentMap request) {
    Objects.requireNonNull(request, "Request map cannot be null");
  
    Object queryParams = request.valAt(QUERY_PARAMS.keyword());
    if (queryParams == null) {
      return request;
    }
    int size = ((Counted) queryParams).count();
    if (size == 0) {
      return request;
    }
  
    IPersistentMap params = (IPersistentMap) queryParams;
    Object[] res = new Object[params.count() << 1];
    var iter = params.iterator();
    for (int i = 0; iter.hasNext(); i += 2) {
      var entry = (IMapEntry) iter.next();
      res[i] = Keyword.intern((String) entry.getKey());
      res[i + 1] = entry.getValue();
    }
    return request.assoc(QUERY_PARAMS.keyword(), RT.mapUniqueKeys(res));
  }
}