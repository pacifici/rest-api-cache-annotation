# Custom Annotations with Cache Implementation in Spring Boot

This repository demonstrates how to implement **custom annotations** in a Spring Boot application to manage a **cache layer** using an `@Aspect`-oriented programming approach. The project includes a fully functional example of a product management API where caching is applied via a custom annotation.

## Features
- **Custom Annotations**: Define and use `@CacheOperation` to specify caching behavior.
- **Aspect-Oriented Programming (AOP)**: Intercept methods and manage caching logic transparently.
- **Concurrent Caching**: Use `ConcurrentHashMap` for thread-safe in-memory caching.
- **CRUD Operations**: A RESTful API for managing products with integrated caching.

## Use Case
This project is ideal for demonstrating how to:
- Apply custom annotations for specific logic (e.g., caching).
- Leverage Spring AOP for cross-cutting concerns like caching.
- Improve API performance by reducing unnecessary method calls with a cache layer.

## Project Structure

### 1. Custom Annotation: `@CacheOperation`
```java
package com.pacifici.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheOperation {
    String operation(); // The cache operation (e.g., "add", "get", "update", "delete").
    String cacheName(); // The name of the cache.
}
```

### 2. Aspect: `CacheAspect`
```java
package com.pacifici.aspects;

import com.pacifici.annotations.CacheOperation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class CacheAspect {

    private final ConcurrentHashMap<String, ConcurrentHashMap<Object, Object>> caches = new ConcurrentHashMap<>();

    @Around("@annotation(cacheOperation)")
    public Object manageCache(ProceedingJoinPoint joinPoint, CacheOperation cacheOperation) throws Throwable {
        String cacheName = cacheOperation.cacheName();
        String operation = cacheOperation.operation();
        Object[] args = joinPoint.getArgs();

        caches.putIfAbsent(cacheName, new ConcurrentHashMap<>());
        ConcurrentHashMap<Object, Object> cache = caches.get(cacheName);

        switch (operation) {
            case "add": {
                Object id = args[0];
                Object value = args[1];
                Object result = joinPoint.proceed();
                cache.put(id, value);
                return result;
            }
            case "get": {
                Object id = args[0];
                if (cache.containsKey(id)) {
                    return cache.get(id);
                }
                return joinPoint.proceed();
            }
            case "update": {
                Object id = args[0];
                Object value = args[1];
                Object result = joinPoint.proceed();
                cache.put(id, value);
                return result;
            }
            case "delete": {
                Object id = args[0];
                Object result = joinPoint.proceed();
                cache.remove(id);
                return result;
            }
            default:
                throw new IllegalArgumentException("Unknown cache operation: " + operation);
        }
    }
}
```

### 3. REST Controller: `ProductController`
```java
package com.pacifici.controllers;

import com.pacifici.annotations.CacheOperation;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final Map<Integer, String> products = new HashMap<>();

    @CacheOperation(operation = "add", cacheName = "productsCache")
    @PostMapping
    public String createProduct(@RequestParam int id, @RequestParam String name) {
        products.put(id, name);
        return "Product added!";
    }

    @CacheOperation(operation = "get", cacheName = "productsCache")
    @GetMapping("/{id}")
    public String getProduct(@PathVariable int id) {
        return products.getOrDefault(id, "Product not found");
    }

    @CacheOperation(operation = "update", cacheName = "productsCache")
    @PutMapping("/{id}")
    public String updateProduct(@PathVariable int id, @RequestParam String name) {
        if (products.containsKey(id)) {
            products.put(id, name);
            return "Product updated!";
        }
        return "Product not found";
    }

    @CacheOperation(operation = "delete", cacheName = "productsCache")
    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable int id) {
        if (products.remove(id) != null) {
            return "Product deleted!";
        }
        return "Product not found";
    }
}
```

## How to Run

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/pacifici/rest-api-cache-annotation.git
   cd CacheAnnotations
   ```

2. **Build and Run**:
   Make sure you have Java 17+ and Maven installed. Run the following:
   ```bash
   mvn spring-boot:run
   ```

3. **Test the API**:
   Use tools like Postman or `curl` to test the endpoints:

   - **Add a Product**:
     ```bash
     curl -X POST "http://localhost:8080/products?id=1&name=TestProduct"
     ```

   - **Get a Product**:
     ```bash
     curl -X GET "http://localhost:8080/products/1"
     ```

   - **Update a Product**:
     ```bash
     curl -X PUT "http://localhost:8080/products/1?name=UpdatedProduct"
     ```

   - **Delete a Product**:
     ```bash
     curl -X DELETE "http://localhost:8080/products/1"
     ```

## Key Benefits

1. **Code Reusability**: The `@CacheOperation` annotation centralizes caching logic, avoiding duplication across methods.
2. **Transparency**: The aspect layer manages caching, keeping the controller logic clean and focused.
3. **Extensibility**: Easily add more caching behavior (e.g., TTL, eviction) by enhancing the `CacheAspect`.

## License

This project is licensed under the MIT License. Feel free to use and modify it for your needs.
