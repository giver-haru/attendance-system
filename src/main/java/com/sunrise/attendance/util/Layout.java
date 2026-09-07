package com.sunrise.attendance.util;

import com.sunrise.attendance.model.Employee;
import com.sunrise.attendance.model.Role;

public final class Layout {

    private Layout() {}

    public static String page(String title, Employee current, String bodyHtml) {
        String nav = current == null ? "" : navFor(current);
        return """
                <!DOCTYPE html>
                <html lang="ja">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>%s | Sunrise 勤怠管理システム</title>
                    <link rel="stylesheet" href="/static/style.css">
                </head>
                <body>
                    %s
                    <main class="container">
                        %s
                    </main>
                </body>
                </html>
                """.formatted(escape(title), nav, bodyHtml);
    }

    private static String navFor(Employee current) {
        String adminLink = current.getRole() == Role.ADMIN
                ? "<a href=\"/leave/approve\">承認待ち一覧</a>"
                : "";
        return """
                <header class="topbar">
                    <div class="brand">Sunrise 勤怠管理システム</div>
                    <nav>
                        <a href="/dashboard">ダッシュボード</a>
                        <a href="/attendance">勤怠一覧</a>
                        %s
                        <span class="user">%s さん（%s）</span>
                        <a href="/logout" class="logout">ログアウト</a>
                    </nav>
                </header>
                """.formatted(adminLink, escape(current.getName()),
                current.getRole() == Role.ADMIN ? "管理者" : "社員");
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
