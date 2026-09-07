package com.sunrise.attendance.model;

import java.io.Serializable;
import java.time.LocalDate;

public class Employee implements Serializable {

    private final int id;
    private final String employeeCode;
    private final String name;
    private final String passwordHash;
    private final Role role;
    private final String department;
    private final Integer managerId;
    private final LocalDate hireDate;

    public Employee(int id, String employeeCode, String name, String passwordHash,
                     Role role, String department, Integer managerId, LocalDate hireDate) {
        this.id = id;
        this.employeeCode = employeeCode;
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
        this.department = department;
        this.managerId = managerId;
        this.hireDate = hireDate;
    }

    public int getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public String getName() { return name; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public String getDepartment() { return department; }
    public Integer getManagerId() { return managerId; }
    public LocalDate getHireDate() { return hireDate; }
}
