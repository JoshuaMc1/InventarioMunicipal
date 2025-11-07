package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("permisos")
public class Permission extends Model {
    static {
        validatePresenceOf("name");
    }
}
