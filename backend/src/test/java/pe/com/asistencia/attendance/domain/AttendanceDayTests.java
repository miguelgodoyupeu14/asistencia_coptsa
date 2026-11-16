package pe.com.asistencia.attendance.domain;

import org.junit.jupiter.api.Test;
import pe.com.asistencia.employee.domain.Employee;
import pe.com.asistencia.schedule.domain.Shift;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;

class AttendanceDayTests {
    @Test
    void calculatesWorkedMinutesFromFirstEntryToExit() {
        var employee = new Employee("E001", "DNI", "12345678", "Ana", "Pérez", null, null, null, LocalDate.of(2026, 1, 1), null);
        var day = new AttendanceDay(employee, LocalDate.of(2026, 9, 18), null);
        Instant entry = Instant.parse("2026-09-18T13:00:00Z");
        day.apply(PunchType.ENTRY, entry);
        day.apply(PunchType.EXIT, entry.plus(Duration.ofHours(8).plusMinutes(15)));
        assertThat(day.getWorkedMinutes()).isEqualTo(495);
        assertThat(day.getStatus()).isEqualTo(AttendanceStatus.COMPLETED);
    }

    @Test
    void exitWithoutEntryIsIncomplete() {
        var employee = new Employee("E002", "DNI", "87654321", "Luis", "Ramos", null, null, null, LocalDate.of(2026, 1, 1), null);
        var day = new AttendanceDay(employee, LocalDate.of(2026, 9, 18), null);
        day.apply(PunchType.EXIT, Instant.parse("2026-09-18T22:00:00Z"));
        assertThat(day.getStatus()).isEqualTo(AttendanceStatus.INCOMPLETE);
        assertThat(day.getWorkedMinutes()).isZero();
    }

    @Test
    void calculatesLateAndEarlyExitUsingConfiguredTolerance() {
        var employee = new Employee("E003", "DNI", "11223344", "Marta", "Silva", null, null, null, LocalDate.of(2026, 1, 1), null);
        var shift = new Shift(5, LocalTime.of(8, 0), LocalTime.of(17, 0), 60);
        var day = new AttendanceDay(employee, LocalDate.of(2026, 9, 18), shift);
        ZoneId lima = ZoneId.of("America/Lima");
        day.apply(PunchType.ENTRY, ZonedDateTime.of(2026, 9, 18, 8, 12, 0, 0, lima).toInstant(), lima, 5, 0);
        day.apply(PunchType.EXIT, ZonedDateTime.of(2026, 9, 18, 16, 45, 0, 0, lima).toInstant(), lima, 5, 0);
        assertThat(day.getLateMinutes()).isEqualTo(7);
        assertThat(day.getEarlyExitMinutes()).isEqualTo(15);
        assertThat(day.getWorkedMinutes()).isEqualTo(453);
    }
}
