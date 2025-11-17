package com.joshuamc1.inventariomunicipal.core;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Roles {
    /**
     * Get the full name of a role based on its identifier.
     */
    @Contract(pure = true)
    public static @NotNull String getRoleFullName(@NotNull String role) {
        return switch (role) {
            case "admin" -> "Administrador";
            case "guest" -> "Invitado";
            case "operator" -> "Operador";
            case "auditor" -> "Auditor";
            default -> "Desconocido";
        };
    }
}
