package com.sunrise.attendance.dao;

import com.sunrise.attendance.util.PasswordUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Random;

public final class Database {

    private static final String URL = "jdbc:h2:file:./data/attendance;AUTO_SERVER=TRUE";

    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, "sa", "");
    }

    public static void initialize() {
        try {
            Files.createDirectories(Path.of("data"));
        } catch (Exception e) {
            throw new IllegalStateException("dataディレクトリの作成に失敗しました", e);
        }

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS employees (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        employee_code VARCHAR(20) UNIQUE NOT NULL,
                        name VARCHAR(50) NOT NULL,
                        password_hash VARCHAR(200) NOT NULL,
                        role VARCHAR(10) NOT NULL,
                        department VARCHAR(50) NOT NULL,
                        manager_id INT,
                        hire_date DATE NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS attendance_records (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        employee_id INT NOT NULL,
                        work_date DATE NOT NULL,
                        clock_in TIME,
                        clock_out TIME,
                        UNIQUE(employee_id, work_date)
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS leave_requests (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        employee_id INT NOT NULL,
                        start_date DATE NOT NULL,
                        end_date DATE NOT NULL,
                        reason VARCHAR(200),
                        status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
                        approver_id INT,
                        requested_at TIMESTAMP NOT NULL
                    )
                    """);

            seedIfEmpty(conn);
        } catch (SQLException e) {
            throw new IllegalStateException("データベース初期化に失敗しました", e);
        }
    }

    private static void seedIfEmpty(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            var rs = st.executeQuery("SELECT COUNT(*) FROM employees");
            rs.next();
            if (rs.getInt(1) > 0) return;
        }

        String adminHash = PasswordUtil.hash("admin123");
        String empHash = PasswordUtil.hash("password123");

        insertEmployee(conn, "admin001", "佐藤 部長", adminHash, "ADMIN", "管理部", null, "2015-04-01");
        insertEmployee(conn, "emp001", "鈴木 太郎", empHash, "EMPLOYEE", "製造部", 1, "2020-04-01");
        insertEmployee(conn, "emp002", "高橋 花子", empHash, "EMPLOYEE", "製造部", 1, "2021-04-01");
        insertEmployee(conn, "emp003", "田中 一郎", empHash, "EMPLOYEE", "営業部", 1, "2022-04-01");

        seedAttendance(conn);
        seedLeaveRequests(conn);
    }

    private static void insertEmployee(Connection conn, String code, String name, String hash,
                                        String role, String dept, Integer managerId, String hireDate) throws SQLException {
        String sql = "INSERT INTO employees (employee_code, name, password_hash, role, department, manager_id, hire_date) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, hash);
            ps.setString(4, role);
            ps.setString(5, dept);
            if (managerId == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, managerId);
            ps.setDate(7, Date.valueOf(hireDate));
            ps.executeUpdate();
        }
    }

    private static void seedAttendance(Connection conn) throws SQLException {
        Random random = new Random(42);
        String sql = "INSERT INTO attendance_records (employee_id, work_date, clock_in, clock_out) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            LocalDate today = LocalDate.now();
            for (int empId = 2; empId <= 4; empId++) {
                LocalDate date = today.minusDays(20);
                while (!date.isAfter(today)) {
                    if (date.getDayOfWeek().getValue() <= 5) {
                        LocalTime clockIn = LocalTime.of(9, random.nextInt(15));
                        int overtimeMinutes = random.nextInt(150);
                        LocalTime clockOut = LocalTime.of(18, 0).plusMinutes(overtimeMinutes);

                        ps.setInt(1, empId);
                        ps.setDate(2, Date.valueOf(date));
                        ps.setTime(3, Time.valueOf(clockIn));
                        if (date.isBefore(today)) {
                            ps.setTime(4, Time.valueOf(clockOut));
                        } else {
                            ps.setNull(4, Types.TIME);
                        }
                        ps.executeUpdate();
                    }
                    date = date.plusDays(1);
                }
            }
        }
    }

    private static void seedLeaveRequests(Connection conn) throws SQLException {
        String sql = "INSERT INTO leave_requests (employee_id, start_date, end_date, reason, status, approver_id, requested_at) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            LocalDate today = LocalDate.now();
            insertLeave(ps, 3, today.plusDays(5), today.plusDays(5), "私用のため", "PENDING", null);
            insertLeave(ps, 4, today.plusDays(10), today.plusDays(12), "帰省のため", "PENDING", null);
            insertLeave(ps, 2, today.minusDays(15), today.minusDays(14), "通院のため", "APPROVED", 1);
        }
    }

    private static void insertLeave(PreparedStatement ps, int employeeId, LocalDate start, LocalDate end,
                                     String reason, String status, Integer approverId) throws SQLException {
        ps.setInt(1, employeeId);
        ps.setDate(2, Date.valueOf(start));
        ps.setDate(3, Date.valueOf(end));
        ps.setString(4, reason);
        ps.setString(5, status);
        if (approverId == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, approverId);
        ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
        ps.executeUpdate();
    }
}
