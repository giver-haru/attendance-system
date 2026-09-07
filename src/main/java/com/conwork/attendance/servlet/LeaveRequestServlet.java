package com.conwork.attendance.servlet;

import com.conwork.attendance.dao.LeaveRequestDao;
import com.conwork.attendance.model.Employee;
import com.conwork.attendance.util.Layout;
import com.conwork.attendance.util.LeaveBalanceCalculator;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class LeaveRequestServlet extends HttpServlet {

    private final LeaveRequestDao leaveRequestDao = new LeaveRequestDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Employee current = (Employee) req.getSession().getAttribute("employee");
        render(resp, current, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Employee current = (Employee) req.getSession().getAttribute("employee");
        String startStr = req.getParameter("startDate");
        String endStr = req.getParameter("endDate");
        String reason = req.getParameter("reason");

        try {
            LocalDate start = LocalDate.parse(startStr);
            LocalDate end = LocalDate.parse(endStr);
            if (end.isBefore(start)) {
                render(resp, current, "終了日は開始日以降の日付を指定してください");
                return;
            }
            if (reason == null || reason.isBlank()) {
                render(resp, current, "申請理由を入力してください");
                return;
            }

            long requestedDays = ChronoUnit.DAYS.between(start, end) + 1;
            long remaining = remainingDays(current);
            if (requestedDays > remaining) {
                render(resp, current, "有給残日数が不足しています（残り%d日、申請%d日）".formatted(remaining, requestedDays));
                return;
            }

            leaveRequestDao.create(current.getId(), start, end, reason.trim());
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (DateTimeParseException e) {
            render(resp, current, "日付の形式が正しくありません");
        }
    }

    private long remainingDays(Employee employee) {
        LocalDate fiscalYearStart = LeaveBalanceCalculator.currentFiscalYearStart(LocalDate.now());
        int granted = LeaveBalanceCalculator.grantedDays(employee.getHireDate(), fiscalYearStart);
        long used = leaveRequestDao.sumApprovedDaysSince(employee.getId(), fiscalYearStart);
        return granted - used;
    }

    private void render(HttpServletResponse resp, Employee current, String error) throws IOException {
        String errorHtml = error == null ? "" : "<p class=\"error\">%s</p>".formatted(Layout.escape(error));
        String body = """
                <h1>有給申請</h1>
                <section class="card">
                    <p class="status">有給残日数: <strong>%d日</strong></p>
                    %s
                    <form method="post" action="/leave/request" class="form">
                        <label>開始日<input type="date" name="startDate" required></label>
                        <label>終了日<input type="date" name="endDate" required></label>
                        <label>理由<textarea name="reason" rows="3" required></textarea></label>
                        <button type="submit" class="primary">申請する</button>
                    </form>
                </section>
                """.formatted(remainingDays(current), errorHtml);
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write(Layout.page("有給申請", current, body));
    }
}
