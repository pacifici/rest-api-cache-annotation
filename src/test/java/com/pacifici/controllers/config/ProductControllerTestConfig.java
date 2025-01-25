package com.pacifici.controllers.config;

import com.pacifici.aspects.CacheAspect;
import com.pacifici.controllers.ProductController;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductControllerTestConfig {

    @Bean
    public CacheAspect cacheAspect() {
        return Mockito.mock(CacheAspect.class);
    }

    @Bean
    public ProductController productController() {
        return Mockito.mock(ProductController.class);
    }
}
