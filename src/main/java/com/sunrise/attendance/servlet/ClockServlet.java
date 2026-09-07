package com.sunrise.attendance.servlet;

import com.sunrise.attendance.dao.AttendanceDao;
import com.sunrise.attendance.model.Employee;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

public class ClockServlet extends HttpServlet {

    private final AttendanceDao attendanceDao = new AttendanceDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Employee current = (Employee) req.getSession().getAttribute("employee");
        String action = req.getParameter("action");
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        if ("in".equals(action)) {
            attendanceDao.clockIn(current.getId(), today, now);
        } else if ("out".equals(action)) {
            attendanceDao.clockOut(current.getId(), today, now);
        }
        resp.sendRedirect(req.getContextPath() + "/dashboard");
    }
}
