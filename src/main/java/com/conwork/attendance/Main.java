package com.conwork.attendance;

import com.conwork.attendance.dao.Database;
import com.conwork.attendance.filter.AuthFilter;
import com.conwork.attendance.servlet.AttendanceListServlet;
import com.conwork.attendance.servlet.ClockServlet;
import com.conwork.attendance.servlet.CsvExportServlet;
import com.conwork.attendance.servlet.DashboardServlet;
import com.conwork.attendance.servlet.LeaveApprovalServlet;
import com.conwork.attendance.servlet.LeaveRequestServlet;
import com.conwork.attendance.servlet.LoginServlet;
import com.conwork.attendance.servlet.LogoutServlet;
import com.conwork.attendance.servlet.PayrollCsvExportServlet;
import com.conwork.attendance.servlet.RootServlet;
import com.conwork.attendance.servlet.StaticServlet;
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
        context.addServlet(new ServletHolder(new PayrollCsvExportServlet()), "/export/payroll-csv");
        context.addServlet(new ServletHolder(new StaticServlet()), "/static/style.css");

        context.addFilter(new FilterHolder(new AuthFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));

        Server server = new Server(port);
        server.setHandler(context);
        server.start();

        System.out.println("=================================================");
        System.out.println(" ConWork 勤怠管理システム 起動しました");
        System.out.println(" URL: http://localhost:" + port + "/");
        System.out.println(" 管理者ログイン: admin001 / admin123");
        System.out.println(" 社員ログイン  : emp001   / password123");
        System.out.println("=================================================");

        server.join();
    }
}
