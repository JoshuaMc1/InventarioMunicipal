package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.HasMany;
import org.javalite.activejdbc.annotations.Table;

import java.util.List;

/**
 * Modelo para representar una transacción (header).
 * Una transacción puede contener múltiples productos (items).
 */
@Table("transactions")
@HasMany(child = TransactionItem.class, foreignKeyName = "transaction_id")
public class Transaction extends Model {

    public static final String TYPE_IN = "in";
    public static final String TYPE_OUT = "out";
    public static final String TYPE_LOAN = "loan";

    /**
     * Obtiene todos los items de esta transacción
     * @return lista de TransactionItem
     */
    public List<TransactionItem> getItems() {
        return this.getAll(TransactionItem.class);
    }

    /**
     * Agrega un producto a la transacción
     * @param productId ID del producto
     * @param quantity cantidad del producto
     * @param notes notas específicas del item
     * @return TransactionItem creado
     */
    public TransactionItem addItem(Integer productId, double quantity, String notes) {
        TransactionItem item = new TransactionItem();
        item.set("transaction_id", this.getId());
        item.set("product_id", productId);
        item.set("quantity", quantity);
        item.set("notes", notes);
        item.saveIt();

        // Actualizar contador
        this.set("total_items", this.getItems().size());
        this.saveIt();

        return item;
    }

    /**
     * Calcula la cantidad total de productos en la transacción
     * @return suma total de cantidades
     */
    public double getTotalQuantity() {
        return this.getItems().stream()
            .mapToDouble(item -> item.getDouble("quantity"))
            .sum();
    }

    /**
     * Obtiene el número total de items en la transacción
     * @return número de items
     */
    public Integer getTotalItems() {
        return getInteger("total_items");
    }
}