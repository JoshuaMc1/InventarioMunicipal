package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.Table;

@Table("auditoria")
@BelongsTo(parent = User.class, foreignKeyName = "usuario_id")
public class Audit extends Model {
    static {
        // validar campos con nombres reales de la tabla
        validatePresenceOf("usuario_id", "accion");
    }

    public User getUser() {
        return parent(User.class);
    }

    public static void logAction(User user, String action, String tableName, Integer recordId, String details) {
        Audit audit = new Audit();
        if (user != null) {
            audit.set("usuario_id", user.getId());
        }
        audit.set("accion", action);
        audit.set("tabla_afectada", tableName);
        audit.set("registro_afectado_id", recordId);
        audit.set("detalles", details);
        audit.saveIt();
    }
}
