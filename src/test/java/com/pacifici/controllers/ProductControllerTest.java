package com.pacifici.controllers;

import com.pacifici.aspects.CacheAspect;
import com.pacifici.controllers.config.ProductControllerTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(ProductControllerTestConfig.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductController productController;

    @Autowired
    private CacheAspect cacheAspect;

    private final Map<Integer, String> internalMap = new ConcurrentHashMap<>();

    @BeforeEach
    void setup() {
        // Mock the productController to simulate API calls
        doAnswer(invocation -> {
            int id = invocation.getArgument(0);
            String name = invocation.getArgument(1);
            internalMap.put(id, name); // Simulate adding to the internal map
            return "Product added!";
        }).when(productController).createProduct(anyInt(), anyString());

        when(productController.getProduct(anyInt()))
                .thenAnswer(invocation -> internalMap.getOrDefault(invocation.getArgument(0), "Product not found"));
    }

    @Test
    public void shouldCallApiIfCacheIsEmpty() throws Exception {
        // Simulate the cache is empty by not intercepting in the CacheAspect
        try {
            when(cacheAspect.manageCache(any(), any())).thenCallRealMethod();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        // Perform the GET request
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Product1"));

        // Verify the actual API was called
        verify(productController, times(1)).getProduct(1);
    }

    @Test
    public void shouldAddToApiAndCache() throws Exception {
        // Mock the CacheAspect to track cache changes
        ArgumentCaptor<Object> keyCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);

        try {
            doAnswer(invocation -> {
                keyCaptor.capture();
                valueCaptor.capture();
                return null;
            }).when(cacheAspect).manageCache(any(), any());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        // Perform a POST request to add a new product
        mockMvc.perform(post("/products?id=2&name=TestProduct"))
                .andExpect(status().isOk());

        // Verify that the product was added to the internal map
        assertThat(internalMap).containsEntry(2, "TestProduct");

        // Verify that the product was added to the cache
        try {
            verify(cacheAspect, times(1)).manageCache(any(), any());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        assertThat(keyCaptor.getValue()).isEqualTo(2); // Cache key
        assertThat(valueCaptor.getValue()).isEqualTo("TestProduct"); // Cache value
    }

    @Test
    public void shouldRetrieveProductFromCache() throws Exception {
        // Simulate a cache hit by mocking CacheAspect behavior
        try {
            when(cacheAspect.manageCache(any(), any())).thenReturn("CachedProduct");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        // Perform the GET request
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("CachedProduct"));

        // Verify that the API was not called since the cache was hit
        verify(productController, never()).getProduct(1);
    }
}
