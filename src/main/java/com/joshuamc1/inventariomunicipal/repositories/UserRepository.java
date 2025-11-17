package com.joshuamc1.inventariomunicipal.repositories;

import com.joshuamc1.inventariomunicipal.models.User;
import org.javalite.activejdbc.LazyList;

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
        user.saveIt();

        return user;
    }

    public User createFull(String name, String username, String password, String role) {
        User user = new User();
        user.set("name", name);
        user.set("username", username);
        user.setPassword(password);
        user.set("role", role);
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
        }
        return u.saveIt();
    }

    public boolean delete(Integer id) {
        User u = User.findById(id);
        if (u == null) return false;
        return u.delete();
    }

    public long countUsers() {
        Long c = User.count();
        return c == null ? 0L : c.longValue();
    }

    public List<User> listPaged(int offset, int limit, String search) {
        if (search != null && !search.isBlank()) {
            LazyList<User> list = User.where("name LIKE ? OR username LIKE ?", "%"+search+"%", "%"+search+"%").orderBy("created_at DESC").offset(offset).limit(limit);
            return list;
        } else {
            return User.findAll().orderBy("created_at DESC").offset(offset).limit(limit);
        }
    }
}
