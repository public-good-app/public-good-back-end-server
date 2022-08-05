package com.example.demo.product;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

@Configuration
public class ProductConfig {

//    String name,
//    String brand,
//    String retailer,
//    Double price,
//    boolean availability,
//    Integer quantity

    @Bean
    CommandLineRunner commandLineRunner(ProductRepository repository) {
        return args -> {
            Product enfamil1 = new Product(
                    "Enfamil 1",
                    "Enfamil",
                    "Target",
                    47.99,
                    true,
                    7);

            Product enfamil2 = new Product(
                    "Enfamil 2",
                    "Enfamil",
                    "Target",
                    17.99,
                    true,
                    11);

            Product enfamil3 = new Product(
                    "Enfamil 2",
                    "Enfamil",
                    "Walmart",
                    19.99,
                    true,
                    17);

            Product enfamil4 = new Product(
                    "Enfamil 2",
                    "Enfamil",
                    "Walmart",
                    17.99,
                    false,
                    null);

            repository.saveAll(List.of(enfamil1, enfamil2, enfamil3, enfamil4));
        };
    }
}
