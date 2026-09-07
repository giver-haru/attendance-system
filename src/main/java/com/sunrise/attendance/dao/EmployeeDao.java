package com.sunrise.attendance.dao;

import com.sunrise.attendance.model.Employee;
import com.sunrise.attendance.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmployeeDao {

    public Optional<Employee> findByCode(String employeeCode) {
        String sql = "SELECT * FROM employees WHERE employee_code = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employeeCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<Employee> findById(int id) {
        String sql = "SELECT * FROM employees WHERE id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Employee> findAll() {
        String sql = "SELECT * FROM employees ORDER BY employee_code";
        List<Employee> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) result.add(map(rs));
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    private Employee map(ResultSet rs) throws SQLException {
        Integer managerId = rs.getObject("manager_id") == null ? null : rs.getInt("manager_id");
        return new Employee(
                rs.getInt("id"),
                rs.getString("employee_code"),
                rs.getString("name"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")),
                rs.getString("department"),
                managerId,
                rs.getDate("hire_date").toLocalDate()
        );
    }
}
