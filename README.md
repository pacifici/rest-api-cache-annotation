
# Custom Annotations with Cache Implementation in Spring Boot

This repository demonstrates how to implement **custom annotations** in a Spring Boot application to manage a **cache layer** using an `@Aspect`-oriented programming approach. The project includes a fully functional example of a product management API where caching is applied via a custom annotation.

## Features
- **Custom Annotations**: Define and use `@CacheOperation` to specify caching behavior, including a `cacheType` to determine the type being cached.
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
    Class<?> cacheType(); // The type of the object being cached.
}
```

### 2. Aspect: `CacheAspect`
```java
package com.pacifici.aspects;

import com.pacifici.annotations.CacheOperation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class CacheAspect {

    private final ConcurrentHashMap<String, ConcurrentHashMap<Object, String>> caches = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(cacheOperation)")
    public Object manageCache(ProceedingJoinPoint joinPoint, CacheOperation cacheOperation) throws Throwable {
        String cacheName = cacheOperation.cacheName();
        String operation = cacheOperation.operation();
        Class<?> cacheType = cacheOperation.cacheType();

        caches.putIfAbsent(cacheName, new ConcurrentHashMap<>());
        ConcurrentHashMap<Object, String> cache = caches.get(cacheName);

        if ("get".equals(operation)) {
            Object id = joinPoint.getArgs()[0];
            if (cache.containsKey(id)) {
                return ResponseEntity.ok(deserialize(cache.get(id), cacheType));
            }
        }

        Object result = joinPoint.proceed();
        if (result instanceof ResponseEntity<?> responseEntity && responseEntity.getStatusCode().is2xxSuccessful()) {
            switch (operation) {
                case "add":
                case "update":
                    cache.put(joinPoint.getArgs()[0], serialize(responseEntity.getBody()));
                    break;
                case "delete":
                    cache.remove(joinPoint.getArgs()[0]);
                    break;
            }
        }

        return result;
    }

    private String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialization error", e);
        }
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Deserialization error", e);
        }
    }
}
```

### 3. REST Controller: `ProductController`
```java
package com.pacifici.controllers;

import com.pacifici.annotations.CacheOperation;
import com.pacifici.models.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final Map<Integer, Product> products = new HashMap<>();

    @CacheOperation(operation = "add", cacheName = "productsCache", cacheType = Product.class)
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestParam int id, @RequestParam String name) {
        Product product = new Product(id, name);
        products.put(id, product);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @CacheOperation(operation = "get", cacheName = "productsCache", cacheType = Product.class)
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable int id) {
        if (products.containsKey(id)) {
            return ResponseEntity.ok(products.get(id));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @CacheOperation(operation = "update", cacheName = "productsCache", cacheType = Product.class)
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable int id, @RequestParam String name) {
        if (products.containsKey(id)) {
            Product product = new Product(id, name);
            products.put(id, product);
            return ResponseEntity.ok(product);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @CacheOperation(operation = "delete", cacheName = "productsCache", cacheType = Product.class)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable int id) {
        if (products.containsKey(id)) {
            products.remove(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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
