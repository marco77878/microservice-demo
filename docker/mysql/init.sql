-- MySQL initialization script
CREATE DATABASE IF NOT EXISTS microservices_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE microservices_db;

-- Users table for UAA
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed default users (passwords: user_1, editor_1, adm_1)
-- BCrypt hashed with cost factor 10
INSERT INTO users (username, password, role) VALUES
('user_1', '$2a$10$7rV0993WbFhBovkP/5nQ4uvG8jZ5M.rJ9bT1K/48u6r5eS7E3K/u2', 'ROLE_USER'),
('editor_1', '$2a$10$qV4eBkJpD4zP9cQzX5U4ueqV4eBkJpD4zP9cQzX5U4ueqV4eBkJpD', 'ROLE_EDITOR'),
('adm_1', '$2a$10$7yM9R1rE7yM9R1rE7yM9Ru7yM9R1rE7yM9R1rE7yM9R1rE7yM9R1r', 'ROLE_PRODUCT_ADMIN')
ON DUPLICATE KEY UPDATE role = VALUES(role);

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed sample products
INSERT INTO products (id, name) VALUES
(1, 'MacBook Pro 16-inch'),
(2, 'iPhone 15 Pro Max'),
(3, 'iPad Pro M4')
ON DUPLICATE KEY UPDATE name = VALUES(name);
