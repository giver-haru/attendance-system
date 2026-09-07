package com.sunrise.attendance;

import com.sunrise.attendance.dao.Database;
import com.sunrise.attendance.filter.AuthFilter;
import com.sunrise.attendance.servlet.AttendanceListServlet;
import com.sunrise.attendance.servlet.ClockServlet;
import com.sunrise.attendance.servlet.CsvExportServlet;
import com.sunrise.attendance.servlet.DashboardServlet;
import com.sunrise.attendance.servlet.LeaveApprovalServlet;
import com.sunrise.attendance.servlet.LeaveRequestServlet;
import com.sunrise.attendance.servlet.LoginServlet;
import com.sunrise.attendance.servlet.LogoutServlet;
import com.sunrise.attendance.servlet.RootServlet;
import com.sunrise.attendance.servlet.StaticServlet;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.EnumSet;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        Database.initialize();

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        context.addServlet(new ServletHolder(new RootServlet()), "/");
        context.addServlet(new ServletHolder(new LoginServlet()), "/login");
        context.addServlet(new ServletHolder(new LogoutServlet()), "/logout");
        context.addServlet(new ServletHolder(new DashboardServlet()), "/dashboard");
        context.addServlet(new ServletHolder(new ClockServlet()), "/clock");
        context.addServlet(new ServletHolder(new LeaveRequestServlet()), "/leave/request");
        context.addServlet(new ServletHolder(new LeaveApprovalServlet()), "/leave/approve");
        context.addServlet(new ServletHolder(new AttendanceListServlet()), "/attendance");
        context.addServlet(new ServletHolder(new CsvExportServlet()), "/export/csv");
        context.addServlet(new ServletHolder(new StaticServlet()), "/static/style.css");

        context.addFilter(new FilterHolder(new AuthFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));

        Server server = new Server(port);
        server.setHandler(context);
        server.start();

        System.out.println("=================================================");
        System.out.println(" Sunrise Industries 勤怠管理システム 起動しました");
        System.out.println(" URL: http://localhost:" + port + "/");
        System.out.println(" 管理者ログイン: admin001 / admin123");
        System.out.println(" 社員ログイン  : emp001   / password123");
        System.out.println("=================================================");

        server.join();
    }
}
