package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

import java.util.List;

@Table("roles")
public class Role extends Model {
    static {
        validatePresenceOf("name");
    }

    // Assign a permission to this role
    public void assignPermission(Permission permission) {
        add(permission);
    }

    public List<Permission> getPermissions() {
        return getAll(Permission.class);
    }

    public boolean hasPermission(String permissionName) {
        return getPermissions().stream().anyMatch(p -> p.getString("nombre").equalsIgnoreCase(permissionName));
    }
}
