package com.conwork.attendance.servlet;

import com.conwork.attendance.dao.AttendanceDao;
import com.conwork.attendance.dao.EmployeeDao;
import com.conwork.attendance.model.AttendanceRecord;
import com.conwork.attendance.model.Employee;
import com.conwork.attendance.model.Role;
import com.conwork.attendance.util.Layout;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class AttendanceListServlet extends HttpServlet {

    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final EmployeeDao employeeDao = new EmployeeDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Employee current = (Employee) req.getSession().getAttribute("employee");
        YearMonth month = parseMonth(req.getParameter("month"));
        String body = current.getRole() == Role.ADMIN
                ? adminSummary(month, req.getParameter("department"))
                : employeeDetail(current, month);
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write(Layout.page("勤怠一覧", current, body));
    }

    private YearMonth parseMonth(String monthParam) {
        YearMonth current = YearMonth.now();
        if (monthParam == null || monthParam.isBlank()) return current;
        try {
            YearMonth parsed = YearMonth.parse(monthParam);
            return parsed.isAfter(current) ? current : parsed;
        } catch (DateTimeParseException e) {
            return current;
        }
    }

    private String monthNav(YearMonth month, String extraQuery) {
        String query = extraQuery == null ? "" : extraQuery;
        return """
                <div class="status">
                    <a class="link" href="/attendance?month=%s%s">＜ 前月</a>
                    %s
                </div>
                """.formatted(month.minusMonths(1), query,
                month.equals(YearMonth.now()) ? "" : "<a class=\"link\" href=\"/attendance?month=%s%s\">翌月 ＞</a>".formatted(month.plusMonths(1), query));
    }

    private String employeeDetail(Employee current, YearMonth month) {
        List<AttendanceRecord> records = attendanceDao.findForMonth(current.getId(), month);

        StringBuilder rows = new StringBuilder();
        for (AttendanceRecord r : records) {
            rows.append("""
                    <tr>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%.1f h</td>
                    </tr>
                    """.formatted(r.getWorkDate(),
                    r.getClockIn() == null ? "-" : r.getClockIn(),
                    r.getClockOut() == null ? "-" : r.getClockOut(),
                    r.getOvertimeMinutes() / 60.0));
        }

        return """
                <h1>勤怠一覧（%s）</h1>
                <section class="card">
                    %s
                    <table>
                        <thead><tr><th>日付</th><th>出勤</th><th>退勤</th><th>残業</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                </section>
                """.formatted(month.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                monthNav(month, ""),
                rows.isEmpty() ? "<tr><td colspan=\"4\">記録がありません</td></tr>" : rows.toString());
    }

    private String adminSummary(YearMonth month, String departmentFilter) {
        List<Employee> team = employeeDao.findAll().stream()
                .filter(e -> e.getRole() == Role.EMPLOYEE)
                .filter(e -> departmentFilter == null || departmentFilter.isBlank() || departmentFilter.equals(e.getDepartment()))
                .toList();

        StringBuilder rows = new StringBuilder();
        for (Employee emp : team) {
            List<AttendanceRecord> records = attendanceDao.findForMonth(emp.getId(), month);
            long workedDays = records.stream().filter(r -> r.getClockIn() != null).count();
            long overtimeMinutes = records.stream().mapToLong(AttendanceRecord::getOvertimeMinutes).sum();
            rows.append("""
                    <tr>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%d 日</td>
                        <td>%.1f h</td>
                    </tr>
                    """.formatted(Layout.escape(emp.getName()), Layout.escape(emp.getDepartment()),
                    workedDays, overtimeMinutes / 60.0));
        }

        StringBuilder deptOptions = new StringBuilder("<option value=\"\">全部署</option>");
        for (String dept : employeeDao.findAllDepartments()) {
            boolean selected = dept.equals(departmentFilter);
            deptOptions.append("<option value=\"%s\"%s>%s</option>"
                    .formatted(Layout.escape(dept), selected ? " selected" : "", Layout.escape(dept)));
        }

        return """
                <h1>勤怠一覧（%s・全社員サマリー）</h1>
                <section class="card">
                    %s
                    <form method="get" action="/attendance" class="inline">
                        <input type="hidden" name="month" value="%s">
                        <select name="department">%s</select>
                        <button type="submit">絞り込む</button>
                    </form>
                    <table>
                        <thead><tr><th>氏名</th><th>部署</th><th>出勤日数</th><th>残業時間</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                    <a class="link" href="/export/csv">CSVで出力</a>
                </section>
                """.formatted(month.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                monthNav(month, departmentFilter == null || departmentFilter.isBlank() ? "" : "&department=" + departmentFilter),
                month, deptOptions.toString(), rows.toString());
    }
}
