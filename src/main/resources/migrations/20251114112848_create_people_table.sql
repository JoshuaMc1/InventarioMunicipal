CREATE TABLE people (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dni VARCHAR(15) UNIQUE NOT NULL,             -- Format: 0000-0000-00000
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    internal_code VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);