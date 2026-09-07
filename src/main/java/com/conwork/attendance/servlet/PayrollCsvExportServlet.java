package com.conwork.attendance.servlet;

import com.conwork.attendance.dao.AttendanceDao;
import com.conwork.attendance.dao.EmployeeDao;
import com.conwork.attendance.dao.LeaveRequestDao;
import com.conwork.attendance.model.AttendanceRecord;
import com.conwork.attendance.model.Employee;
import com.conwork.attendance.model.LeaveRequest;
import com.conwork.attendance.model.LeaveStatus;
import com.conwork.attendance.model.Role;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 給与計算ソフトへの引き渡しを想定した、社員1人1行の月次サマリーCSV。
 * 日次明細ではなく、実労働時間・残業時間・欠勤日数・有給消化日数だけに絞った集計を出力する。
 */
public class PayrollCsvExportServlet extends HttpServlet {

    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final EmployeeDao employeeDao = new EmployeeDao();
    private final LeaveRequestDao leaveRequestDao = new LeaveRequestDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        YearMonth month = YearMonth.now();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = today.isBefore(month.atEndOfMonth()) ? today : month.atEndOfMonth();

        List<Employee> team = employeeDao.findAll().stream().filter(e -> e.getRole() == Role.EMPLOYEE).toList();

        resp.setContentType("text/csv");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setHeader("Content-Disposition", "attachment; filename=\"payroll_%s.csv\"".formatted(month));

        PrintWriter writer = resp.getWriter();
        writer.write(0xFEFF);
        writer.println("社員コード,氏名,実労働時間(h),残業時間(h),欠勤日数,有給消化日数");

        long weekdaysInRange = countWeekdays(monthStart, monthEnd);

        for (Employee emp : team) {
            List<AttendanceRecord> records = attendanceDao.findForMonth(emp.getId(), month);

            double workedHours = records.stream()
                    .filter(r -> r.getClockIn() != null && r.getClockOut() != null)
                    .mapToDouble(r -> r.getWorkedMinutes() / 60.0)
                    .sum();
            double overtimeHours = records.stream().mapToDouble(r -> r.getOvertimeMinutes() / 60.0).sum();
            long workedDays = records.stream().filter(r -> r.getClockIn() != null).count();

            long paidLeaveDays = leaveRequestDao.findByEmployee(emp.getId()).stream()
                    .filter(lr -> lr.getStatus() == LeaveStatus.APPROVED)
                    .filter(lr -> overlapsRange(lr, monthStart, monthEnd))
                    .mapToLong(LeaveRequest::getDays)
                    .sum();

            long absenceDays = Math.max(0, weekdaysInRange - workedDays - paidLeaveDays);

            writer.printf("%s,%s,%.1f,%.1f,%d,%d%n",
                    emp.getEmployeeCode(), emp.getName(), workedHours, overtimeHours, absenceDays, paidLeaveDays);
        }
        writer.flush();
    }

    private boolean overlapsRange(LeaveRequest lr, LocalDate rangeStart, LocalDate rangeEnd) {
        return !lr.getStartDate().isAfter(rangeEnd) && !lr.getEndDate().isBefore(rangeStart);
    }

    private long countWeekdays(LocalDate start, LocalDate end) {
        long count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek().getValue() <= 5) count++;
        }
        return count;
    }
}
