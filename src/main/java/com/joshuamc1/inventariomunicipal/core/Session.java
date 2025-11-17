package com.joshuamc1.inventariomunicipal.core;

import com.joshuamc1.inventariomunicipal.models.User;
import org.jetbrains.annotations.Nullable;

public final class Session {
    private static volatile User currentUser = null;

    private Session() {}

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static @Nullable Integer getCurrentUserId() {
        if (currentUser == null) return null;
        return currentUser.getInteger("id");
    }

    public static @Nullable String getCurrentUserRole() {
        if (currentUser == null) return null;
        return currentUser.getString("role");
    }

    public static  String getCurrentUsername() {
        if (currentUser == null) return "Guest";
        return currentUser.getString("username");
    }
}

