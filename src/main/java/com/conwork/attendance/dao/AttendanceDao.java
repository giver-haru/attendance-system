package com.conwork.attendance.dao;

import com.conwork.attendance.model.AttendanceRecord;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttendanceDao {

    public Optional<AttendanceRecord> findByEmployeeAndDate(int employeeId, LocalDate date) {
        String sql = "SELECT * FROM attendance_records WHERE employee_id = ? AND work_date = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<AttendanceRecord> findForMonth(int employeeId, YearMonth month) {
        String sql = "SELECT * FROM attendance_records WHERE employee_id = ? AND work_date BETWEEN ? AND ? ORDER BY work_date";
        List<AttendanceRecord> result = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(month.atDay(1)));
            ps.setDate(3, Date.valueOf(month.atEndOfMonth()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    public void clockIn(int employeeId, LocalDate date, LocalTime time) {
        String sql = "MERGE INTO attendance_records (employee_id, work_date, clock_in) KEY (employee_id, work_date) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(time));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void clockOut(int employeeId, LocalDate date, LocalTime time) {
        String sql = "UPDATE attendance_records SET clock_out = ? WHERE employee_id = ? AND work_date = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTime(1, Time.valueOf(time));
            ps.setInt(2, employeeId);
            ps.setDate(3, Date.valueOf(date));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private AttendanceRecord map(ResultSet rs) throws SQLException {
        Time in = rs.getTime("clock_in");
        Time out = rs.getTime("clock_out");
        return new AttendanceRecord(
                rs.getInt("id"),
                rs.getInt("employee_id"),
                rs.getDate("work_date").toLocalDate(),
                in == null ? null : in.toLocalTime(),
                out == null ? null : out.toLocalTime()
        );
    }
}
