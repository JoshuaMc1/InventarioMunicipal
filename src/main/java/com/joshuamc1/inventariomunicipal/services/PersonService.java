package com.joshuamc1.inventariomunicipal.services;

import com.joshuamc1.inventariomunicipal.core.DatabaseManager;
import com.joshuamc1.inventariomunicipal.core.Session;
import com.joshuamc1.inventariomunicipal.models.Person;
import com.joshuamc1.inventariomunicipal.repositories.PersonRepository;

import java.util.List;

public class PersonService {
    private final PersonRepository repo = new PersonRepository();
    private final AuditService auditService = new AuditService();

    public Person create(String dni, String firstName, String lastName, String phone, String internalCode) {
        return DatabaseManager.callInConnection(() -> {
            Person person = repo.create(dni, firstName, lastName, phone, internalCode);

            auditService.logAction(
                Session.getCurrentUserId(),
                "create_person",
                "people",
                person.getInteger("id"),
                String.format("person_id=%d,dni=%s,name=%s %s",
                    person.getInteger("id"), dni, firstName, lastName)
            );

            return person;
        });
    }

    public boolean update(Integer id, String dni, String firstName, String lastName, String phone, String internalCode) {
        return DatabaseManager.callInConnection(() -> {
            boolean ok = repo.update(id, dni, firstName, lastName, phone, internalCode);
            if (ok) {
                auditService.logAction(
                    Session.getCurrentUserId(),
                    "update_person",
                    "people",
                    id,
                    String.format("person_id=%d,dni=%s", id, dni)
                );
            }
            return ok;
        });
    }

    public boolean delete(Integer id) {
        return DatabaseManager.callInConnection(() -> {
            boolean ok = repo.delete(id);
            if (ok) {
                auditService.logAction(
                    Session.getCurrentUserId(),
                    "delete_person",
                    "people",
                    id,
                    "person_id=" + id
                );
            }
            return ok;
        });
    }

    public Person findById(Integer id) {
        return DatabaseManager.callInConnection(() -> repo.findById(id));
    }

    public Person findByDni(String dni) {
        return DatabaseManager.callInConnection(() -> repo.findByDni(dni));
    }

    public long count() {
        return DatabaseManager.callInConnection(repo::count);
    }

    public List<Person> listPaged(int offset, int limit, String search) {
        return DatabaseManager.callInConnection(() -> repo.listPaged(offset, limit, search));
    }

    public List<Person> findAll() {
        return DatabaseManager.callInConnection(repo::findAll);
    }
}

