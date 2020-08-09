package com.appsflyer.donkey.route.ring;

import clojure.lang.IPersistentMap;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class BlockingRingHandler implements Handler<RoutingContext>
{
  private static final Logger logger = LoggerFactory.getLogger(BlockingRingHandler.class.getName());
  private final Function<RoutingContext, ?> fun;
  
  public BlockingRingHandler(Function<RoutingContext, ?> fun)
  {
    this.fun = fun;
  }
  
  @Override
  public void handle(RoutingContext ctx)
  {
    IPersistentMap response;
    try {
      response = (IPersistentMap) fun.apply(ctx);
    } catch (Exception ex) {
      logger.error(String.format("User handler failed: %s", ex.getMessage()), ex);
      ctx.fail(ex);
      return;
    }
    ctx.put("ring-response", response);
    ctx.next();
  }
}
