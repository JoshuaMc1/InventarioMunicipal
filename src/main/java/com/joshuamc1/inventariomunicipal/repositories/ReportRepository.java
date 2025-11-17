package com.joshuamc1.inventariomunicipal.repositories;

import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.models.Report;
import org.javalite.activejdbc.Base;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ReportRepository {
    // Reporte de usuarios con roles
    public Report usersWithRoles() {
        String sql = "SELECT id, name, username, role, created_at FROM users ORDER BY name";
        Report res = new Report("Usuarios y roles");
        res.addHeader("ID"); res.addHeader("Nombre"); res.addHeader("Username"); res.addHeader("Rol"); res.addHeader("Creado");

        try (PreparedStatement ps = Base.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(rs.getInt("id")));
                row.add(rs.getString("name"));
                row.add(rs.getString("username"));
                row.add(rs.getString("role"));
                row.add(rs.getString("created_at"));
                res.addRow(row);
            }
        } catch (Exception e) {
            Logs.error(this.getClass(), "Error ejecutando usersWithRoles: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return res;
    }

    // Reporte de inventario total
    public Report inventoryTotal() {
        String sql = "SELECT id, name, unit, current_stock, minimum_stock, created_at FROM products ORDER BY name";
        Report res = new Report("Inventario total");
        res.addHeader("ID"); res.addHeader("Nombre"); res.addHeader("Unidad"); res.addHeader("Stock actual"); res.addHeader("Stock mínimo"); res.addHeader("Creado");

        try (PreparedStatement ps = Base.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(rs.getInt("id")));
                row.add(rs.getString("name"));
                row.add(rs.getString("unit"));
                row.add(String.valueOf(rs.getObject("current_stock")));
                row.add(String.valueOf(rs.getObject("minimum_stock")));
                row.add(rs.getString("created_at"));
                res.addRow(row);
            }
        } catch (Exception e) {
            Logs.error(this.getClass(), "Error ejecutando inventoryTotal: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return res;
    }

    // Reporte de inventario con stock bajo
    public Report inventoryLowStock() {
        String sql = "SELECT id, name, unit, current_stock, minimum_stock FROM products WHERE current_stock <= minimum_stock ORDER BY current_stock ASC";
        Report res = new Report("Inventario - Stock bajo");
        res.addHeader("ID"); res.addHeader("Nombre"); res.addHeader("Unidad"); res.addHeader("Stock actual"); res.addHeader("Stock mínimo");

        try (PreparedStatement ps = Base.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(rs.getInt("id")));
                row.add(rs.getString("name"));
                row.add(rs.getString("unit"));
                row.add(String.valueOf(rs.getObject("current_stock")));
                row.add(String.valueOf(rs.getObject("minimum_stock")));
                res.addRow(row);
            }
        } catch (Exception e) {
            Logs.error(this.getClass(), "Error ejecutando inventoryLowStock: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return res;
    }

    // Reporte de inventario por rango de fechas (created_at)
    public Report inventoryByDateRange(String from, String to) {
        String sql = "SELECT id, name, unit, current_stock, minimum_stock, created_at FROM products WHERE date(created_at) BETWEEN date(?) AND date(?) ORDER BY created_at";
        Report res = new Report("Inventario por rango de fechas");
        res.addHeader("ID"); res.addHeader("Nombre"); res.addHeader("Unidad"); res.addHeader("Stock actual"); res.addHeader("Stock mínimo"); res.addHeader("Creado");

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = Base.connection().prepareStatement(sql);
            ps.setString(1, from);
            ps.setString(2, to);
            rs = ps.executeQuery();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(rs.getInt("id")));
                row.add(rs.getString("name"));
                row.add(rs.getString("unit"));
                row.add(String.valueOf(rs.getObject("current_stock")));
                row.add(String.valueOf(rs.getObject("minimum_stock")));
                row.add(rs.getString("created_at"));
                res.addRow(row);
            }
        } catch (Exception e) {
            Logs.error(this.getClass(), "Error ejecutando inventoryByDateRange: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
        }
        return res;
    }

    // Reporte transacciones
    public Report transactions(String from, String to) {
        String sql = "SELECT t.id, p.name as producto, t.type, t.quantity, t.date, u.name as usuario, t.notes, t.created_at " +
                "FROM transactions t LEFT JOIN products p ON p.id = t.product_id LEFT JOIN users u ON u.id = t.user_id " +
                "WHERE date(t.created_at) BETWEEN date(?) AND date(?) ORDER BY t.created_at DESC";
        Report res = new Report("Transacciones");
        res.addHeader("ID"); res.addHeader("Producto"); res.addHeader("Tipo"); res.addHeader("Cantidad"); res.addHeader("Fecha"); res.addHeader("Usuario"); res.addHeader("Notas"); res.addHeader("Creado");

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = Base.connection().prepareStatement(sql);
            ps.setString(1, from);
            ps.setString(2, to);
            rs = ps.executeQuery();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(rs.getInt("id")));
                row.add(rs.getString("producto"));
                row.add(rs.getString("type"));
                row.add(String.valueOf(rs.getObject("quantity")));
                row.add(rs.getString("date"));
                row.add(rs.getString("usuario"));
                row.add(rs.getString("notes"));
                row.add(rs.getString("created_at"));
                res.addRow(row);
            }
        } catch (Exception e) {
            Logs.error(this.getClass(), "Error ejecutando transactions: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
        }
        return res;
    }

    // Reporte transacciones por tipo
    public Report transactionsByType(String type, String from, String to) {
        String sql = "SELECT t.id, p.name as producto, t.type, t.quantity, t.date, u.name as usuario, t.notes, t.created_at " +
                "FROM transactions t LEFT JOIN products p ON p.id = t.product_id LEFT JOIN users u ON u.id = t.user_id " +
                "WHERE t.type = ? AND date(t.created_at) BETWEEN date(?) AND date(?) ORDER BY t.created_at DESC";
        Report res = new Report("Transacciones por tipo: " + type);
        res.addHeader("ID"); res.addHeader("Producto"); res.addHeader("Tipo"); res.addHeader("Cantidad"); res.addHeader("Fecha"); res.addHeader("Usuario"); res.addHeader("Notas"); res.addHeader("Creado");

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = Base.connection().prepareStatement(sql);
            ps.setString(1, type);
            ps.setString(2, from);
            ps.setString(3, to);
            rs = ps.executeQuery();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(rs.getInt("id")));
                row.add(rs.getString("producto"));
                row.add(rs.getString("type"));
                row.add(String.valueOf(rs.getObject("quantity")));
                row.add(rs.getString("date"));
                row.add(rs.getString("usuario"));
                row.add(rs.getString("notes"));
                row.add(rs.getString("created_at"));
                res.addRow(row);
            }
        } catch (Exception e) {
            Logs.error(this.getClass(), "Error ejecutando transactionsByType: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
        }
        return res;
    }

    // Reporte transacciones por persona
    public Report transactionsByPerson(int personId, String from, String to) {
        String sql = "SELECT t.id, p.name as producto, t.type, t.quantity, t.date, u.name as usuario, t.person_id, t.notes, t.created_at " +
                "FROM transactions t LEFT JOIN products p ON p.id = t.product_id LEFT JOIN users u ON u.id = t.user_id " +
                "WHERE t.person_id = ? AND date(t.created_at) BETWEEN date(?) AND date(?) ORDER BY t.created_at DESC";
        Report res = new Report("Transacciones por persona");
        res.addHeader("ID"); res.addHeader("Producto"); res.addHeader("Tipo"); res.addHeader("Cantidad"); res.addHeader("Fecha"); res.addHeader("Usuario"); res.addHeader("Person ID"); res.addHeader("Notas"); res.addHeader("Creado");

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = Base.connection().prepareStatement(sql);
            ps.setInt(1, personId);
            ps.setString(2, from);
            ps.setString(3, to);
            rs = ps.executeQuery();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(rs.getInt("id")));
                row.add(rs.getString("producto"));
                row.add(rs.getString("type"));
                row.add(String.valueOf(rs.getObject("quantity")));
                row.add(rs.getString("date"));
                row.add(rs.getString("usuario"));
                row.add(String.valueOf(rs.getObject("person_id")));
                row.add(rs.getString("notes"));
                row.add(rs.getString("created_at"));
                res.addRow(row);
            }
        } catch (Exception e) {
            Logs.error(this.getClass(), "Error ejecutando transactionsByPerson: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
        }
        return res;
    }

    // Reporte de personas registradas
    public Report peopleRegistered(String from, String to) {
        String sql = "SELECT id, dni, first_name, last_name, phone, internal_code, created_at FROM people WHERE date(created_at) BETWEEN date(?) AND date(?) ORDER BY created_at DESC";
        Report res = new Report("Personas registradas");
        res.addHeader("ID"); res.addHeader("DNI"); res.addHeader("Nombre"); res.addHeader("Apellido"); res.addHeader("Teléfono"); res.addHeader("Código interno"); res.addHeader("Creado");

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = Base.connection().prepareStatement(sql);
            ps.setString(1, from);
            ps.setString(2, to);
            rs = ps.executeQuery();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(rs.getInt("id")));
                row.add(rs.getString("dni"));
                row.add(rs.getString("first_name"));
                row.add(rs.getString("last_name"));
                row.add(rs.getString("phone"));
                row.add(rs.getString("internal_code"));
                row.add(rs.getString("created_at"));
                res.addRow(row);
            }
        } catch (Exception e) {
            Logs.error(this.getClass(), "Error ejecutando peopleRegistered: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
        }
        return res;
    }
}

