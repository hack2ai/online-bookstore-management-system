-- ============================================================================
-- Online Bookstore Management System — MySQL 8 Schema
-- ============================================================================
-- This file is the single source of truth for the physical schema.
-- spring.jpa.hibernate.ddl-auto=validate (see application.properties) means
-- Hibernate will compare every @Entity mapping against these tables on
-- startup and refuse to boot if anything disagrees — column name, type,
-- nullability, length. If you add/change a field on an entity, update this
-- file in the SAME commit, or the app will fail fast at startup with a
-- SchemaManagementException (which is the intended behavior: better a loud
-- failure at boot than a silent, slowly-discovered mismatch in production).
--
-- Run manually with:
--   mysql -u root -p < src/main/resources/db/schema.sql
-- ...or let Spring Boot run it automatically via spring.sql.init.* on a
-- fresh `dev` profile boot (see application.properties). Either way it is
-- idempotent (CREATE TABLE IF NOT EXISTS) so re-running it is harmless.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS bookstore_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE bookstore_db;

SET NAMES utf8mb4;

-- ----------------------------------------------------------------------------
-- users
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL,
    password    VARCHAR(255)  NOT NULL,           -- BCrypt hash, never plaintext
    role        VARCHAR(20)   NOT NULL DEFAULT 'CUSTOMER',
    phone       VARCHAR(20)   NULL,
    address     VARCHAR(255)  NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- categories
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name   VARCHAR(100) NOT NULL,
    description     VARCHAR(500) NULL,

    CONSTRAINT uk_categories_name UNIQUE (category_name)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- books
-- ----------------------------------------------------------------------------
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
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE RESTRICT,                 -- a category in use by books cannot be deleted out from under them
    CONSTRAINT chk_books_price_nonneg CHECK (price >= 0),
    CONSTRAINT chk_books_stock_nonneg CHECK (stock >= 0),

    INDEX idx_books_title (title),
    INDEX idx_books_author (author),
    INDEX idx_books_category_id (category_id)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- cart  (exactly one row per user; created lazily on first add-to-cart)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,

    CONSTRAINT uk_cart_user UNIQUE (user_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE                   -- deleting a user cleans up their (now-orphaned) cart
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- cart_items
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id     BIGINT  NOT NULL,
    book_id     BIGINT  NOT NULL,
    quantity    INT     NOT NULL,

    CONSTRAINT uk_cart_items_cart_book UNIQUE (cart_id, book_id),  -- one row per book per cart; re-adding increments quantity instead
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES cart (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_book FOREIGN KEY (book_id) REFERENCES books (id)
        ON DELETE CASCADE,                  -- if a book is ever hard-deleted, it can't remain referenced in someone's cart
    CONSTRAINT chk_cart_items_qty_positive CHECK (quantity >= 1)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- orders
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    total_amount    DECIMAL(10, 2)  NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    order_date      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,                 -- order history must survive even if a user account is later deleted; delete the user's orders explicitly first if that's truly intended
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_orders_total_nonneg CHECK (total_amount >= 0),

    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_status (status)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- order_items
-- ----------------------------------------------------------------------------
-- price here is a FROZEN SNAPSHOT of books.price at the moment the order was
-- placed, not a live reference. See OrderItem.java javadoc for why.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT          NOT NULL,
    book_id     BIGINT          NOT NULL,
    quantity    INT             NOT NULL,
    price       DECIMAL(10, 2)  NOT NULL,       -- price AT TIME OF PURCHASE; never updated afterwards

    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE CASCADE,                  -- deleting an order (admin-only, rare) takes its line items with it
    CONSTRAINT fk_order_items_book FOREIGN KEY (book_id) REFERENCES books (id)
        ON DELETE RESTRICT,                 -- a book that has ever been ordered cannot be hard-deleted, preserving order history integrity
    CONSTRAINT chk_order_items_qty_positive CHECK (quantity >= 1),
    CONSTRAINT chk_order_items_price_nonneg CHECK (price >= 0),

    INDEX idx_order_items_order_id (order_id),
    INDEX idx_order_items_book_id (book_id)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- payments  (one row per order)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT          NOT NULL,
    payment_method  VARCHAR(30)     NOT NULL,
    payment_status  VARCHAR(20)     NOT NULL DEFAULT 'CREATED',
    transaction_id  VARCHAR(100)    NULL,

    CONSTRAINT uk_payments_order UNIQUE (order_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_payments_status CHECK (payment_status IN ('CREATED', 'SUCCESS', 'FAILED', 'REFUNDED')),

    INDEX idx_payments_transaction_id (transaction_id)
) ENGINE = InnoDB;
