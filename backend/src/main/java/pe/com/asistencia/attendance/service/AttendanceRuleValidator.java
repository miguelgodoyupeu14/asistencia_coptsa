package pe.com.asistencia.attendance.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import pe.com.asistencia.attendance.config.AttendanceRulesProperties;
import pe.com.asistencia.attendance.domain.AttendanceDay;
import pe.com.asistencia.attendance.domain.PunchType;
import pe.com.asistencia.schedule.domain.Shift;

public class AttendanceRuleValidator {
    public void validate(PunchType type, AttendanceDay existingDay, LocalDate workDate, Shift shift,
                         Instant occurredAt, ZoneId zone, AttendanceRulesProperties rules) {
        validate(type, existingDay, workDate, shift.getStartTime(), shift.getEndTime(), shift.isCrossesMidnight(),
            occurredAt, zone, rules);
    }

    public void validate(PunchType type, AttendanceDay existingDay, LocalDate workDate,
                         java.time.LocalTime startTime, java.time.LocalTime endTime, boolean crossesMidnight,
                         Instant occurredAt, ZoneId zone, AttendanceRulesProperties rules) {
        var start = workDate.atTime(startTime).atZone(zone).toInstant();
        var endDate = crossesMidnight ? workDate.plusDays(1) : workDate;
        var end = endDate.atTime(endTime).atZone(zone).toInstant();

        if (type == PunchType.ENTRY) {
            if (existingDay != null && existingDay.getFirstEntryAt() != null) fail("DUPLICATE_ENTRY");
            var opensAt = start.minusSeconds(rules.entryOpenBeforeMinutes() * 60L);
            var closesAt = start.plusSeconds(rules.entryCloseAfterMinutes() * 60L);
            if (occurredAt.isBefore(opensAt)) fail("CHECK_IN_NOT_OPEN");
            if (occurredAt.isAfter(closesAt)) fail("CHECK_IN_WINDOW_CLOSED");
            return;
        }

        if (existingDay == null || existingDay.getFirstEntryAt() == null) fail("EXIT_WITHOUT_ENTRY");
        if (existingDay.getLastExitAt() != null) fail("DUPLICATE_EXIT");
        var closesAt = end.plusSeconds(rules.exitCloseAfterMinutes() * 60L);
        if (occurredAt.isBefore(start)) fail("CHECK_OUT_NOT_OPEN");
        if (occurredAt.isAfter(closesAt)) fail("CHECK_OUT_WINDOW_CLOSED");
        if (occurredAt.isBefore(existingDay.getFirstEntryAt())) fail("EXIT_BEFORE_ENTRY");
    }

    private static void fail(String code) { throw new Violation(code); }

    public static class Violation extends RuntimeException {
        private final String code;
        public Violation(String code) { super(code); this.code = code; }
        public String code() { return code; }
    }
}
