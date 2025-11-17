CREATE TABLE IF NOT EXISTS auditoria (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL,
    accion TEXT NOT NULL,
    tabla_afectada TEXT,
    registro_afectado_id INTEGER,
    fecha TEXT DEFAULT (datetime('now')),
    detalles TEXT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
