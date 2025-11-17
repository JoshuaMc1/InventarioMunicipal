package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("products")
public class Product extends Model {

    public boolean isLowStock() {
        Double currentStock = getDouble("current_stock");
        Double minimumStock = getDouble("minimum_stock");
        return currentStock != null && minimumStock != null && currentStock <= minimumStock;
    }

    public void updateStock(double quantity) {
        Double currentStock = getDouble("current_stock");
        set("current_stock", currentStock + quantity);
    }
}