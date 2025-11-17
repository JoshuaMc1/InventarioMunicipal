package com.joshuamc1.inventariomunicipal.services;

import com.joshuamc1.inventariomunicipal.core.DatabaseManager;
import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.Session;
import com.joshuamc1.inventariomunicipal.models.User;
import com.joshuamc1.inventariomunicipal.repositories.UserRepository;

import java.util.List;

public class UserService {
    private final UserRepository repo = new UserRepository();
    private final AuditService auditService = new AuditService();

    public User login(String username, String password) {
        return DatabaseManager.callInConnection(() -> {
            User user = repo.findByUsername(username);

            if (user != null && user.validatePassword(password)) {
                auditService.logAction(
                        user.getInteger("id"),
                        "login",
                        "users",
                        user.getInteger("id"),
                        String.format("user_id=%d,username=%s", user.getInteger("id"), username)
                );
                return user;
            }

            return null;
        });
    }

    public User createUser(String username, String password, String role) {
        return DatabaseManager.callInConnection(() -> {
            // password en texto plano: el modelo User.setPassword hará el hash
            User user = repo.create(username, password, role);

            auditService.logAction(
                    Session.getCurrentUserId(),
                    "create_user",
                    "users",
                    user.getInteger("id"),
                    String.format("user_id=%d,username=%s,role=%s", user.getInteger("id"), username, role)
            );

            return user;
        });
    }

    public User createUserFull(String name, String username, String password, String role) {
        return DatabaseManager.callInConnection(() -> {
            // password en texto plano: el modelo User.setPassword hará el hash
            User user = repo.createFull(name, username, password, role);

            auditService.logAction(
                    Session.getCurrentUserId(),
                    "create_user",
                    "users",
                    user.getInteger("id"),
                    String.format("user_id=%d,name=%s,username=%s,role=%s", user.getInteger("id"), name, username, role)
            );

            return user;
        });
    }

    public boolean updateUser(Integer id, String name, String username, String role, String newPassword) {
        return DatabaseManager.callInConnection(() -> {
            boolean ok = repo.update(id, name, username, role, newPassword);
            if (ok) {
                auditService.logAction(
                    Session.getCurrentUserId(),
                    "update_user",
                    "users",
                    id,
                    String.format("user_id=%d,username=%s,role=%s", id, username, role)
                );
            }
            return ok;
        });
    }

    public boolean deleteUser(Integer id) {
        return DatabaseManager.callInConnection(() -> {
            boolean ok = repo.delete(id);

            if (ok) {
                auditService.logAction(
                        Session.getCurrentUserId(),
                        "delete_user",
                        "users",
                        id,
                        "user_id=" + id
                );
            }
            return ok;
        });
    }

    public User findById(Integer id) {
        return DatabaseManager.callInConnection(() -> repo.findById(id));
    }

    public long countUsers() {
        return DatabaseManager.callInConnection(repo::countUsers);
    }

    public List<User> listPaged(int offset, int limit, String search) {
        return DatabaseManager.callInConnection(() -> repo.listPaged(offset, limit, search));
    }
}