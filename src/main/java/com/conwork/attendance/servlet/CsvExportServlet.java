package com.conwork.attendance.servlet;

import com.conwork.attendance.dao.AttendanceDao;
import com.conwork.attendance.dao.EmployeeDao;
import com.conwork.attendance.model.AttendanceRecord;
import com.conwork.attendance.model.Employee;
import com.conwork.attendance.model.Role;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;

public class CsvExportServlet extends HttpServlet {

    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final EmployeeDao employeeDao = new EmployeeDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        YearMonth month = YearMonth.now();
        List<Employee> team = employeeDao.findAll().stream().filter(e -> e.getRole() == Role.EMPLOYEE).toList();

        resp.setContentType("text/csv");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setHeader("Content-Disposition", "attachment; filename=\"attendance_%s.csv\"".formatted(month));

        PrintWriter writer = resp.getWriter();
        writer.write(0xFEFF);
        writer.println("氏名,部署,日付,出勤,退勤,残業時間(h)");

        for (Employee emp : team) {
            List<AttendanceRecord> records = attendanceDao.findForMonth(emp.getId(), month);
            for (AttendanceRecord r : records) {
                writer.printf("%s,%s,%s,%s,%s,%.1f%n",
                        emp.getName(), emp.getDepartment(), r.getWorkDate(),
                        r.getClockIn() == null ? "" : r.getClockIn(),
                        r.getClockOut() == null ? "" : r.getClockOut(),
                        r.getOvertimeMinutes() / 60.0);
            }
        }
        writer.flush();
    }
}
