package com.pacifici.controllers;

import com.pacifici.aspects.CacheAspect;
import com.pacifici.models.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(ProductControllerTest.CacheAspectTestConfig.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheAspect cacheAspect;

    private final Product testProduct = new Product(1, "TestProduct");

    @BeforeEach
    void setup() {
        reset(cacheAspect); // Reset the spy to ensure a clean state
    }

    // Utility to create a product
    private void createTestProduct(int id, String name) throws Exception {
        mockMvc.perform(post("/products?id=" + id + "&name=" + name))
                .andExpect(status().isCreated());
    }

    // Utility to verify CacheAspect invocation
    private void verifyCacheAspectInvocation() throws Throwable {
        verify(cacheAspect, atLeastOnce()).manageCache(any(), any());
    }

    @Test
    void createProduct_ShouldReturnCreatedProduct() throws Exception {
        mockMvc.perform(post("/products?id=1&name=TestProduct"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(testProduct.getId())))
                .andExpect(jsonPath("$.name", is(testProduct.getName())));
    }

    @Test
    void getProduct_ShouldReturnExistingProduct() throws Exception {
        createTestProduct(1, "TestProduct");

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testProduct.getId())))
                .andExpect(jsonPath("$.name", is(testProduct.getName())));
    }

    @Test
    void updateProduct_ShouldReturnUpdatedProduct() throws Exception {
        createTestProduct(1, "TestProduct");

        mockMvc.perform(put("/products/1?name=UpdatedProduct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("UpdatedProduct")));
    }

    @Test
    void deleteProduct_ShouldRemoveProduct() throws Exception {
        createTestProduct(1, "TestProduct");

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_ShouldInvokeCacheAspect() throws Throwable {
        createTestProduct(1, "TestProduct");

        // Verify CacheAspect invocation
        verifyCacheAspectInvocation();
    }

    @Test
    void getProduct_ShouldInvokeCacheAspect() throws Throwable {
        createTestProduct(1, "TestProduct");

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("TestProduct"));

        // Verify CacheAspect invocation
        verifyCacheAspectInvocation();
    }

    // Test configuration to provide a spy for CacheAspect
    static class CacheAspectTestConfig {
        @Bean
        @Primary // Ensures this bean is used in the test context
        public CacheAspect cacheAspect() {
            return Mockito.spy(new CacheAspect());
        }
    }
}
