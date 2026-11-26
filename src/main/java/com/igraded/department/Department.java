package com.igraded.department;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A department such as Science or Math. Each is led by an admin (the department head). */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    protected Department() {
        // required by JPA
    }

    public Department(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
