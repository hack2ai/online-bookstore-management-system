-- ============================================================================
-- Online Bookstore Management System — MySQL 8 Schema
-- ============================================================================
-- This file is the single source of truth for the physical schema.
-- spring.jpa.hibernate.ddl-auto=validate means Hibernate validates mappings
-- against this schema at startup.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS bookstore_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE bookstore_db;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL,
    password    VARCHAR(255)  NOT NULL,
    role        VARCHAR(20)   NOT NULL DEFAULT 'CUSTOMER',
    phone       VARCHAR(20)   NULL,
    address     VARCHAR(255)  NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- refresh_tokens
-- Only a SHA-256 hash is stored; the raw token is returned once to the client.
-- Revocation supports logout and refresh-token rotation.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  DATETIME     NOT NULL,
    revoked_at  DATETIME     NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,

    INDEX idx_refresh_tokens_hash (token_hash),
    INDEX idx_refresh_tokens_user_id (user_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS categories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name   VARCHAR(100) NOT NULL,
    description     VARCHAR(500) NULL,
    CONSTRAINT uk_categories_name UNIQUE (category_name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS books (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(255)    NOT NULL,
    author          VARCHAR(150)    NOT NULL,
    isbn            VARCHAR(20)     NOT NULL,
    description     VARCHAR(2000)   NULL,
    price           DECIMAL(10, 2)  NOT NULL,
    stock           INT             NOT NULL DEFAULT 0,
    image_url       VARCHAR(500)    NULL,
    category_id     BIGINT          NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_books_isbn UNIQUE (isbn),
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT chk_books_price_nonneg CHECK (price >= 0),
    CONSTRAINT chk_books_stock_nonneg CHECK (stock >= 0),
    INDEX idx_books_title (title),
    INDEX idx_books_author (author),
    INDEX idx_books_category_id (category_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS cart (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    CONSTRAINT uk_cart_user UNIQUE (user_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id     BIGINT  NOT NULL,
    book_id     BIGINT  NOT NULL,
    quantity    INT     NOT NULL,
    CONSTRAINT uk_cart_items_cart_book UNIQUE (cart_id, book_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES cart (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT chk_cart_items_qty_positive CHECK (quantity >= 1)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    total_amount    DECIMAL(10, 2)  NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    order_date      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_orders_total_nonneg CHECK (total_amount >= 0),
    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_status (status)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT          NOT NULL,
    book_id     BIGINT          NOT NULL,
    quantity    INT             NOT NULL,
    price       DECIMAL(10, 2)  NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE RESTRICT,
    CONSTRAINT chk_order_items_qty_positive CHECK (quantity >= 1),
    CONSTRAINT chk_order_items_price_nonneg CHECK (price >= 0),
    INDEX idx_order_items_order_id (order_id),
    INDEX idx_order_items_book_id (book_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT          NOT NULL,
    payment_method  VARCHAR(30)     NOT NULL,
    payment_status  VARCHAR(20)     NOT NULL DEFAULT 'CREATED',
    transaction_id  VARCHAR(100)    NULL,
    CONSTRAINT uk_payments_order UNIQUE (order_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT chk_payments_status CHECK (payment_status IN ('CREATED', 'SUCCESS', 'FAILED', 'REFUNDED')),
    INDEX idx_payments_transaction_id (transaction_id)
) ENGINE = InnoDB;
