package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.mindrot.jbcrypt.BCrypt;

@Table("users")
public class User extends Model {
    public boolean validatePassword(String password) {
        return BCrypt.checkpw(password, getString("password"));
    }

    public void setPassword(String password) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        set("password", hashedPassword);
    }

    public boolean isAdmin() {
        return "admin".equals(getString("role"));
    }

    public boolean isRoot() {
        return "root".equals(getString("role"));
    }

    public boolean isOperator() {
        return "operator".equals(getString("role"));
    }
}