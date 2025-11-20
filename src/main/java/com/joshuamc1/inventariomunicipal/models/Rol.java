package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

import java.util.List;

@Table("roles")
public class Rol extends Model {

    /**
     * Obtiene todos los permisos asociados a este rol
     */
    public List<Permiso> getPermisos() {
        String sql = "SELECT p.* FROM permisos p " +
                    "INNER JOIN rol_permisos rp ON p.id = rp.permiso_id " +
                    "WHERE rp.rol_id = ?";
        return Permiso.findBySQL(sql, getId());
    }

    /**
     * Asigna un permiso a este rol
     */
    public void asignarPermiso(int permisoId) {
        String sql = "INSERT INTO rol_permisos (rol_id, permiso_id) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE rol_id = rol_id";
        Base.exec(sql, getId(), permisoId);
    }

    /**
     * Elimina un permiso de este rol
     */
    public void removerPermiso(int permisoId) {
        String sql = "DELETE FROM rol_permisos WHERE rol_id = ? AND permiso_id = ?";
        Base.exec(sql, getId(), permisoId);
    }

    /**
     * Verifica si el rol tiene un permiso especifico
     */
    public boolean tienePermiso(String modulo, String accion) {
        String sql = "SELECT COUNT(*) as count FROM permisos p " +
                    "INNER JOIN rol_permisos rp ON p.id = rp.permiso_id " +
                    "WHERE rp.rol_id = ? AND p.modulo = ? AND p.accion = ?";
        Long count = (Long) Base.firstCell(sql, getId(), modulo, accion);
        return count != null && count > 0;
    }

    /**
     * Sincroniza los permisos del rol con una lista nueva
     */
    public void sincronizarPermisos(List<Integer> permisosIds) {
        // Eliminar todos los permisos actuales
        String deleteSql = "DELETE FROM rol_permisos WHERE rol_id = ?";
        Base.exec(deleteSql, getId());

        // Asignar los nuevos permisos
        if (permisosIds != null && !permisosIds.isEmpty()) {
            for (Integer permisoId : permisosIds) {
                asignarPermiso(permisoId);
            }
        }
    }

    /**
     * Obtiene todos los usuarios con este rol
     */
    public List<User> getUsuarios() {
        return User.where("rol_id = ?", getId());
    }

    /**
     * Verifica si el rol es ROOT
     */
    public boolean isRoot() {
        return "ROOT".equalsIgnoreCase(getString("nombre"));
    }

    /**
     * Verifica si el rol esta activo
     */
    public boolean isActivo() {
        Object activo = get("activo");
        return activo != null && (activo.equals(1) || activo.equals(true));
    }
}

