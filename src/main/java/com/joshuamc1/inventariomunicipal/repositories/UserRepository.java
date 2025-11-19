package com.joshuamc1.inventariomunicipal.repositories;

import com.joshuamc1.inventariomunicipal.models.User;

import java.util.List;

public class UserRepository {
    public User findByUsername(String username) {
        return User.findFirst("username = ?", username);
    }

    public User findById(Integer id) {
        return User.findById(id);
    }

    public User create(String username, String password, String role) {
        User user = new User();
        user.set("username", username);
        user.setPassword(password);
        user.set("role", role);
        // Forzar cambio de contraseña al primer acceso
        user.set("must_change_password", 1);
        user.saveIt();

        return user;
    }

    public User createFull(String name, String username, String password, String role) {
        User user = new User();
        user.set("name", name);
        user.set("username", username);
        user.setPassword(password);
        user.set("role", role);
        // Forzar cambio de contraseña al primer acceso
        user.set("must_change_password", 1);
        user.saveIt();
        return user;
    }

    public boolean update(Integer id, String name, String username, String role, String newPassword) {
        User u = User.findById(id);
        if (u == null) return false;
        u.set("name", name);
        u.set("username", username);
        u.set("role", role);
        if (newPassword != null && !newPassword.isBlank()) {
            u.setPassword(newPassword);
            // Si se cambia la contraseña desde el propio usuario, quitar el flag
            u.set("must_change_password", 0);
        }
        return u.saveIt();
    }

    public boolean resetPassword(Integer id, String newPassword, boolean mustChange) {
        User u = User.findById(id);
        if (u == null) return false;
        u.setPassword(newPassword);
        u.set("must_change_password", mustChange ? 1 : 0);
        return u.saveIt();
    }

    public boolean delete(Integer id) {
        User u = User.findById(id);
        if (u == null) return false;
        return u.delete();
    }

    public long countUsers() {
        Long c = User.count();
        return c == null ? 0 : c;
    }

    public long countAdmins() {
        Long c = User.count("role = ?", "admin");
        return c == null ? 0 : c;
    }

    public List<User> listPaged(int offset, int limit, String search) {
        // Excluir usuarios con rol root por seguridad
        if (search != null && !search.isBlank()) {
            return User.where("(name LIKE ? OR username LIKE ?) AND role != ?",
                "%"+search+"%", "%"+search+"%", "root")
                .orderBy("created_at DESC")
                .offset(offset)
                .limit(limit);
        } else {
            return User.where("role != ?", "root")
                .orderBy("created_at DESC")
                .offset(offset)
                .limit(limit);
        }
    }

    /**
     * Verifica si un usuario es root
     */
    public boolean isRootUser(Integer id) {
        User u = User.findById(id);
        return u != null && "root".equals(u.getString("role"));
    }

    /**
     * Cuenta cuántos usuarios root existen
     */
    public long countRootUsers() {
        Long c = User.count("role = ?", "root");
        return c == null ? 0 : c;
    }
}
