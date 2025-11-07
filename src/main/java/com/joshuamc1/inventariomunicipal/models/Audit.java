package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.jetbrains.annotations.NotNull;

@Table("auditoria")
public class Audit extends Model {
    static {
        validatePresenceOf("user_id", "action");
    }

    public User getUser() {
        return parent(User.class);
    }

    public static void logAction(@NotNull User user, String action, String tableName, Integer recordId, String details) {
        Audit audit = new Audit();
        audit.set("user_id", user.getId());
        audit.set("accion", action);
        audit.set("tabla_afectada", tableName);
        audit.set("registro_afectado_id", recordId);
        audit.set("detalles", details);
        audit.saveIt();
    }
}
