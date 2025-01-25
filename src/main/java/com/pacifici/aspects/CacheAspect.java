package com.pacifici.aspects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacifici.annotations.CacheOperation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class CacheAspect {

    private static final Logger logger = LoggerFactory.getLogger(CacheAspect.class);

    private final ConcurrentHashMap<String, ConcurrentHashMap<Object, String>> caches = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(cacheOperation)")
    public Object manageCache(ProceedingJoinPoint joinPoint, CacheOperation cacheOperation) throws Throwable {
        String cacheName = cacheOperation.cacheName();
        String operation = cacheOperation.operation();
        Class<?> cacheType = cacheOperation.cacheType(); // Get the specified type
        Object[] args = joinPoint.getArgs();

        // Ensure the cache exists
        caches.putIfAbsent(cacheName, new ConcurrentHashMap<>());
        ConcurrentHashMap<Object, String> cache = caches.get(cacheName);

        logger.info("Operation '{}' on cache '{}' with arguments: {}", operation, cacheName, args);

        // Handle "get" operation
        if ("get".equals(operation)) {
            Object id = args[0];
            if (cache.containsKey(id)) {
                String cachedValue = cache.get(id);
                logger.info("Cache hit for key '{}' in cache '{}': {}", id, cacheName, cachedValue);
                return ResponseEntity.ok(deserialize(cachedValue, cacheType)); // Use the specified type
            }
            logger.info("Cache miss for key '{}' in cache '{}'", id, cacheName);
        }

        // Proceed with the controller logic
        Object result = joinPoint.proceed();

        // Handle response and update the cache if the operation is successful
        if (result instanceof ResponseEntity<?> responseEntity && isSuccessful(responseEntity)) {
            Object body = responseEntity.getBody();
            switch (operation) {
                case "add":
                case "update": {
                    Object id = args[0];
                    cache.put(id, serialize(body));
                    logger.info("Cache '{}' operation completed: {} -> {}", operation, id, body);
                    break;
                }
                case "delete": {
                    Object id = args[0];
                    cache.remove(id);
                    logger.info("Cache 'delete' operation completed: {}", id);
                    break;
                }
                default:
                    logger.error("Unknown cache operation: {}", operation);
                    throw new IllegalArgumentException("Unknown cache operation: " + operation);
            }
        } else if (result instanceof ResponseEntity<?>) {
            logger.warn("Cache operation '{}' skipped due to unsuccessful response: {}", operation,
                    ((ResponseEntity<?>) result).getStatusCode());
        }

        return result;
    }

    private String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize object: {}", object, e);
            throw new RuntimeException("Serialization error", e);
        }
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize JSON: {}", json, e);
            throw new RuntimeException("Deserialization error", e);
        }
    }

    private boolean isSuccessful(ResponseEntity<?> response) {
        return response.getStatusCode().is2xxSuccessful();
    }
}
