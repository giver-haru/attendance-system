package com.conwork.attendance.filter;

import com.conwork.attendance.model.Employee;
import com.conwork.attendance.model.Role;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/**
 * 未ログインならログイン画面へ、管理者専用パスに社員がアクセスしたら403にする単純なゲートキーパー。
 */
public class AuthFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS = Set.of("/login", "/", "/static/style.css");
    private static final Set<String> ADMIN_ONLY_PATHS = Set.of("/leave/approve", "/export/csv");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (PUBLIC_PATHS.contains(path)) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        Employee employee = session == null ? null : (Employee) session.getAttribute("employee");

        if (employee == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (ADMIN_ONLY_PATHS.contains(path) && employee.getRole() != Role.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "権限がありません");
            return;
        }

        chain.doFilter(req, res);
    }
}
