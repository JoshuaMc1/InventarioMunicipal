package com.joshuamc1.inventariomunicipal.services;

import com.joshuamc1.inventariomunicipal.core.DatabaseManager;
import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.models.Audit;
import com.joshuamc1.inventariomunicipal.models.dto.AuditDTO;
import com.joshuamc1.inventariomunicipal.repositories.AuditRepository;

import java.time.LocalDate;
import java.util.List;

public class AuditService {
    private final AuditRepository repo = new AuditRepository();

    public void logAction(Integer userId, String action, String table, Integer recordId, String details) {
        DatabaseManager.runInConnection(() -> {
            Audit audit = new Audit();
            audit.set("user_id", userId);
            audit.set("action", action);
            audit.set("table_name", table);
            audit.set("record_id", recordId);
            audit.set("details", details);
            repo.save(audit);
        });

        Logs.info(getClass(), "Logged action: " + action + " on table: " + table + " for record ID: " + recordId);
    }

    public List<Audit> getAll() {
        return DatabaseManager.callInConnection(repo::findAll);
    }

    public List<AuditDTO> getAllWithUser() {
        return DatabaseManager.callInConnection(repo::findAllWithUser);
    }

    public List<AuditDTO> getPageWithUser(int offset, int limit, String search) {
        return DatabaseManager.callInConnection(() -> repo.findPageWithUser(offset, limit, search));
    }

    public int countWithFilter(String search) {
        return DatabaseManager.callInConnection(() -> repo.countWithFilter(search));
    }

    public List<AuditDTO> getAllWithUserByDateRange(LocalDate from, LocalDate to) {
        return DatabaseManager.callInConnection(() -> repo.findAllWithUserByDateRange(from, to));
    }
}
