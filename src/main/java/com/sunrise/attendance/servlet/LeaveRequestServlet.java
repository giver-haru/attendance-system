package com.sunrise.attendance.servlet;

import com.sunrise.attendance.dao.LeaveRequestDao;
import com.sunrise.attendance.model.Employee;
import com.sunrise.attendance.util.Layout;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

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
            leaveRequestDao.create(current.getId(), start, end, reason.trim());
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (DateTimeParseException e) {
            render(resp, current, "日付の形式が正しくありません");
        }
    }

    private void render(HttpServletResponse resp, Employee current, String error) throws IOException {
        String errorHtml = error == null ? "" : "<p class=\"error\">%s</p>".formatted(Layout.escape(error));
        String body = """
                <h1>有給申請</h1>
                <section class="card">
                    %s
                    <form method="post" action="/leave/request" class="form">
                        <label>開始日<input type="date" name="startDate" required></label>
                        <label>終了日<input type="date" name="endDate" required></label>
                        <label>理由<textarea name="reason" rows="3" required></textarea></label>
                        <button type="submit" class="primary">申請する</button>
                    </form>
                </section>
                """.formatted(errorHtml);
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write(Layout.page("有給申請", current, body));
    }
}
