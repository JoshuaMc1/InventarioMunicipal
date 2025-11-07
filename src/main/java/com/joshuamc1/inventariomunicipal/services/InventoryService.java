package com.joshuamc1.inventariomunicipal.services;

import com.joshuamc1.inventariomunicipal.models.InventoryMovement;
import com.joshuamc1.inventariomunicipal.models.Product;
import com.joshuamc1.inventariomunicipal.models.User;

import java.util.List;

public class InventoryService {
    AuditService auditService = new AuditService();

    // Create a new product
    public Product createProduct(String code, String name, String brand, String model, String color, int year, String serialNumber, int quantity, User currentUser) {
        Product product = new Product();
        product.set("codigo_producto", code);
        product.set("nombre", name);
        product.set("marca", brand);
        product.set("modelo", model);
        product.set("color", color);
        product.set("anio", year);
        product.set("numero_serie", serialNumber);
        product.set("cantidad", quantity);
        product.saveIt();

        auditService.logAction(currentUser, "Created product", "productos", (Integer) product.getId(), "Initial stock: " + quantity);
        return product;
    }

    // Register inventory entry
    public InventoryMovement registerEntry(Product product, User user, int amount, String subType, String note) {
        InventoryMovement movement = InventoryMovement.recordEntry(product, user, amount, subType, note);
        auditService.logAction(user, "Registered inventory entry", "movimientos_inventario", (Integer) movement.getId(), note);
        return movement;
    }

    // Register inventory exit
    public InventoryMovement registerExit(Product product, User user, int amount, String subType, String note) {
        InventoryMovement movement = InventoryMovement.recordExit(product, user, amount, subType, note);
        auditService.logAction(user, "Registered inventory exit", "movimientos_inventario", (Integer) movement.getId(), note);
        return movement;
    }

    // Get all products
    public List<Product> getAllProducts() {
        return Product.findAll();
    }
}
