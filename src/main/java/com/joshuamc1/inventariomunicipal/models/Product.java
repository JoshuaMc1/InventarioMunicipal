package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("productos")
public class Product extends Model {
    static {
        validatePresenceOf("name", "product_code", "quantity");
        validateNumericalityOf("quantity");
    }

    public void increaseQuantity(int amount) {
        set("cantidad", getInteger("cantidad") + amount);
        saveIt();
    }

    public void decreaseQuantity(int amount) {
        int current = getInteger("cantidad");
        if(current < amount) throw new IllegalArgumentException("Insufficient stock");
        set("cantidad", current - amount);
        saveIt();
    }

    public int getQuantity() {
        return getInteger("cantidad");
    }
}
