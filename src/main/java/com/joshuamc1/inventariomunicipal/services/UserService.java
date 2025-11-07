package com.joshuamc1.inventariomunicipal.services;

import com.joshuamc1.inventariomunicipal.models.Role;
import com.joshuamc1.inventariomunicipal.models.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UserService {
    AuditService auditService = new AuditService();

    // Authenticate user by username and password hash
    public User login(String username, String passwordHash) {
        User user = User.findFirst("username = ? AND password_hash = ?", username, passwordHash);

        if(user != null && user.getBoolean("activo")) {
            auditService.logAction(user, "Login successful", "usuarios", (Integer) user.getId(), null);
            return user;
        }
        return null;
    }

    // Create a new user
    public User createUser(String username, String passwordHash, String name, String email, User currentUser) {
        User user = new User();
        user.set("username", username);
        user.set("password_hash", passwordHash);
        user.set("nombre", name);
        user.set("email", email);
        user.saveIt();

        auditService.logAction(currentUser, "Created user", "usuarios", (Integer) user.getId(), null);
        return user;
    }

    // Assign role to a user
    public void assignRole(@NotNull User user, Role role, User currentUser) {
        user.assignRole(role);
        auditService.logAction(currentUser, "Assigned role " + role.getString("nombre"), "usuario_roles", null, null);
    }

    // Change password
    public void changePassword(@NotNull User user, String newPasswordHash, User currentUser) {
        user.set("password_hash", newPasswordHash);
        user.saveIt();
        auditService.logAction(currentUser, "Changed password", "usuarios", (Integer) user.getId(), null);
    }

    // Get all users
    public List<User> getAllUsers() {
        return User.findAll();
    }
}
