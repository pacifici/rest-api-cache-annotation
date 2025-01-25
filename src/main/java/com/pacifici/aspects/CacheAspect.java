package com.pacifici.aspects;

import com.pacifici.annotations.CacheOperation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class CacheAspect {

    private static final Logger logger = LoggerFactory.getLogger(CacheAspect.class);

    private final ConcurrentHashMap<String, ConcurrentHashMap<Object, Object>> caches = new ConcurrentHashMap<>();

    @Around("@annotation(cacheOperation)")
    public Object manageCache(ProceedingJoinPoint joinPoint, CacheOperation cacheOperation) throws Throwable {
        String cacheName = cacheOperation.cacheName();
        String operation = cacheOperation.operation();
        Object[] args = joinPoint.getArgs();

        // Ensure the cache exists
        caches.putIfAbsent(cacheName, new ConcurrentHashMap<>());
        ConcurrentHashMap<Object, Object> cache = caches.get(cacheName);

        logger.info("Operation '{}' on cache '{}' with arguments: {}", operation, cacheName, args);

        switch (operation) {
            case "add": {
                Object id = args[0];
                Object value = args[1];
                // Proceed with the method call to update the internal data store
                Object result = joinPoint.proceed();
                // Add the item to the cache after the method succeeds
                cache.put(id, value);
                logger.info("Cache 'add' operation completed: {} -> {}", id, value);
                return result;
            }
            case "get": {
                Object id = args[0];
                if (cache.containsKey(id)) {
                    logger.info("Cache 'get' operation hit: {} -> {}", id, cache.get(id));
                    return cache.get(id);
                }
                logger.info("Cache 'get' operation miss: {}", id);
                // Proceed with the method call if cache miss
                return joinPoint.proceed();
            }
            case "update": {
                Object id = args[0];
                Object value = args[1];
                // Proceed with the method call to update the internal data store
                Object result = joinPoint.proceed();
                // Update the item in the cache after the method succeeds
                if (cache.containsKey(id)) {
                    cache.put(id, value);
                    logger.info("Cache 'update' operation completed: {} -> {}", id, value);
                }
                return result;
            }
            case "delete": {
                Object id = args[0];
                // Proceed with the method call to update the internal data store
                Object result = joinPoint.proceed();
                // Remove the item from the cache after the method succeeds
                if (cache.containsKey(id)) {
                    cache.remove(id);
                    logger.info("Cache 'delete' operation completed: {}", id);
                }
                return result;
            }
            default:
                logger.error("Unknown cache operation: {}", operation);
                throw new IllegalArgumentException("Unknown cache operation: " + operation);
        }
    }
}
