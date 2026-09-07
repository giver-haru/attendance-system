package com.sunrise.attendance.servlet;

import com.sunrise.attendance.dao.AttendanceDao;
import com.sunrise.attendance.dao.EmployeeDao;
import com.sunrise.attendance.dao.LeaveRequestDao;
import com.sunrise.attendance.model.AttendanceRecord;
import com.sunrise.attendance.model.Employee;
import com.sunrise.attendance.model.LeaveRequest;
import com.sunrise.attendance.model.LeaveStatus;
import com.sunrise.attendance.model.Role;
import com.sunrise.attendance.util.Layout;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class DashboardServlet extends HttpServlet {

    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final EmployeeDao employeeDao = new EmployeeDao();
    private final LeaveRequestDao leaveRequestDao = new LeaveRequestDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Employee current = (Employee) req.getSession().getAttribute("employee");
        String body = current.getRole() == Role.ADMIN ? adminBody() : employeeBody(current);
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write(Layout.page("ダッシュボード", current, body));
    }

    private String employeeBody(Employee current) {
        LocalDate today = LocalDate.now();
        Optional<AttendanceRecord> todayRecord = attendanceDao.findByEmployeeAndDate(current.getId(), today);
        List<AttendanceRecord> monthRecords = attendanceDao.findForMonth(current.getId(), YearMonth.now());

        long totalOvertime = monthRecords.stream().mapToLong(AttendanceRecord::getOvertimeMinutes).sum();
        long workedDays = monthRecords.stream().filter(r -> r.getClockIn() != null).count();

        String status;
        String actionButton;
        if (todayRecord.isEmpty()) {
            status = "本日はまだ出勤していません";
            actionButton = "<form method=\"post\" action=\"/clock\"><input type=\"hidden\" name=\"action\" value=\"in\"><button type=\"submit\" class=\"primary\">出勤する</button></form>";
        } else if (!todayRecord.get().isClockedOut()) {
            status = "出勤中（出勤 %s）".formatted(todayRecord.get().getClockIn());
            actionButton = "<form method=\"post\" action=\"/clock\"><input type=\"hidden\" name=\"action\" value=\"out\"><button type=\"submit\" class=\"primary\">退勤する</button></form>";
        } else {
            status = "本日の勤務は終了しました（%s 〜 %s）".formatted(todayRecord.get().getClockIn(), todayRecord.get().getClockOut());
            actionButton = "";
        }

        List<LeaveRequest> recentLeaves = leaveRequestDao.findByEmployee(current.getId());
        StringBuilder leaveRows = new StringBuilder();
        for (LeaveRequest lr : recentLeaves.stream().limit(5).toList()) {
            leaveRows.append("""
                    <tr>
                        <td>%s 〜 %s（%d日）</td>
                        <td>%s</td>
                        <td><span class="badge badge-%s">%s</span></td>
                    </tr>
                    """.formatted(lr.getStartDate(), lr.getEndDate(), lr.getDays(),
                    Layout.escape(lr.getReason()), lr.getStatus().name().toLowerCase(), statusLabel(lr.getStatus())));
        }

        return """
                <h1>ようこそ、%s さん</h1>
                <section class="card">
                    <h2>本日の勤怠</h2>
                    <p class="status">%s</p>
                    %s
                </section>
                <section class="card">
                    <h2>今月のサマリー（%s）</h2>
                    <div class="stats">
                        <div class="stat"><span class="num">%d</span><span class="label">出勤日数</span></div>
                        <div class="stat"><span class="num">%.1f</span><span class="label">残業時間（h）</span></div>
                    </div>
                </section>
                <section class="card">
                    <h2>有給申請の状況</h2>
                    <table>
                        <thead><tr><th>期間</th><th>理由</th><th>ステータス</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                    <a class="link" href="/leave/request">＋ 新しく有給を申請する</a>
                </section>
                """.formatted(Layout.escape(current.getName()), status, actionButton,
                YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy年M月")),
                workedDays, totalOvertime / 60.0,
                leaveRows.isEmpty() ? "<tr><td colspan=\"3\">申請履歴はありません</td></tr>" : leaveRows.toString());
    }

    private String adminBody() {
        List<Employee> team = employeeDao.findAll().stream().filter(e -> e.getRole() == Role.EMPLOYEE).toList();
        LocalDate today = LocalDate.now();

        StringBuilder rows = new StringBuilder();
        for (Employee emp : team) {
            Optional<AttendanceRecord> record = attendanceDao.findByEmployeeAndDate(emp.getId(), today);
            String status = record.isEmpty() ? "未出勤"
                    : record.get().isClockedOut() ? "退勤済み（%s〜%s）".formatted(record.get().getClockIn(), record.get().getClockOut())
                    : "出勤中（%s〜）".formatted(record.get().getClockIn());
            rows.append("""
                    <tr><td>%s</td><td>%s</td><td>%s</td></tr>
                    """.formatted(Layout.escape(emp.getName()), Layout.escape(emp.getDepartment()), status));
        }

        int pendingCount = leaveRequestDao.findPending().size();

        return """
                <h1>管理者ダッシュボード</h1>
                <section class="card">
                    <h2>承認待ちの有給申請</h2>
                    <p class="status">現在 <strong>%d件</strong> の申請が承認待ちです。</p>
                    <a class="link" href="/leave/approve">承認画面へ</a>
                </section>
                <section class="card">
                    <h2>本日のチーム勤怠状況</h2>
                    <table>
                        <thead><tr><th>氏名</th><th>部署</th><th>状況</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                    <a class="link" href="/export/csv">今月の勤怠をCSVで出力</a>
                </section>
                """.formatted(pendingCount, rows.toString());
    }

    private String statusLabel(LeaveStatus status) {
        return switch (status) {
            case PENDING -> "承認待ち";
            case APPROVED -> "承認済み";
            case REJECTED -> "却下";
        };
    }
}
