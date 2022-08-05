package com.example.demo.product;

import javax.persistence.*;

// for hibernate
@Entity
// map student class to table in db
@Table
public class Product {

    // build sequence generator
    @Id
    @SequenceGenerator(
            name = "product_sequence",
            sequenceName = "product_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            // SEQUENCE recommended for postgres
            strategy = GenerationType.SEQUENCE,
            generator = "product_sequence"
    )
    private Long id;
    private String name;
    private String brand;
    private String retailer;
    private Double price;
    private boolean availability;
    private Integer quantity;
//    @Transient
//    private Integer age;
//    private LocalDate dob;
//    private String email;

    public Product() {
    }

    public Product(Long id,
                   String name,
                   String brand,
                   String retailer,
                   Double price,
                   boolean availability,
                   Integer quantity) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.retailer = retailer;
        this.price = price;
        this.availability = availability;
        this.quantity = quantity;
    }

    public Product(String name,
                   String brand,
                   String retailer,
                   Double price,
                   boolean availability,
                   Integer quantity) {
        this.name = name;
        this.brand = brand;
        this.retailer = retailer;
        this.price = price;
        this.availability = availability;
        this.quantity = quantity;
    }

//    public Student(String name,
//                   String email,
//                   LocalDate dob) {
//        this.name = name;
//        this.email = email;
//        this.dob = dob;
//    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getRetailer() {
        return retailer;
    }

    public void setRetailer(String retailer) {
        this.retailer = retailer;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public boolean isAvailable() {
        return availability;
    }

    public void setAvailability(boolean availability) {
        this.availability = availability;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }


//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public Integer getAge() {
//        return Period.between(this.dob, LocalDate.now()).getYears();
//    }
//
//    public void setAge(Integer age) {
//        this.age = age;
//    }
//
//    public LocalDate getDob() {
//        return dob;
//    }
//
//    public void setDob(LocalDate dob) {
//        this.dob = dob;
//    }
//
//    public String getEmail() {
//        return email;
//    }
//
//    public void setEmail(String email) {
//        this.email = email;
//    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + this.id +
                ", name=" + this.name +
                ", brand=" + brand +
                ", retailer=" + retailer +
                ", price=" + price +
                ", availability=" + availability +
                ", quantity=" + quantity +
                '}';
    }
}
