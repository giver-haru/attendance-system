package com.conwork.attendance.servlet;

import com.conwork.attendance.dao.EmployeeDao;
import com.conwork.attendance.dao.LeaveRequestDao;
import com.conwork.attendance.model.Employee;
import com.conwork.attendance.model.LeaveRequest;
import com.conwork.attendance.model.LeaveStatus;
import com.conwork.attendance.util.Layout;
import com.conwork.attendance.util.LeaveBalanceCalculator;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class LeaveApprovalServlet extends HttpServlet {

    private final LeaveRequestDao leaveRequestDao = new LeaveRequestDao();
    private final EmployeeDao employeeDao = new EmployeeDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Employee current = (Employee) req.getSession().getAttribute("employee");
        List<LeaveRequest> pending = leaveRequestDao.findPending();

        StringBuilder rows = new StringBuilder();
        for (LeaveRequest lr : pending) {
            Optional<Employee> applicant = employeeDao.findById(lr.getEmployeeId());
            String applicantName = applicant.map(Employee::getName).orElse("不明");
            long remaining = applicant.map(this::remainingDays).orElse(0L);
            rows.append("""
                    <tr>
                        <td>%s</td>
                        <td>%s 〜 %s（%d日）</td>
                        <td>%s</td>
                        <td>残り%d日</td>
                        <td>
                            <form method="post" action="/leave/approve" class="inline">
                                <input type="hidden" name="requestId" value="%d">
                                <button type="submit" name="decision" value="APPROVED" class="approve">承認</button>
                                <button type="submit" name="decision" value="REJECTED" class="reject">却下</button>
                            </form>
                        </td>
                    </tr>
                    """.formatted(Layout.escape(applicantName), lr.getStartDate(), lr.getEndDate(), lr.getDays(),
                    Layout.escape(lr.getReason()), remaining, lr.getId()));
        }

        String body = """
                <h1>有給申請の承認</h1>
                <section class="card">
                    <table>
                        <thead><tr><th>申請者</th><th>期間</th><th>理由</th><th>有給残日数</th><th>操作</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                </section>
                """.formatted(rows.isEmpty() ? "<tr><td colspan=\"5\">承認待ちの申請はありません</td></tr>" : rows.toString());

        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write(Layout.page("有給承認", current, body));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Employee current = (Employee) req.getSession().getAttribute("employee");
        int requestId = Integer.parseInt(req.getParameter("requestId"));
        LeaveStatus decision = LeaveStatus.valueOf(req.getParameter("decision"));
        leaveRequestDao.updateStatus(requestId, decision, current.getId());
        resp.sendRedirect(req.getContextPath() + "/leave/approve");
    }

    private long remainingDays(Employee employee) {
        LocalDate fiscalYearStart = LeaveBalanceCalculator.currentFiscalYearStart(LocalDate.now());
        int granted = LeaveBalanceCalculator.grantedDays(employee.getHireDate(), fiscalYearStart);
        long used = leaveRequestDao.sumApprovedDaysSince(employee.getId(), fiscalYearStart);
        return granted - used;
    }
}
