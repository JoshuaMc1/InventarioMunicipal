package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo para representar un item dentro de una transacción.
 * Cada item representa un producto con su cantidad dentro de una transacción multi-producto.
 */
@Table("transaction_items")
public class TransactionItem extends Model {

    /**
     * Obtiene el producto asociado a este item
     * @return Product asociado
     */
    public Product getProduct() {
        return this.parent(Product.class);
    }

    /**
     * Obtiene la transacción asociada a este item
     * @return Transaction asociada
     */
    public Transaction getTransaction() {
        return this.parent(Transaction.class);
    }

    /**
     * Obtiene la cantidad del producto en este item
     * @return cantidad
     */
    public Double getQuantity() {
        return getDouble("quantity");
    }

    /**
     * Obtiene las notas específicas de este item
     * @return notas del item
     */
    public String getNotes() {
        return getString("notes");
    }
}

