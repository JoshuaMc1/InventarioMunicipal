package com.joshuamc1.inventariomunicipal.repositories;

import com.joshuamc1.inventariomunicipal.models.Person;
import org.javalite.activejdbc.LazyList;

import java.util.List;

public class PersonRepository {

    public Person findById(Integer id) {
        return Person.findById(id);
    }

    public Person findByDni(String dni) {
        return Person.findFirst("dni = ?", dni);
    }

    public Person create(String dni, String firstName, String lastName, String phone, String internalCode) {
        Person person = new Person();
        person.set("dni", dni);
        person.set("first_name", firstName);
        person.set("last_name", lastName);
        person.set("phone", phone);
        person.set("internal_code", internalCode);
        person.saveIt();
        return person;
    }

    public boolean update(Integer id, String dni, String firstName, String lastName, String phone, String internalCode) {
        Person p = Person.findById(id);
        if (p == null) return false;
        p.set("dni", dni);
        p.set("first_name", firstName);
        p.set("last_name", lastName);
        p.set("phone", phone);
        p.set("internal_code", internalCode);
        return p.saveIt();
    }

    public boolean delete(Integer id) {
        Person p = Person.findById(id);
        if (p == null) return false;
        return p.delete();
    }

    public long count() {
        Long c = Person.count();
        return c == null ? 0L : c;
    }

    public List<Person> listPaged(int offset, int limit, String search) {
        if (search != null && !search.isBlank()) {
            LazyList<Person> list = Person.where(
                "dni LIKE ? OR first_name LIKE ? OR last_name LIKE ? OR internal_code LIKE ?",
                "%" + search + "%", "%" + search + "%", "%" + search + "%", "%" + search + "%"
            ).orderBy("created_at DESC").offset(offset).limit(limit);
            return list;
        } else {
            return Person.findAll().orderBy("created_at DESC").offset(offset).limit(limit);
        }
    }

    public List<Person> findAll() {
        return Person.findAll().orderBy("first_name, last_name");
    }
}

