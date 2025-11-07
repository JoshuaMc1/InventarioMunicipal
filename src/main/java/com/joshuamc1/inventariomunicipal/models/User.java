package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

import java.util.List;

@Table("usuarios")
public class User extends Model {

    static {
        validatePresenceOf("username", "password_hash");
        validateEmailOf("email");
    }

    public void setPasswordHash(String hash) {
        set("password_hash", hash);
    }

    public String getPasswordHash() {
        return getString("password_hash");
    }

    public List<Role> getRoles() {
        return getAll(Role.class);
    }

    public void assignRole(Role role) {
        add(role);
    }

    public boolean hasRole(String roleName) {
        return getRoles().stream().anyMatch(r -> r.getString("nombre").equalsIgnoreCase(roleName));
    }
}