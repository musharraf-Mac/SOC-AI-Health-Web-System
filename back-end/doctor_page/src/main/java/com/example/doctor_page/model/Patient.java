package com.example.doctor_page.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "test_booked_patients")
public class Patient {
    @Id
    private String id;
    private String name;
    private String email;
    private int age;
    private String disease;

    // Constructors
    public Patient() {}

    public Patient(String name, String email, int age, String disease) {
        this.name = name;
        this.email = email;
        this.age = age;
        this.disease = disease;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getDisease() {
        return disease;
    }

    public void setDisease(String disease) {
        this.disease = disease;
    }
}


