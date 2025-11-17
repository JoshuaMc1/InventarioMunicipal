package com.joshuamc1.inventariomunicipal.services;

import com.joshuamc1.inventariomunicipal.core.DatabaseManager;
import com.joshuamc1.inventariomunicipal.models.Report;
import com.joshuamc1.inventariomunicipal.repositories.ReportRepository;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReportService {
    private final ReportRepository repo = new ReportRepository();

    public Report usersWithRoles() {
        return DatabaseManager.callInConnection(repo::usersWithRoles);
    }

    public Report inventoryTotal() {
        return DatabaseManager.callInConnection(repo::inventoryTotal);
    }

    public Report inventoryLowStock() {
        return DatabaseManager.callInConnection(repo::inventoryLowStock);
    }

    public Report inventoryByDateRange(String from, String to) {
        String[] r = normalizeRange(from, to);
        return DatabaseManager.callInConnection(() -> repo.inventoryByDateRange(r[0], r[1]));
    }

    public Report transactions(String from, String to) {
        String[] r = normalizeRange(from, to);
        return DatabaseManager.callInConnection(() -> repo.transactions(r[0], r[1]));
    }

    public Report transactionsByType(String type, String from, String to) {
        String[] r = normalizeRange(from, to);
        return DatabaseManager.callInConnection(() -> repo.transactionsByType(type, r[0], r[1]));
    }

    public Report transactionsByPerson(int personId, String from, String to) {
        String[] r = normalizeRange(from, to);
        return DatabaseManager.callInConnection(() -> repo.transactionsByPerson(personId, r[0], r[1]));
    }

    public Report peopleRegistered(String from, String to) {
        String[] r = normalizeRange(from, to);
        return DatabaseManager.callInConnection(() -> repo.peopleRegistered(r[0], r[1]));
    }

    // --- helpers ---
    @Contract("_, _ -> new")
    private String @NotNull [] normalizeRange(String from, String to) {
        // Defaults amplios si vienen nulos
        LocalDate f = (from != null && !from.isBlank()) ? LocalDate.parse(from) : LocalDate.of(1900, 1, 1);
        LocalDate t = (to != null && !to.isBlank()) ? LocalDate.parse(to) : LocalDate.of(9999, 12, 31);
        if (f.isAfter(t)) { LocalDate tmp = f; f = t; t = tmp; }
        return new String[]{ f.format(DateTimeFormatter.ISO_DATE), t.format(DateTimeFormatter.ISO_DATE) };
    }
}
