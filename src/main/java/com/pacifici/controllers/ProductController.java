package com.pacifici.controllers;

import com.pacifici.annotations.CacheOperation;
import com.pacifici.models.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final Map<Integer, Product> products = new HashMap<>();

    @CacheOperation(operation = "add", cacheName = "productsCache", cacheType = Product.class)
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestParam int id, @RequestParam String name) {
        logger.info("Executing controller logic: Adding product with id={} and name={}", id, name);
        Product product = new Product(id, name);
        products.put(id, product);
        logger.info("Product added to internal map: {}", products);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @CacheOperation(operation = "get", cacheName = "productsCache", cacheType = Product.class)
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable int id) {
        logger.info("Executing controller logic: Fetching product with id={}", id);
        if (products.containsKey(id)) {
            Product product = products.get(id);
            logger.info("Product with id={} found: {}", id, product);
            return ResponseEntity.ok(product);
        }
        logger.warn("Product with id={} not found in internal map", id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @CacheOperation(operation = "update", cacheName = "productsCache", cacheType = Product.class)
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable int id, @RequestParam String name) {
        logger.info("Executing controller logic: Updating product with id={} to new name={}", id, name);
        if (products.containsKey(id)) {
            Product product = new Product(id, name);
            products.put(id, product);
            logger.info("Product updated in internal map: {}", products);
            return ResponseEntity.ok(product);
        }
        logger.warn("Product with id={} not found for update", id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @CacheOperation(operation = "delete", cacheName = "productsCache", cacheType = Product.class)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable int id) {
        logger.info("Executing controller logic: Deleting product with id={}", id);
        if (products.containsKey(id)) {
            products.remove(id);
            logger.info("Product with id={} deleted from internal map", id);
            return ResponseEntity.ok().build();
        }
        logger.warn("Product with id={} not found for deletion", id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
