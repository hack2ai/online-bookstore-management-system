-- Demo catalog data for local/container development only.
-- Safe to execute after schema.sql. No credentials or payment secrets are included.

USE bookstore_db;
SET NAMES utf8mb4;

INSERT INTO categories (category_name, description)
VALUES
    ('Programming', 'Software engineering, development and computer science.'),
    ('Business', 'Leadership, startups, management and strategy.'),
    ('Design', 'Product, UX, visual design and creative practice.')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO books (title, author, isbn, description, price, stock, image_url, category_id)
SELECT 'Clean Code', 'Robert C. Martin', '9780132350884',
       'A practical guide to writing readable, maintainable and professional software.',
       499.00, 25, 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4', c.id
FROM categories c WHERE c.category_name = 'Programming'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.isbn = '9780132350884');

INSERT INTO books (title, author, isbn, description, price, stock, image_url, category_id)
SELECT 'Effective Java', 'Joshua Bloch', '9780134685991',
       'Best practices for modern Java development, APIs and robust application design.',
       699.00, 18, 'https://images.unsplash.com/photo-1516116216624-53e697fedbea', c.id
FROM categories c WHERE c.category_name = 'Programming'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.isbn = '9780134685991');

INSERT INTO books (title, author, isbn, description, price, stock, image_url, category_id)
SELECT 'The Pragmatic Programmer', 'David Thomas & Andrew Hunt', '9780135957059',
       'Timeless habits and techniques for becoming a more effective software developer.',
       649.00, 20, 'https://images.unsplash.com/photo-1498050108023-c5249f4df085', c.id
FROM categories c WHERE c.category_name = 'Programming'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.isbn = '9780135957059');

INSERT INTO books (title, author, isbn, description, price, stock, image_url, category_id)
SELECT 'Atomic Habits', 'James Clear', '9780735211292',
       'A practical framework for building good habits and breaking bad ones.',
       399.00, 30, 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d', c.id
FROM categories c WHERE c.category_name = 'Business'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.isbn = '9780735211292');

INSERT INTO books (title, author, isbn, description, price, stock, image_url, category_id)
SELECT 'The Lean Startup', 'Eric Ries', '9780307887894',
       'A disciplined approach to building products, learning from customers and reducing waste.',
       549.00, 15, 'https://images.unsplash.com/photo-1556761175-b413da4baf72', c.id
FROM categories c WHERE c.category_name = 'Business'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.isbn = '9780307887894');

INSERT INTO books (title, author, isbn, description, price, stock, image_url, category_id)
SELECT 'Refactoring UI', 'Adam Wathan & Steve Schoger', '9780991347880',
       'Practical visual techniques for turning rough interfaces into polished products.',
       799.00, 12, 'https://images.unsplash.com/photo-1561070791-2526d30994b5', c.id
FROM categories c WHERE c.category_name = 'Design'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.isbn = '9780991347880');
