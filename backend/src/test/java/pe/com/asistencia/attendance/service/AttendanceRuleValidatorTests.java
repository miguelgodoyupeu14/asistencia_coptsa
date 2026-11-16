package pe.com.asistencia.attendance.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import pe.com.asistencia.attendance.config.AttendanceRulesProperties;
import pe.com.asistencia.attendance.domain.AttendanceDay;
import pe.com.asistencia.attendance.domain.PunchType;
import pe.com.asistencia.employee.domain.Employee;
import pe.com.asistencia.schedule.domain.Shift;

class AttendanceRuleValidatorTests {
    private final AttendanceRuleValidator validator = new AttendanceRuleValidator();
    private final ZoneId lima = ZoneId.of("America/Lima");
    private final LocalDate date = LocalDate.of(2026, 10, 12);
    private final Shift shift = new Shift(1, LocalTime.of(8, 0), LocalTime.of(17, 0), 60);
    private final AttendanceRulesProperties rules = new AttendanceRulesProperties(0, 0, 60, 120, 180, 100);

    @Test void acceptsEntryInsideConfiguredWindow() {
        assertThatCode(() -> validator.validate(PunchType.ENTRY, null, date, shift, at(7, 30), lima, rules))
            .doesNotThrowAnyException();
    }

    @Test void rejectsEntryOutsideConfiguredWindow() {
        assertViolation("CHECK_IN_WINDOW_CLOSED", () ->
            validator.validate(PunchType.ENTRY, null, date, shift, at(10, 1), lima, rules));
    }

    @Test void rejectsDuplicateEntry() {
        var day = dayWithEntry();
        assertViolation("DUPLICATE_ENTRY", () ->
            validator.validate(PunchType.ENTRY, day, date, shift, at(8, 30), lima, rules));
    }

    @Test void rejectsExitWithoutEntry() {
        assertViolation("EXIT_WITHOUT_ENTRY", () ->
            validator.validate(PunchType.EXIT, null, date, shift, at(16, 30), lima, rules));
    }

    @Test void acceptsEarlyExitAfterShiftStarted() {
        assertThatCode(() -> validator.validate(PunchType.EXIT, dayWithEntry(), date, shift, at(15, 0), lima, rules))
            .doesNotThrowAnyException();
    }

    @Test void rejectsExitAfterConfiguredWindow() {
        assertViolation("CHECK_OUT_WINDOW_CLOSED", () ->
            validator.validate(PunchType.EXIT, dayWithEntry(), date, shift, at(20, 1), lima, rules));
    }

    @Test void rejectsDuplicateExit() {
        var day = dayWithEntry();
        day.apply(PunchType.EXIT, at(17, 0), lima, 0, 0);
        assertViolation("DUPLICATE_EXIT", () ->
            validator.validate(PunchType.EXIT, day, date, shift, at(17, 30), lima, rules));
    }

    @Test void acceptsExitAfterMidnightForWorkDateWhereNightShiftStarted() {
        var nightShift = new Shift(1, LocalTime.of(22, 0), LocalTime.of(6, 0), 0);
        var employee = new Employee("NIGHT-1", "DNI", "70000010", "Turno", "Noche", null, null, null, date, null);
        var day = new AttendanceDay(employee, date, nightShift);
        day.apply(PunchType.ENTRY, at(22, 0), lima, 0, 0);
        var exit = date.plusDays(1).atTime(6, 0).atZone(lima).toInstant();

        assertThatCode(() -> validator.validate(PunchType.EXIT, day, date, nightShift, exit, lima, rules))
            .doesNotThrowAnyException();
    }

    @Test void rejectsNightShiftExitAfterItsConfiguredClosingWindow() {
        var nightShift = new Shift(1, LocalTime.of(22, 0), LocalTime.of(6, 0), 0);
        var employee = new Employee("NIGHT-2", "DNI", "70000011", "Turno", "Tarde", null, null, null, date, null);
        var day = new AttendanceDay(employee, date, nightShift);
        day.apply(PunchType.ENTRY, at(22, 0), lima, 0, 0);
        var exit = date.plusDays(1).atTime(9, 1).atZone(lima).toInstant();

        assertViolation("CHECK_OUT_WINDOW_CLOSED", () ->
            validator.validate(PunchType.EXIT, day, date, nightShift, exit, lima, rules));
    }

    @ParameterizedTest(name = "entrada {0} permitida={1}")
    @MethodSource("entryBoundaries")
    void validatesEveryEntryWindowBoundary(LocalTime time, boolean accepted, String code) {
        var action = (org.assertj.core.api.ThrowableAssert.ThrowingCallable) () ->
            validator.validate(PunchType.ENTRY, null, date, shift, at(time.getHour(), time.getMinute()), lima, rules);
        if (accepted) assertThatCode(action).doesNotThrowAnyException();
        else assertViolation(code, action);
    }

    @ParameterizedTest(name = "salida {0} permitida={1}")
    @MethodSource("exitBoundaries")
    void validatesEveryExitWindowBoundary(LocalTime time, boolean accepted, String code) {
        var action = (org.assertj.core.api.ThrowableAssert.ThrowingCallable) () ->
            validator.validate(PunchType.EXIT, dayWithEntry(), date, shift, at(time.getHour(), time.getMinute()), lima, rules);
        if (accepted) assertThatCode(action).doesNotThrowAnyException();
        else assertViolation(code, action);
    }

    private static Stream<Arguments> entryBoundaries() {
        return Stream.of(
            Arguments.of(LocalTime.of(6, 59), false, "CHECK_IN_NOT_OPEN"),
            Arguments.of(LocalTime.of(7, 0), true, null),
            Arguments.of(LocalTime.of(7, 59), true, null),
            Arguments.of(LocalTime.of(8, 0), true, null),
            Arguments.of(LocalTime.of(8, 1), true, null),
            Arguments.of(LocalTime.of(9, 59), true, null),
            Arguments.of(LocalTime.of(10, 0), true, null),
            Arguments.of(LocalTime.of(10, 1), false, "CHECK_IN_WINDOW_CLOSED"));
    }

    private static Stream<Arguments> exitBoundaries() {
        return Stream.of(
            Arguments.of(LocalTime.of(7, 59), false, "CHECK_OUT_NOT_OPEN"),
            Arguments.of(LocalTime.of(8, 0), true, null),
            Arguments.of(LocalTime.of(16, 59), true, null),
            Arguments.of(LocalTime.of(17, 0), true, null),
            Arguments.of(LocalTime.of(20, 0), true, null),
            Arguments.of(LocalTime.of(20, 1), false, "CHECK_OUT_WINDOW_CLOSED"));
    }

    private AttendanceDay dayWithEntry() {
        var employee = new Employee("RULE-1", "DNI", "70000009", "Regla", "Prueba", null, null, null, date, null);
        var day = new AttendanceDay(employee, date, shift);
        day.apply(PunchType.ENTRY, at(8, 0), lima, 0, 0);
        return day;
    }

    private Instant at(int hour, int minute) { return date.atTime(hour, minute).atZone(lima).toInstant(); }

    private void assertViolation(String code, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(AttendanceRuleValidator.Violation.class).hasMessage(code);
    }
}
