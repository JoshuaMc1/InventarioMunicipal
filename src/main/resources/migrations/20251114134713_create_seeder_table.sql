INSERT INTO users (id, name, username, password, role, created_at) VALUES
-- Hashed password for 'admin' is generated using bcrypt
(1, 'Joshua Mclean', 'admin', '$2a$10$69PnQOpVuqa.5e1gsG3xguPfxe6ZzNzpBMVTnqTNk9.o1AQm0vUMS', 'admin', datetime('now', 'localtime'));