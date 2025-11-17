package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("people")
public class Person extends Model {

    public String getFullName() {
        return getString("first_name") + " " + getString("last_name");
    }
}