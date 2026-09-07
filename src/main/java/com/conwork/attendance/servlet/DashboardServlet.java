package com.conwork.attendance.servlet;

import com.conwork.attendance.dao.AttendanceDao;
import com.conwork.attendance.dao.EmployeeDao;
import com.conwork.attendance.dao.LeaveRequestDao;
import com.conwork.attendance.model.AttendanceRecord;
import com.conwork.attendance.model.Employee;
import com.conwork.attendance.model.LeaveRequest;
import com.conwork.attendance.model.LeaveStatus;
import com.conwork.attendance.model.Role;
import com.conwork.attendance.util.Layout;
import com.conwork.attendance.util.LeaveBalanceCalculator;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
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
        String body = current.getRole() == Role.ADMIN ? adminBody(req.getParameter("department")) : employeeBody(current);
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write(Layout.page("ダッシュボード", current, body));
    }

    private String employeeBody(Employee current) {
        LocalDate today = LocalDate.now();
        Optional<AttendanceRecord> todayRecord = attendanceDao.findByEmployeeAndDate(current.getId(), today);
        List<AttendanceRecord> monthRecords = attendanceDao.findForMonth(current.getId(), YearMonth.now());

        long totalOvertime = monthRecords.stream().mapToLong(AttendanceRecord::getOvertimeMinutes).sum();
        long workedDays = monthRecords.stream().filter(r -> r.getClockIn() != null).count();

        LocalDate fiscalYearStart = LeaveBalanceCalculator.currentFiscalYearStart(today);
        int grantedLeaveDays = LeaveBalanceCalculator.grantedDays(current.getHireDate(), fiscalYearStart);
        long usedLeaveDays = leaveRequestDao.sumApprovedDaysSince(current.getId(), fiscalYearStart);
        long remainingLeaveDays = grantedLeaveDays - usedLeaveDays;

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
                    <h2>有給休暇</h2>
                    <div class="stats">
                        <div class="stat"><span class="num">%d</span><span class="label">残日数（付与%d日）</span></div>
                    </div>
                    <table>
                        <thead><tr><th>期間</th><th>理由</th><th>ステータス</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                    <a class="link" href="/leave/request">＋ 新しく有給を申請する</a>
                </section>
                """.formatted(Layout.escape(current.getName()), status, actionButton,
                YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy年M月")),
                workedDays, totalOvertime / 60.0,
                remainingLeaveDays, grantedLeaveDays,
                leaveRows.isEmpty() ? "<tr><td colspan=\"3\">申請履歴はありません</td></tr>" : leaveRows.toString());
    }

    private String adminBody(String departmentFilter) {
        List<Employee> team = employeeDao.findAll().stream()
                .filter(e -> e.getRole() == Role.EMPLOYEE)
                .filter(e -> departmentFilter == null || departmentFilter.isBlank() || departmentFilter.equals(e.getDepartment()))
                .toList();
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.now();
        boolean pastClockInDeadline = LocalTime.now().isAfter(LocalTime.of(10, 0));

        int missingClockInCount = 0;
        int overtimeWarningCount = 0;

        StringBuilder rows = new StringBuilder();
        for (Employee emp : team) {
            Optional<AttendanceRecord> record = attendanceDao.findByEmployeeAndDate(emp.getId(), today);
            String status;
            if (record.isEmpty()) {
                if (pastClockInDeadline) {
                    status = "<span class=\"badge badge-rejected\">打刻漏れ？</span>";
                    missingClockInCount++;
                } else {
                    status = "未出勤";
                }
            } else if (record.get().isClockedOut()) {
                status = "退勤済み（%s〜%s）".formatted(record.get().getClockIn(), record.get().getClockOut());
            } else {
                status = "出勤中（%s〜）".formatted(record.get().getClockIn());
            }

            double monthlyOvertimeHours = attendanceDao.findForMonth(emp.getId(), thisMonth).stream()
                    .mapToLong(AttendanceRecord::getOvertimeMinutes).sum() / 60.0;
            if (monthlyOvertimeHours >= 45) overtimeWarningCount++;

            rows.append("""
                    <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>
                    """.formatted(Layout.escape(emp.getName()), Layout.escape(emp.getDepartment()), status,
                    overtimeBadge(monthlyOvertimeHours)));
        }

        StringBuilder deptOptions = new StringBuilder("<option value=\"\">全部署</option>");
        for (String dept : employeeDao.findAllDepartments()) {
            boolean selected = dept.equals(departmentFilter);
            deptOptions.append("<option value=\"%s\"%s>%s</option>"
                    .formatted(Layout.escape(dept), selected ? " selected" : "", Layout.escape(dept)));
        }

        int pendingCount = leaveRequestDao.findPending().size();

        List<LeaveRequest> upcomingLeaves = leaveRequestDao.findApprovedUpcoming(today);
        StringBuilder upcomingRows = new StringBuilder();
        for (LeaveRequest lr : upcomingLeaves.stream().limit(10).toList()) {
            String name = employeeDao.findById(lr.getEmployeeId()).map(Employee::getName).orElse("不明");
            upcomingRows.append("""
                    <tr><td>%s</td><td>%s 〜 %s（%d日）</td></tr>
                    """.formatted(Layout.escape(name), lr.getStartDate(), lr.getEndDate(), lr.getDays()));
        }

        return """
                <h1>管理者ダッシュボード</h1>
                <section class="card">
                    <h2>承認待ちの有給申請</h2>
                    <p class="status">現在 <strong>%d件</strong> の申請が承認待ちです。</p>
                    <a class="link" href="/leave/approve">承認画面へ</a>
                </section>
                <section class="card">
                    <h2>アラート</h2>
                    <div class="stats">
                        <div class="stat"><span class="num">%d</span><span class="label">本日 打刻漏れの可能性</span></div>
                        <div class="stat"><span class="num">%d</span><span class="label">今月 残業45h超（36協定注意）</span></div>
                    </div>
                </section>
                <section class="card">
                    <h2>本日のチーム勤怠状況</h2>
                    <form method="get" action="/dashboard" class="inline">
                        <select name="department">%s</select>
                        <button type="submit">絞り込む</button>
                    </form>
                    <table>
                        <thead><tr><th>氏名</th><th>部署</th><th>状況</th><th>今月の残業</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                    <a class="link" href="/attendance">勤怠一覧（過去月も見られます）</a>
                    <a class="link" href="/export/csv">今月の勤怠をCSVで出力</a>
                    <a class="link" href="/export/payroll-csv">給与計算用CSVを出力</a>
                </section>
                <section class="card">
                    <h2>今後の有給予定</h2>
                    <table>
                        <thead><tr><th>氏名</th><th>期間</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                </section>
                """.formatted(pendingCount, missingClockInCount, overtimeWarningCount,
                deptOptions.toString(), rows.toString(),
                upcomingRows.isEmpty() ? "<tr><td colspan=\"2\">今後の有給予定はありません</td></tr>" : upcomingRows.toString());
    }

    private String overtimeBadge(double hours) {
        if (hours >= 80) return "<span class=\"badge badge-rejected\">%.1fh（危険）</span>".formatted(hours);
        if (hours >= 45) return "<span class=\"badge badge-pending\">%.1fh（注意）</span>".formatted(hours);
        return "%.1fh".formatted(hours);
    }

    private String statusLabel(LeaveStatus status) {
        return switch (status) {
            case PENDING -> "承認待ち";
            case APPROVED -> "承認済み";
            case REJECTED -> "却下";
        };
    }
}
