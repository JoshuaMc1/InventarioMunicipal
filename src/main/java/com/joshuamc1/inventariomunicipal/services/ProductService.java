package com.joshuamc1.inventariomunicipal.services;

import com.joshuamc1.inventariomunicipal.core.DatabaseManager;
import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.Session;
import com.joshuamc1.inventariomunicipal.models.Product;
import com.joshuamc1.inventariomunicipal.repositories.ProductRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProductService {
    private final ProductRepository repo = new ProductRepository();
    private final AuditService auditService = new AuditService();

    public List<Product> findAll() {
        return DatabaseManager.callInConnection(repo::findAll);
    }

    /**
     * Busca productos por nombre o ID con límite de resultados (optimizado para ComboBox)
     * @param query Texto a buscar (nombre o ID)
     * @param limit Número máximo de resultados
     * @return Lista limitada de productos
     */
    public List<Product> searchProducts(String query, int limit) {
        return DatabaseManager.callInConnection(() -> repo.searchProducts(query, limit));
    }

    public Product findById(Integer id) {
        return DatabaseManager.callInConnection(() -> repo.findById(id));
    }

    public Product createProduct(String name, String desc, String unit, double minStock) {
        return DatabaseManager.callInConnection(() ->
                repo.create(name, desc, unit, 0.0, minStock)
        );
    }

    public Product createProduct(String name, String desc, String unit, double currentStock, double minStock) {
        Product p = DatabaseManager.callInConnection(() -> repo.create(name, desc, unit, currentStock, minStock));
        try {
            Integer userId = Session.getCurrentUserId();
            String details = String.format("name=%s,unit=%s,current=%.2f,min=%.2f", name, unit, currentStock, minStock);

            auditService.logAction(userId, "create", "products", p.getInteger("id"), details);
        } catch (Exception e) {
            Logs.error(getClass(), e.getMessage(), e);
        }

        return p;
    }

    public List<Product> findLowStockProducts() {
        return DatabaseManager.callInConnection(repo::findLowStock);
    }

    public long countAll() {
        return DatabaseManager.callInConnection(repo::countAll);
    }

    public long countLowStock() {
        return DatabaseManager.callInConnection(repo::countLowStock);
    }

    public long countOutOfStock() {
        return DatabaseManager.callInConnection(repo::countOutOfStock);
    }

    public long countCreatedThisMonth() {
        return DatabaseManager.callInConnection(repo::countCreatedThisMonth);
    }

    public long countCreatedLastMonth() {
        return DatabaseManager.callInConnection(repo::countCreatedLastMonth);
    }

    public void save(Product product) {
        DatabaseManager.runInConnection(() -> {
            repo.save(product);
        });

        try {
            Integer userId = Session.getCurrentUserId();
            String details = String.format("id=%s,name=%s,unit=%s,current=%.2f,min=%.2f",
                    product.getInteger("id"), product.getString("name"), product.getString("unit"),
                    product.getDouble("current_stock") == null ? 0.0 : product.getDouble("current_stock"),
                    product.getDouble("minimum_stock") == null ? 0.0 : product.getDouble("minimum_stock"));
            auditService.logAction(userId, "update", "products", product.getInteger("id"), details);
        } catch (Exception ignored) {}
    }

    public void delete(@NotNull Product product) {
        Integer id = product.getInteger("id");
        String name = product.getString("name");
        DatabaseManager.runInConnection(() -> {

            repo.delete(product);
            auditService.logAction(
                    Session.getCurrentUserId(),
                    "delete",
                    "products",
                    id,
                    String.format("id=%s,name=%s", id, name)
            );
        });
    }
}