package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.Table;

@Table("transactions")
@BelongsTo(parent = Product.class, foreignKeyName = "product_id")
public class Transaction extends Model {

    public static final String TYPE_IN = "in";
    public static final String TYPE_OUT = "out";
    public static final String TYPE_LOAN = "loan";
}