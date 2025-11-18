CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    date DATE DEFAULT (CURRENT_DATE),
    user_id INT NOT NULL,
    person_id INT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    FOREIGN KEY (person_id) REFERENCES people(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,
    CHECK (type IN ('in', 'out', 'loan'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
