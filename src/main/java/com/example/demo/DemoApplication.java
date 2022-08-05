package com.example.demo;

import com.example.demo.product.Product;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
//// @RestController API makes the class serve REST endpoints
//@RestController
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

//// 	GET
//	@GetMapping
//	public List<Student> hello() {
//		return List.of(
//				new Student(
//					1L,
//					"Mariam",
//					"mariam.jamal@gmail.com",
//					LocalDate.of(2000, 11, 29),
//					21
//			)
//		);
//	}
}
