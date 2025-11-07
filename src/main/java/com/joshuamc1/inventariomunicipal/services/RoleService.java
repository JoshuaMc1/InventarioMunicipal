package com.joshuamc1.inventariomunicipal.services;

import com.joshuamc1.inventariomunicipal.models.Permission;
import com.joshuamc1.inventariomunicipal.models.Role;
import com.joshuamc1.inventariomunicipal.models.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RoleService {
    AuditService auditService = new AuditService();

    // Create a role
    public Role createRole(String name, String description, User currentUser) {
        Role role = new Role();
        role.set("nombre", name);
        role.set("descripcion", description);
        role.saveIt();

        auditService.logAction(currentUser, "Created role", "roles", (Integer) role.getId(), null);
        return role;
    }

    // Assign permission to role
    public void assignPermission(@NotNull Role role, Permission permission, User currentUser) {
        role.assignPermission(permission);
        auditService.logAction(currentUser, "Assigned permission " + permission.getString("nombre"), "rol_permisos", null, null);
    }

    // Get all roles
    public List<Role> getAllRoles() {
        return Role.findAll();
    }
}
