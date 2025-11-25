package com.joshuamc1.inventariomunicipal.services;

import com.joshuamc1.inventariomunicipal.core.DatabaseManager;
import com.joshuamc1.inventariomunicipal.core.Session;
import com.joshuamc1.inventariomunicipal.core.PermissionHelper;
import com.joshuamc1.inventariomunicipal.models.Product;
import com.joshuamc1.inventariomunicipal.models.Transaction;
import com.joshuamc1.inventariomunicipal.models.TransactionItem;
import com.joshuamc1.inventariomunicipal.models.dto.TransactionItemDTO;
import com.joshuamc1.inventariomunicipal.repositories.ProductRepository;
import com.joshuamc1.inventariomunicipal.repositories.TransactionRepository;
import com.joshuamc1.inventariomunicipal.repositories.TransactionItemRepository;
import org.javalite.activejdbc.Base;

import java.time.LocalDate;
import java.util.List;

public class TransactionService {
    private final ProductRepository productRepo = new ProductRepository();
    private final TransactionRepository repo = new TransactionRepository();
    private final TransactionItemRepository itemRepo = new TransactionItemRepository();
    private final AuditService auditService = new AuditService();

    public Transaction createTransaction(Integer productId, String type,
                                         double quantity, Integer userId,
                                         Integer personId, String notes) {

        // Validar permisos específicos por tipo; ROOT siempre pasa
        boolean isRoot = Session.getCurrentUser() != null && Session.getCurrentUser().isRootRole();
        boolean allowed = switch (type) {
            case "in" -> PermissionHelper.canAccess("INGRESOS", "CREATE");
            case "out" -> PermissionHelper.canAccess("EGRESOS", "CREATE");
            case "loan" -> PermissionHelper.canAccess("PRESTAMOS", "CREATE");
            default -> false;
        };
        if (!isRoot && !allowed) {
            throw new IllegalStateException("Acceso denegado: no tiene permiso para crear " + type);
        }

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

    // Sobrecarga para permitir fecha personalizada
    public Transaction createTransaction(Integer productId, String type,
                                         double quantity, String date,
                                         Integer personId, String notes) {

        boolean isRoot = Session.getCurrentUser() != null && Session.getCurrentUser().isRootRole();
        boolean allowed = switch (type) {
            case "in" -> PermissionHelper.canAccess("INGRESOS", "CREATE");
            case "out" -> PermissionHelper.canAccess("EGRESOS", "CREATE");
            case "loan" -> PermissionHelper.canAccess("PRESTAMOS", "CREATE");
            default -> false;
        };
        if (!isRoot && !allowed) {
            throw new IllegalStateException("Acceso denegado: no tiene permiso para crear " + type);
        }

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
            t.set("date", date);
            t.set("user_id", Session.getCurrentUserId());
            t.set("person_id", personId);
            t.set("notes", notes);

            repo.create(t);

            auditService.logAction(
                    Session.getCurrentUserId(),
                    "create_transaction",
                    "transactions",
                    t.getInteger("id"),
                    String.format("product_id=%d,type=%s,quantity=%.2f,date=%s,person_id=%s,notes=%s",
                            productId, type, quantity, date, personId, notes)
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

    public long countByType(String type) {
        return DatabaseManager.callInConnection(() -> repo.countByType(type));
    }

    public List<Transaction> findByTypePage(String type, int limit, int offset) {
        return DatabaseManager.callInConnection(() -> repo.findByTypePage(type, limit, offset));
    }

    public long countToday() {
        return DatabaseManager.callInConnection(repo::countToday);
    }

    public List<Transaction> findLatest(int limit) {
        return DatabaseManager.callInConnection(() -> repo.findLatest(limit));
    }

    public long countThisMonth() {
        return DatabaseManager.callInConnection(repo::countThisMonth);
    }

    public long countLastMonth() {
        return DatabaseManager.callInConnection(repo::countLastMonth);
    }

    public long countActiveLoans() {
        return DatabaseManager.callInConnection(repo::countActiveLoans);
    }

    public List<Transaction> findLast30Days() {
        return DatabaseManager.callInConnection(repo::findLast30Days);
    }

    public java.util.Map<String, Long> getMostMovedProducts(int limit) {
        return DatabaseManager.callInConnection(() -> repo.getMostMovedProducts(limit));
    }

    public java.util.Map<String, Long> getTransactionsByType() {
        return DatabaseManager.callInConnection(repo::getTransactionsByType);
    }

    public java.util.Map<Integer, Long> getWeeklyActivity() {
        return DatabaseManager.callInConnection(repo::getWeeklyActivity);
    }

    public long countByTypeThisMonth(String type) {
        return DatabaseManager.callInConnection(() -> repo.countByTypeThisMonth(type));
    }

    public long countByTypeLastMonth(String type) {
        return DatabaseManager.callInConnection(() -> repo.countByTypeLastMonth(type));
    }

    public java.util.Map<String, java.util.Map<String, Long>> getQuarterlyTrend() {
        return DatabaseManager.callInConnection(repo::getQuarterlyTrend);
    }

    /**
     * Crear transacción con múltiples productos
     * @param type Tipo de transacción (in, out, loan)
     * @param date Fecha de la transacción
     * @param userId Usuario que realiza la transacción
     * @param personId Persona relacionada (proveedor/beneficiario)
     * @param notes Notas generales de la transacción
     * @param items Lista de items con productos, cantidades y notas
     * @return Transaction creada con sus items
     */
    public Transaction createMultiProductTransaction(
            String type,
            String date,
            Integer userId,
            Integer personId,
            String notes,
            List<TransactionItemDTO> items) {

        // Validar permisos específicos por tipo; ROOT siempre pasa
        boolean isRoot = Session.getCurrentUser() != null && Session.getCurrentUser().isRootRole();
        boolean allowed = switch (type) {
            case "in" -> PermissionHelper.canAccess("INGRESOS", "CREATE");
            case "out" -> PermissionHelper.canAccess("EGRESOS", "CREATE");
            case "loan" -> PermissionHelper.canAccess("PRESTAMOS", "CREATE");
            default -> false;
        };

        if (!isRoot && !allowed) {
            throw new IllegalStateException("Acceso denegado: no tiene permiso para crear " + type);
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La transacción debe contener al menos un producto");
        }

        return DatabaseManager.callInConnection(() -> {
            Base.openTransaction();

            // 1. Crear header de transacción
            Transaction transaction = new Transaction();
            transaction.set("type", type);
            transaction.set("date", date != null ? date : LocalDate.now().toString());
            transaction.set("user_id", userId);
            transaction.set("person_id", personId);
            transaction.set("notes", notes);
            transaction.set("total_items", items.size());
            repo.create(transaction);

            Integer transactionId = transaction.getInteger("id");

            // 2. Crear items y actualizar stock
            for (TransactionItemDTO itemDTO : items) {
                Product product = productRepo.findById(itemDTO.getProductId());

                if (product == null) {
                    Base.rollbackTransaction();
                    throw new IllegalArgumentException("Producto no encontrado: " + itemDTO.getProductId());
                }

                // Actualizar stock según tipo
                if ("in".equals(type)) {
                    product.updateStock(itemDTO.getQuantity());
                } else {
                    product.updateStock(-itemDTO.getQuantity());
                }
                productRepo.save(product);

                // Crear item de transacción
                TransactionItem item = new TransactionItem();
                item.set("transaction_id", transactionId);
                item.set("product_id", itemDTO.getProductId());
                item.set("quantity", itemDTO.getQuantity());
                item.set("notes", itemDTO.getNotes());
                itemRepo.create(item);
            }

            // 3. Auditoría
            auditService.logAction(
                    userId,
                    "create_multi_transaction",
                    "transactions",
                    transactionId,
                    String.format("type=%s, total_items=%d", type, items.size())
            );

            Base.commitTransaction();
            return transaction;
        });
    }

    /**
     * Obtener todos los items de una transacción
     * @param transactionId ID de la transacción
     * @return lista de items
     */
    public List<TransactionItem> getTransactionItems(Integer transactionId) {
        return DatabaseManager.callInConnection(() ->
                itemRepo.findByTransactionId(transactionId)
        );
    }

    /**
     * Método actualizado para mantener compatibilidad con código existente
     * Internamente usa el nuevo método multi-producto con un solo item
     */
    public Transaction createTransactionCompat(Integer productId, String type,
                                               double quantity, Integer userId,
                                               Integer personId, String notes) {
        List<TransactionItemDTO> items = List.of(
                new TransactionItemDTO(productId, quantity, notes)
        );

        return createMultiProductTransaction(
                type,
                LocalDate.now().toString(),
                userId,
                personId,
                notes,
                items
        );
    }
}