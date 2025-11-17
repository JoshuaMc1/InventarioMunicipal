CREATE TABLE transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('in', 'out', 'loan')),
    quantity REAL NOT NULL,
    date TEXT DEFAULT (date('now')),
    user_id INTEGER NOT NULL,
    person_id INTEGER,
    notes TEXT,
    created_at TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (product_id) REFERENCES products(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    FOREIGN KEY (person_id) REFERENCES people(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);
