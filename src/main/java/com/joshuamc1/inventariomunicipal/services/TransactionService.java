package com.joshuamc1.inventariomunicipal.services;

import com.joshuamc1.inventariomunicipal.core.DatabaseManager;
import com.joshuamc1.inventariomunicipal.core.Session;
import com.joshuamc1.inventariomunicipal.models.Product;
import com.joshuamc1.inventariomunicipal.models.Transaction;
import com.joshuamc1.inventariomunicipal.repositories.ProductRepository;
import com.joshuamc1.inventariomunicipal.repositories.TransactionRepository;
import org.javalite.activejdbc.Base;

import java.util.List;

public class TransactionService {
    private final ProductRepository productRepo = new ProductRepository();
    private final TransactionRepository repo = new TransactionRepository();
    private final AuditService auditService = new AuditService();

    public Transaction createTransaction(Integer productId, String type,
                                         double quantity, Integer userId,
                                         Integer personId, String notes) {

        return DatabaseManager.callInConnection(() -> {

            Base.openTransaction();

            Product product = productRepo.findById(productId);

            if (product == null)
                throw new IllegalArgumentException("Product not found");

            if ("in".equals(type)) {
                product.updateStock(quantity);
            } else {
                product.updateStock(-quantity);
            }

            productRepo.save(product);

            Transaction t = new Transaction();
            t.set("product_id", productId);
            t.set("type", type);
            t.set("quantity", quantity);
            t.set("user_id", userId);
            t.set("person_id", personId);
            t.set("notes", notes);

            repo.create(t);

            auditService.logAction(
                    Session.getCurrentUserId(),
                    "create_transaction",
                    "transactions",
                    t.getInteger("id"),
                    String.format("product_id=%d,type=%s,quantity=%.2f,user_id=%d,person_id=%d,notes=%s",
                            productId, type, quantity, userId, personId, notes)
            );

            Base.commitTransaction();
            return t;
        });
    }

    public List<Transaction> findByProduct(Integer productId) {
        return DatabaseManager.callInConnection(() ->
                repo.findByProduct(productId)
        );
    }

    public List<Transaction> findByType(String type) {
        return DatabaseManager.callInConnection(() ->
                repo.findByType(type)
        );
    }

    public long countToday() {
        return DatabaseManager.callInConnection(repo::countToday);
    }

    public List<Transaction> findLatest(int limit) {
        return DatabaseManager.callInConnection(() -> repo.findLatest(limit));
    }
}