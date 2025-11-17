package com.joshuamc1.inventariomunicipal.repositories;

import com.joshuamc1.inventariomunicipal.models.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TransactionRepository {
    public Transaction create(@NotNull Transaction transaction) {
        transaction.saveIt();
        return transaction;
    }

    public List<Transaction> findByProduct(Integer productId) {
        return Transaction.where("product_id = ?", productId).orderBy("created_at DESC");
    }

    public List<Transaction> findByType(String type) {
        return Transaction.where("type = ?", type).orderBy("created_at DESC");
    }

    // Nuevo: contar movimientos del día
    public long countToday() {
        return (long) Transaction.where("date = date('now')").size();
    }

    // Nuevo: obtener últimas N transacciones
    public List<Transaction> findLatest(int limit) {
        return Transaction.findAll().orderBy("created_at DESC").limit(limit);
    }
}
