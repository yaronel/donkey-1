/*
 * Copyright 2020 AppsFlyer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appsflyer.donkey.server.ring.route;

import com.appsflyer.donkey.server.route.PathDefinition;
import com.appsflyer.donkey.server.route.RouteDefinition;
import com.appsflyer.donkey.server.router.RouteList;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.appsflyer.donkey.server.route.PathDefinition.MatchType.REGEX;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class RingRouteCreatorTest {
  
  @Test
  void testBuildRoute(Vertx vertx, VertxTestContext testContext) {
    var routeDescriptor =
        RouteDefinition.create()
                       .addMethod(HttpMethod.GET)
                       .addMethod(HttpMethod.POST)
                       .path(PathDefinition.create("/foo"))
                       .handler(RoutingContext::next);
  
    var routeCreator = new RingRouteCreator(Router.router(vertx), RouteList.from(routeDescriptor));
    Router router = routeCreator.addRoutes();
    assertEquals(1, router.getRoutes().size());
    Route route = router.getRoutes().get(0);
    
    assertEquals(routeDescriptor.path().value(), route.getPath());
    assertEquals(routeDescriptor.methods(), route.methods());
    assertFalse(route.isRegexPath());
    
    testContext.completeNow();
  }
  
  @Test
  void testBuildRegexRoute(Vertx vertx, VertxTestContext testContext) {
    var routeDescriptor =
        RouteDefinition.create()
                       .path(PathDefinition.create("/foo/[0-9]+", REGEX))
                       .handler(RoutingContext::next);
  
    var routeCreator = new RingRouteCreator(Router.router(vertx), RouteList.from(routeDescriptor));
    Router router = routeCreator.addRoutes();
    assertEquals(1, router.getRoutes().size());
    Route route = router.getRoutes().get(0);
    assertTrue(route.isRegexPath());
    
    testContext.completeNow();
  }
}
