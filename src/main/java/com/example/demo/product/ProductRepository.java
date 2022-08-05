package com.example.demo.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long> {

    // same as SELECT * FROM student WHERE email = ?
    @Query("SELECT s FROM Product s WHERE s.name = ?1")
    Optional<Product> findProductByName(String name);
}