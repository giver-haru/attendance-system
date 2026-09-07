package com.conwork.attendance.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaveBalanceCalculatorTest {

    @Test
    void currentFiscalYearStart_returnsApril1stOfSameYear_whenMonthIsAprilOrLater() {
        assertEquals(LocalDate.of(2026, 4, 1),
                LeaveBalanceCalculator.currentFiscalYearStart(LocalDate.of(2026, 9, 7)));
        assertEquals(LocalDate.of(2026, 4, 1),
                LeaveBalanceCalculator.currentFiscalYearStart(LocalDate.of(2026, 4, 1)));
    }

    @Test
    void currentFiscalYearStart_returnsApril1stOfPreviousYear_whenMonthIsBeforeApril() {
        assertEquals(LocalDate.of(2025, 4, 1),
                LeaveBalanceCalculator.currentFiscalYearStart(LocalDate.of(2026, 2, 1)));
        assertEquals(LocalDate.of(2025, 4, 1),
                LeaveBalanceCalculator.currentFiscalYearStart(LocalDate.of(2026, 3, 31)));
    }

    @Test
    void grantedDays_isZero_whenHiredAfterTheGrantDate() {
        LocalDate fiscalYearStart = LocalDate.of(2026, 4, 1);
        assertEquals(0, LeaveBalanceCalculator.grantedDays(LocalDate.of(2026, 5, 1), fiscalYearStart));
    }

    @Test
    void grantedDays_isZero_whenServiceIsUnderSixMonths() {
        LocalDate fiscalYearStart = LocalDate.of(2026, 4, 1);
        assertEquals(0, LeaveBalanceCalculator.grantedDays(LocalDate.of(2025, 11, 1), fiscalYearStart));
    }

    /**
     * 労働基準法の法定付与日数テーブル:
     * 6ヶ月=10日, 1年6ヶ月=11日, 2年6ヶ月=12日, 3年6ヶ月=14日,
     * 4年6ヶ月=16日, 5年6ヶ月=18日, 6年6ヶ月以上=20日（上限）。
     * 最初の付与だけ6ヶ月区切り、以降は12ヶ月ごとに区分が上がる。
     */
    @ParameterizedTest
    @CsvSource({
            "6, 10",
            "17, 10",
            "18, 11",
            "29, 11",
            "30, 12",
            "41, 12",
            "42, 14",
            "53, 14",
            "54, 16",
            "65, 16",
            "66, 18",
            "77, 18",
            "78, 20",
            "120, 20"
    })
    void grantedDays_followsStatutoryGrantTable(int serviceMonths, int expectedDays) {
        LocalDate fiscalYearStart = LocalDate.of(2026, 4, 1);
        LocalDate hireDate = fiscalYearStart.minusMonths(serviceMonths);
        assertEquals(expectedDays, LeaveBalanceCalculator.grantedDays(hireDate, fiscalYearStart));
    }
}
