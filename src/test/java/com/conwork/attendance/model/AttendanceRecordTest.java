package com.conwork.attendance.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceRecordTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 7);

    @Test
    void getWorkedMinutes_isZero_whenNotClockedOut() {
        AttendanceRecord record = new AttendanceRecord(1, 1, DATE, LocalTime.of(9, 0), null);
        assertEquals(0, record.getWorkedMinutes());
    }

    @Test
    void getWorkedMinutes_computesDurationBetweenClockInAndOut() {
        AttendanceRecord record = new AttendanceRecord(1, 1, DATE, LocalTime.of(9, 0), LocalTime.of(18, 30));
        assertEquals(570, record.getWorkedMinutes());
    }

    @Test
    void getOvertimeMinutes_isZero_whenWorkedExactlyStandardHours() {
        AttendanceRecord record = new AttendanceRecord(1, 1, DATE, LocalTime.of(9, 0), LocalTime.of(17, 0));
        assertEquals(0, record.getOvertimeMinutes());
    }

    @Test
    void getOvertimeMinutes_isZero_whenWorkedLessThanStandardHours() {
        AttendanceRecord record = new AttendanceRecord(1, 1, DATE, LocalTime.of(9, 0), LocalTime.of(15, 0));
        assertEquals(0, record.getOvertimeMinutes());
    }

    @Test
    void getOvertimeMinutes_returnsMinutesBeyondStandardEightHours() {
        AttendanceRecord record = new AttendanceRecord(1, 1, DATE, LocalTime.of(9, 0), LocalTime.of(19, 0));
        assertEquals(120, record.getOvertimeMinutes());
    }

    @Test
    void isClockedOut_reflectsWhetherClockOutIsPresent() {
        AttendanceRecord inProgress = new AttendanceRecord(1, 1, DATE, LocalTime.of(9, 0), null);
        AttendanceRecord finished = new AttendanceRecord(1, 1, DATE, LocalTime.of(9, 0), LocalTime.of(18, 0));

        assertFalse(inProgress.isClockedOut());
        assertTrue(finished.isClockedOut());
    }
}
