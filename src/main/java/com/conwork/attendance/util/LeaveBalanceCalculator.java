package com.conwork.attendance.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 有給休暇の残日数を計算する。
 * 個々の入社日に応じた付与ではなく、毎年4/1に全社員一律で付与する「基準日方式」を採用している。
 * 繰越は行わず、当年度（直近4/1〜翌3/31）に付与された分のみを管理する。
 */
public final class LeaveBalanceCalculator {

    // 労働基準法の法定付与日数（勤続6ヶ月ごとの区分）に基づく
    private static final int[] GRANT_DAYS_BY_HALF_YEAR_STEP = {0, 10, 11, 12, 14, 16, 18, 20};

    private LeaveBalanceCalculator() {}

    /** 基準となる年度の開始日（直近の4/1）を返す。 */
    public static LocalDate currentFiscalYearStart(LocalDate asOf) {
        int year = asOf.getMonthValue() >= 4 ? asOf.getYear() : asOf.getYear() - 1;
        return LocalDate.of(year, 4, 1);
    }

    /** 指定した基準日時点での、入社日に応じた法定付与日数を返す。 */
    public static int grantedDays(LocalDate hireDate, LocalDate fiscalYearStart) {
        if (hireDate.isAfter(fiscalYearStart)) return 0;
        long serviceMonths = ChronoUnit.MONTHS.between(hireDate, fiscalYearStart);
        int step = (int) Math.min(serviceMonths / 6, GRANT_DAYS_BY_HALF_YEAR_STEP.length - 1);
        return GRANT_DAYS_BY_HALF_YEAR_STEP[step];
    }
}
