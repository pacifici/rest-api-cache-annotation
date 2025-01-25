package com.pacifici.controllers;

import com.pacifici.annotations.CacheOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final Map<Integer, String> products = new HashMap<>();

    @CacheOperation(operation = "add", cacheName = "productsCache")
    @PostMapping
    public String createProduct(@RequestParam int id, @RequestParam String name) {
        logger.info("Executing controller logic: Adding product with id={} and name={}", id, name);
        products.put(id, name);
        logger.info("Product added to internal map: {}", products);
        return "Product added!";
    }

    @CacheOperation(operation = "get", cacheName = "productsCache")
    @GetMapping("/{id}")
    public String getProduct(@PathVariable int id) {
        logger.info("Executing controller logic: Fetching product with id={}", id);
        String product = products.getOrDefault(id, "Product not found");
        if ("Product not found".equals(product)) {
            logger.warn("Product with id={} not found in internal map", id);
        } else {
            logger.info("Product with id={} found: {}", id, product);
        }
        return product;
    }

    @CacheOperation(operation = "update", cacheName = "productsCache")
    @PutMapping("/{id}")
    public String updateProduct(@PathVariable int id, @RequestParam String name) {
        logger.info("Executing controller logic: Updating product with id={} to new name={}", id, name);
        if (products.containsKey(id)) {
            products.put(id, name);
            logger.info("Product updated in internal map: {}", products);
            return "Product updated!";
        }
        logger.warn("Product with id={} not found for update", id);
        return "Product not found";
    }

    @CacheOperation(operation = "delete", cacheName = "productsCache")
    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable int id) {
        logger.info("Executing controller logic: Deleting product with id={}", id);
        if (products.remove(id) != null) {
            logger.info("Product with id={} deleted from internal map", id);
            return "Product deleted!";
        }
        logger.warn("Product with id={} not found for deletion", id);
        return "Product not found";
    }
}
