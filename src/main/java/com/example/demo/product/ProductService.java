package com.example.demo.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Component / Service wrappers makes it a Spring Bean
@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    public void addNewProduct(Product product) {
        Optional<Product> productOptional = productRepository.findProductByName(product.getName());
        if (productOptional.isPresent()) {
            throw new IllegalStateException("Product name taken");
        } else {
            productRepository.save(product);
        }
    }

    public void deleteProduct (Long productId) {
        boolean exists = productRepository.existsById(productId);
        if (!exists) {
            throw new IllegalStateException("product with id " + productId + " does not exist");
        }
        productRepository.deleteById(productId);
    }

    // @Transactional makes the entity go into a managed state without need to use queries from StudentRepository
    @Transactional
    public void updateProduct(Long productId, String name, Double price, boolean availability, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException(
                        "product with id " + productId + " does not exist"));

        if (name != null && name.length() > 0 && !Objects.equals(product.getName(), name)) {
            product.setName(name);
        }

        if (price != null && !Objects.equals(product.getPrice(), price)) {
            product.setPrice(price);
        }

        if (!Objects.equals(product.isAvailable(), availability)) {
            product.setAvailability(availability);
        }

        if (quantity != null && !Objects.equals(product.getQuantity(), quantity)) {
            product.setQuantity(quantity);
        }

//        if (email != null && email.length() > 0 && !Objects.equals(student.getEmail(), email)) {
//            Optional<Student> studentOptional = studentRepository.findStudentByEmail(email);
//            if (studentOptional.isPresent()) {
//                throw new IllegalStateException("email taken");
//            }
//            student.setEmail(email);
//        }
    }
}
