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

    /**
     * Verifica si es admin (compatibilidad con sistema de roles)
     */
    public boolean isAdmin() {
        Rol rol = getRol();
        if (rol != null) {
            return "ADMIN".equalsIgnoreCase(rol.getString("nombre"));
        }
        return false;
    }

    /**
     * Verifica si es root (compatibilidad con sistema de roles)
     */
    public boolean isRoot() {
        return isRootRole();
    }

    /**
     * Verifica si es operador (compatibilidad con sistema de roles)
     */
    public boolean isOperator() {
        Rol rol = getRol();
        if (rol != null) {
            return "OPERADOR".equalsIgnoreCase(rol.getString("nombre"));
        }
        return false;
    }

    /**
     * Obtiene el rol del usuario desde el nuevo sistema
     */
    public Rol getRol() {
        Object rolId = get("rol_id");
        if (rolId != null) {
            return Rol.findById(rolId);
        }
        return null;
    }

    /**
     * Verifica si el usuario tiene el rol ROOT
     */
    public boolean isRootRole() {
        Rol rol = getRol();
        return rol != null && rol.isRoot();
    }

    /**
     * Verifica si el usuario tiene un permiso específico
     */
    public boolean tienePermiso(String modulo, String accion) {
        Rol rol = getRol();
        return rol != null && rol.tienePermiso(modulo, accion);
    }

    /**
     * Obtiene el nombre del rol del usuario
     */
    public String getRolNombre() {
        Rol rol = getRol();
        return rol != null ? rol.getString("nombre") : "SIN ROL";
    }
}