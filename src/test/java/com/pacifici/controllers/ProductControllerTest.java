package com.pacifici.controllers;

import com.pacifici.models.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final Product testProduct = new Product(1, "TestProduct");

    @BeforeEach
    void setup() {}

    @Test
    void createProduct_ShouldReturnCreatedProduct() throws Exception {
        mockMvc.perform(post("/products?id=1&name=TestProduct"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(testProduct.getId())))
                .andExpect(jsonPath("$.name", is(testProduct.getName())));
    }

    @Test
    void getProduct_ShouldReturnExistingProduct() throws Exception {
        mockMvc.perform(post("/products?id=1&name=TestProduct"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testProduct.getId())))
                .andExpect(jsonPath("$.name", is(testProduct.getName())));
    }

    @Test
    void updateProduct_ShouldReturnUpdatedProduct() throws Exception {
        mockMvc.perform(post("/products?id=1&name=TestProduct"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/products/1?name=UpdatedProduct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("UpdatedProduct")));
    }

    @Test
    void deleteProduct_ShouldRemoveProduct() throws Exception {
        mockMvc.perform(post("/products?id=1&name=TestProduct"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isNotFound());
    }
}
