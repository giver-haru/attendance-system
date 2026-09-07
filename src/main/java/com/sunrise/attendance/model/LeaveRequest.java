package com.sunrise.attendance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class LeaveRequest {

    private final int id;
    private final int employeeId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String reason;
    private final LeaveStatus status;
    private final Integer approverId;
    private final LocalDateTime requestedAt;

    public LeaveRequest(int id, int employeeId, LocalDate startDate, LocalDate endDate,
                         String reason, LeaveStatus status, Integer approverId, LocalDateTime requestedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
        this.approverId = approverId;
        this.requestedAt = requestedAt;
    }

    public int getId() { return id; }
    public int getEmployeeId() { return employeeId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getReason() { return reason; }
    public LeaveStatus getStatus() { return status; }
    public Integer getApproverId() { return approverId; }
    public LocalDateTime getRequestedAt() { return requestedAt; }

    public long getDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}
