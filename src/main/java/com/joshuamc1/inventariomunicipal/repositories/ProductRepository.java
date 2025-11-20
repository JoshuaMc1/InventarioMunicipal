package com.joshuamc1.inventariomunicipal.repositories;

import com.joshuamc1.inventariomunicipal.models.Product;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProductRepository {
    public List<Product> findAll() {
        return Product.findAll();
    }

    public Product findById(Integer id) {
        return Product.findById(id);
    }

    public Product create(String name, String description, String unit, double currentStock, double minimumStock) {
        Product product = new Product();
        product.set("name", name);
        product.set("description", description);
        product.set("unit", unit);
        product.set("current_stock", currentStock);
        product.set("minimum_stock", minimumStock);
        product.saveIt();
        return product;
    }

    public List<Product> findLowStock() {
        return Product.where("current_stock <= minimum_stock");
    }

    public void save(@NotNull Product product) {
        product.saveIt();
    }

    // Nuevos métodos para contadores
    public long countAll() {
        Long c = Product.count();
        return c == null ? 0L : c.longValue();
    }

    public long countLowStock() {
        return (long) Product.where("current_stock < minimum_stock").size();
    }

    public long countOutOfStock() {
        Long c = Product.count("current_stock = 0");
        return c == null ? 0L : c;
    }

    public long countCreatedThisMonth() {
        Long c = Product.count("YEAR(created_at) = YEAR(CURDATE()) AND MONTH(created_at) = MONTH(CURDATE())");
        return c == null ? 0L : c;
    }

    public long countCreatedLastMonth() {
        Long c = Product.count("YEAR(created_at) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) AND MONTH(created_at) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))");
        return c == null ? 0L : c;
    }

    // Borra un producto de la base de datos
    public void delete(@NotNull Product product) {
        // ActiveJDBC Model provee delete() para eliminar el registro
        product.delete();
    }

    // Actualiza (guarda) los cambios en un producto
    public void update(@NotNull Product product) {
        product.saveIt();
    }
}
