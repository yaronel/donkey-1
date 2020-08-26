package com.appsflyer.donkey.log;

import io.vertx.core.logging.Logger;
import io.vertx.core.spi.logging.LogDelegate;
import io.vertx.core.spi.logging.LogDelegateFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Mostly borrowed from {@link io.vertx.core.logging.LoggerFactory}</b>
 * <p></p>
 * <p>
 * This implementation is only used internally to set the log level
 * programmatically during a debug session.
 * It relies on <a href="http://logback.qos.ch/">Logback</a> to be in the classpath.
 */
public final class LogbackLoggerFactory {
  
  private static volatile LogDelegateFactory delegateFactory;
  private static final Map<String, LogbackLogger> loggers = new ConcurrentHashMap<>(10);
  private static final String FACTORY_DELEGATE_CLASS_NAME = "com.appsflyer.donkey.log.LogbackDelegateFactory";
  
  static {
    initialise();
  }
  
  private LogbackLoggerFactory() {}
  
  public static synchronized void initialise() {
    LogDelegateFactory delegateFactory;
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    try {
      Class<?> clz = loader.loadClass(FACTORY_DELEGATE_CLASS_NAME);
      delegateFactory = (LogDelegateFactory) clz.getConstructor().newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format("Error instantiating transformer class \"%s\"", FACTORY_DELEGATE_CLASS_NAME), e);
    }
    LogbackLoggerFactory.delegateFactory = delegateFactory;
    
  }
  
  public static Logger getLogger(Class<?> clazz) {
    String name = clazz.isAnonymousClass() ?
        clazz.getEnclosingClass().getCanonicalName() :
        clazz.getCanonicalName();
    return getLogger(name);
  }
  
  public static LogbackLogger getLogger(String name) {
    LogbackLogger logger = loggers.get(name);
    
    if (logger == null) {
      LogDelegate delegate = delegateFactory.createDelegate(name);
      
      logger = new LogbackLogger(delegate);
      
      LogbackLogger oldLogger = loggers.putIfAbsent(name, logger);
      
      if (oldLogger != null) {
        logger = oldLogger;
      }
    }
    
    return logger;
  }
  
  public static void removeLogger(String name) {
    loggers.remove(name);
  }
}