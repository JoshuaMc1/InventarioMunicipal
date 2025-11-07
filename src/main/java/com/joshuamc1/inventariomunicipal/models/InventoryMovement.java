package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.jetbrains.annotations.NotNull;

@Table("movimientos_inventario")
public class InventoryMovement extends Model {
    static {
        validatePresenceOf("product_id", "user_id", "type", "sub_type", "quantity");
    }

    // Relations
    public Product getProduct() {
        return parent(Product.class);
    }

    public User getUser() {
        return parent(User.class);
    }

    @NotNull
    public static InventoryMovement recordEntry(@NotNull Product product, @NotNull User user, int amount, String subType, String note) {
        InventoryMovement movement = new InventoryMovement();
        movement.set("product_id", product.getId());
        movement.set("user_id", user.getId());
        movement.set("tipo", "INGRESO");
        movement.set("sub_tipo", subType);
        movement.set("cantidad", amount);
        movement.set("observaciones", note);
        movement.saveIt();

        product.increaseQuantity(amount);
        return movement;
    }

    public static @NotNull InventoryMovement recordExit(@NotNull Product product, @NotNull User user, int amount, String subType, String note) {
        InventoryMovement movement = new InventoryMovement();
        movement.set("product_id", product.getId());
        movement.set("user_id", user.getId());
        movement.set("tipo", "EGRESO");
        movement.set("sub_tipo", subType);
        movement.set("cantidad", amount);
        movement.set("observaciones", note);
        movement.saveIt();

        product.decreaseQuantity(amount);
        return movement;
    }
}
