package com.conwork.attendance.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

public class AttendanceRecord {

    private static final long STANDARD_WORK_MINUTES = 8 * 60;

    private final int id;
    private final int employeeId;
    private final LocalDate workDate;
    private final LocalTime clockIn;
    private final LocalTime clockOut;

    public AttendanceRecord(int id, int employeeId, LocalDate workDate, LocalTime clockIn, LocalTime clockOut) {
        this.id = id;
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.clockIn = clockIn;
        this.clockOut = clockOut;
    }

    public int getId() { return id; }
    public int getEmployeeId() { return employeeId; }
    public LocalDate getWorkDate() { return workDate; }
    public LocalTime getClockIn() { return clockIn; }
    public LocalTime getClockOut() { return clockOut; }

    public boolean isClockedOut() {
        return clockOut != null;
    }

    public long getWorkedMinutes() {
        if (clockIn == null || clockOut == null) return 0;
        return Duration.between(clockIn, clockOut).toMinutes();
    }

    public long getOvertimeMinutes() {
        return Math.max(0, getWorkedMinutes() - STANDARD_WORK_MINUTES);
    }
}
