package com.example.product.config;

import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ProductInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductInitializer.class);

    private final ProductRepository productRepository;

    public ProductInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            productRepository.save(new Product("MacBook Pro 16"));
            productRepository.save(new Product("iPhone 15 Pro Max"));
            productRepository.save(new Product("iPad Pro M4"));
            log.info("Initialized default sample products.");
        }
    }
}
