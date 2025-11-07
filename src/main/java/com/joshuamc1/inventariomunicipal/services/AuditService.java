package com.joshuamc1.inventariomunicipal.services;

import com.joshuamc1.inventariomunicipal.models.Audit;
import com.joshuamc1.inventariomunicipal.models.User;

import java.util.List;

public class AuditService {
    // Log a custom action
    public void logAction(User user, String action, String table, Integer recordId, String details) {
        Audit.logAction(user, action, table, recordId, details);
    }

    // Get all audit entries
    public List<Audit> getAllAudit() {
        return Audit.findAll();
    }
}
