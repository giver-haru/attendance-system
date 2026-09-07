package com.conwork.attendance.servlet;

import com.conwork.attendance.dao.EmployeeDao;
import com.conwork.attendance.model.Employee;
import com.conwork.attendance.util.Layout;
import com.conwork.attendance.util.PasswordUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;

public class LoginServlet extends HttpServlet {

    private final EmployeeDao employeeDao = new EmployeeDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        renderForm(resp, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String code = req.getParameter("employeeCode");
        String password = req.getParameter("password");

        Optional<Employee> employeeOpt = employeeDao.findByCode(code == null ? "" : code.trim());
        if (employeeOpt.isEmpty() || !PasswordUtil.matches(password == null ? "" : password, employeeOpt.get().getPasswordHash())) {
            renderForm(resp, "社員コードまたはパスワードが正しくありません");
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("employee", employeeOpt.get());
        resp.sendRedirect(req.getContextPath() + "/dashboard");
    }

    private void renderForm(HttpServletResponse resp, String error) throws IOException {
        String errorHtml = error == null ? "" : "<p class=\"error\">%s</p>".formatted(Layout.escape(error));
        String body = """
                <div class="login-card">
                    <h1>ConWork</h1>
                    <p class="subtitle">勤怠管理システム</p>
                    %s
                    <form method="post" action="/login">
                        <label>社員コード<input type="text" name="employeeCode" required autofocus></label>
                        <label>パスワード<input type="password" name="password" required></label>
                        <button type="submit">ログイン</button>
                    </form>
                    <p class="hint">デモ用アカウント：管理者 admin001 / admin123　社員 emp001 / password123</p>
                </div>
                """.formatted(errorHtml);
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write(Layout.page("ログイン", null, body));
    }
}
