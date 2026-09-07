package com.conwork.attendance.dao;

import com.conwork.attendance.model.LeaveRequest;
import com.conwork.attendance.model.LeaveStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LeaveRequestDao {

    public void create(int employeeId, LocalDate start, LocalDate end, String reason) {
        String sql = "INSERT INTO leave_requests (employee_id, start_date, end_date, reason, status, requested_at) VALUES (?,?,?,?,'PENDING',?)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            ps.setString(4, reason);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<LeaveRequest> findByEmployee(int employeeId) {
        String sql = "SELECT * FROM leave_requests WHERE employee_id = ? ORDER BY requested_at DESC";
        List<LeaveRequest> result = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    /** 指定日以降に開始する、承認済みの有給取得日数の合計を返す（残日数の消化分の計算に使う）。 */
    public long sumApprovedDaysSince(int employeeId, LocalDate since) {
        String sql = "SELECT start_date, end_date FROM leave_requests WHERE employee_id = ? AND status = 'APPROVED' AND start_date >= ?";
        long total = 0;
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(since));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate start = rs.getDate("start_date").toLocalDate();
                    LocalDate end = rs.getDate("end_date").toLocalDate();
                    total += java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return total;
    }

    /** 承認済みで、開始日が本日以降の有給申請を全社員分まとめて返す（チームの有給予定一覧に使う）。 */
    public List<LeaveRequest> findApprovedUpcoming(LocalDate today) {
        String sql = "SELECT * FROM leave_requests WHERE status = 'APPROVED' AND start_date >= ? ORDER BY start_date";
        List<LeaveRequest> result = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    public List<LeaveRequest> findPending() {
        String sql = "SELECT * FROM leave_requests WHERE status = 'PENDING' ORDER BY requested_at";
        List<LeaveRequest> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) result.add(map(rs));
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    public void updateStatus(int id, LeaveStatus status, int approverId) {
        String sql = "UPDATE leave_requests SET status = ?, approver_id = ? WHERE id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, approverId);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private LeaveRequest map(ResultSet rs) throws SQLException {
        Integer approverId = rs.getObject("approver_id") == null ? null : rs.getInt("approver_id");
        return new LeaveRequest(
                rs.getInt("id"),
                rs.getInt("employee_id"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getString("reason"),
                LeaveStatus.valueOf(rs.getString("status")),
                approverId,
                rs.getTimestamp("requested_at").toLocalDateTime()
        );
    }
}
