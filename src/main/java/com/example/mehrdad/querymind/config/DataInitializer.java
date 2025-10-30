package com.example.mehrdad.querymind.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            // Create Users table
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) NOT NULL, " +
                "age INT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Create Products table
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS products (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "price DECIMAL(10,2) NOT NULL, " +
                "category VARCHAR(50), " +
                "stock INT DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Create Orders table
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS orders (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT, " +
                "product_id BIGINT, " +
                "quantity INT NOT NULL, " +
                "total_amount DECIMAL(10,2), " +
                "order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "status VARCHAR(20) DEFAULT 'PENDING'" +
                ")"
            );

            // Insert sample users
            jdbcTemplate.execute(
                "INSERT INTO users (name, email, age) VALUES " +
                "('John Doe', 'john@example.com', 30), " +
                "('Jane Smith', 'jane@example.com', 25), " +
                "('Bob Johnson', 'bob@example.com', 35), " +
                "('Alice Williams', 'alice@example.com', 28), " +
                "('Charlie Brown', 'charlie@example.com', 42)"
            );

            // Insert sample products
            jdbcTemplate.execute(
                "INSERT INTO products (name, price, category, stock) VALUES " +
                "('Laptop', 999.99, 'Electronics', 50), " +
                "('Mouse', 25.99, 'Electronics', 200), " +
                "('Keyboard', 79.99, 'Electronics', 150), " +
                "('Desk Chair', 299.99, 'Furniture', 30), " +
                "('Monitor', 349.99, 'Electronics', 75), " +
                "('Headphones', 149.99, 'Electronics', 100)"
            );

            // Insert sample orders
            jdbcTemplate.execute(
                "INSERT INTO orders (user_id, product_id, quantity, total_amount, status) VALUES " +
                "(1, 1, 1, 999.99, 'COMPLETED'), " +
                "(2, 2, 2, 51.98, 'COMPLETED'), " +
                "(3, 4, 1, 299.99, 'PENDING'), " +
                "(1, 5, 1, 349.99, 'COMPLETED'), " +
                "(4, 3, 1, 79.99, 'SHIPPED'), " +
                "(5, 6, 2, 299.98, 'COMPLETED')"
            );

            System.out.println("✅ Sample database initialized with users, products, and orders!");
        };
    }
}

