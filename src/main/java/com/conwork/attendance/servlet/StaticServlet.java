package com.conwork.attendance.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * shaded jar実行時でもクラスパス上のリソースとして確実に配信できるよう、
 * Jetty標準のDefaultServlet(ファイルシステム前提)ではなく自前で配信する。
 */
public class StaticServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/css; charset=UTF-8");
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("static/style.css")) {
            if (in == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            in.transferTo(resp.getOutputStream());
        }
    }
}
