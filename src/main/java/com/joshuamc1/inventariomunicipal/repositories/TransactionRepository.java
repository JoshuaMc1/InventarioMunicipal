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

    // Nuevo: contar transacciones por tipo (útil para paginación)
    public long countByType(String type) {
        Long c = Transaction.count("type = ?", type);
        return c == null ? 0L : c;
    }

    // Nuevo: obtener página de transacciones por tipo usando LIMIT/OFFSET
    public List<Transaction> findByTypePage(String type, int limit, int offset) {
        // ActiveJDBC permite chaining de limit() y offset()
        return Transaction.where("type = ?", type)
                .orderBy("created_at DESC")
                .limit(limit)
                .offset(offset);
    }

    // Nuevo: contar movimientos del día
    public long countToday() {
        Long count = Transaction.count("DATE(created_at) = CURDATE()");
        return count == null ? 0L : count;
    }

    // Nuevo: obtener últimas N transacciones
    public List<Transaction> findLatest(int limit) {
        return Transaction.findAll().orderBy("created_at DESC").limit(limit);
    }

    public long countThisMonth() {
        Long c = Transaction.count("YEAR(created_at) = YEAR(CURDATE()) AND MONTH(created_at) = MONTH(CURDATE())");
        return c == null ? 0L : c;
    }

    public long countLastMonth() {
        Long c = Transaction.count("YEAR(created_at) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) AND MONTH(created_at) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))");
        return c == null ? 0L : c;
    }

    public long countActiveLoans() {
        Long c = Transaction.count("type = ?", "loan");
        return c == null ? 0L : c;
    }

    public List<Transaction> findLast30Days() {
        return Transaction.where("created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)")
            .orderBy("created_at DESC");
    }

    public java.util.Map<String, Long> getMostMovedProducts(int limit) {
        // Obtener productos con más transacciones usando conteo por producto
        java.util.Map<String, Long> result = new java.util.LinkedHashMap<>();

        // Obtener todas las transacciones y contar por producto
        List<Transaction> allTransactions = Transaction.findAll();
        java.util.Map<Integer, Long> countByProductId = new java.util.HashMap<>();

        for (Transaction t : allTransactions) {
            Integer productId = t.getInteger("product_id");
            if (productId != null) {
                countByProductId.merge(productId, 1L, Long::sum);
            }
        }

        // Ordenar por conteo y tomar top N
        countByProductId.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
            .limit(limit)
            .forEach(entry -> {
                com.joshuamc1.inventariomunicipal.models.Product p =
                    com.joshuamc1.inventariomunicipal.models.Product.findById(entry.getKey());
                if (p != null) {
                    String name = p.getString("name");
                    result.put(name != null ? name : "N/A", entry.getValue());
                }
            });

        return result;
    }

    public java.util.Map<String, Long> getTransactionsByType() {
        java.util.Map<String, Long> result = new java.util.HashMap<>();
        String[] types = {"in", "out", "loan"};
        for (String type : types) {
            Long count = Transaction.count("type = ?", type);
            if (count != null && count > 0) {
                result.put(type, count);
            }
        }
        return result;
    }

    public java.util.Map<Integer, Long> getWeeklyActivity() {
        // Obtener transacciones de últimos 30 días y agrupar por día de semana
        java.util.Map<Integer, Long> result = new java.util.HashMap<>();

        List<Transaction> recent = Transaction.where("created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)");

        for (Transaction t : recent) {
            String createdAt = t.getString("created_at");
            if (createdAt != null && createdAt.length() >= 10) {
                try {
                    java.time.LocalDate date = java.time.LocalDate.parse(createdAt.substring(0, 10));
                    // Java: 1=Lunes, 7=Domingo
                    int dayOfWeek = date.getDayOfWeek().getValue();
                    result.merge(dayOfWeek, 1L, Long::sum);
                } catch (Exception ignored) {}
            }
        }

        return result;
    }

    public long countByTypeThisMonth(String type) {
        Long c = Transaction.count("type = ? AND YEAR(created_at) = YEAR(CURDATE()) AND MONTH(created_at) = MONTH(CURDATE())", type);
        return c == null ? 0L : c;
    }

    public long countByTypeLastMonth(String type) {
        Long c = Transaction.count("type = ? AND YEAR(created_at) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) AND MONTH(created_at) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))", type);
        return c == null ? 0L : c;
    }

    public java.util.Map<String, java.util.Map<String, Long>> getQuarterlyTrend() {
        // Últimos 3 meses completos - procesar en memoria
        java.util.Map<String, java.util.Map<String, Long>> result = new java.util.LinkedHashMap<>();

        List<Transaction> recent = Transaction.where("created_at >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH)");

        for (Transaction t : recent) {
            String createdAt = t.getString("created_at");
            String type = t.getString("type");

            if (createdAt != null && createdAt.length() >= 7 && type != null) {
                try {
                    // Extraer año-mes (YYYY-MM)
                    String month = createdAt.substring(0, 7);

                    result.computeIfAbsent(month, k -> new java.util.HashMap<>());
                    result.get(month).merge(type, 1L, Long::sum);
                } catch (Exception ignored) {}
            }
        }

        // Ordenar por mes
        java.util.Map<String, java.util.Map<String, Long>> sortedResult = new java.util.LinkedHashMap<>();
        result.entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .forEach(entry -> sortedResult.put(entry.getKey(), entry.getValue()));

        return sortedResult;
    }
}
