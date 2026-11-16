package pe.com.asistencia.attendance.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import pe.com.asistencia.schedule.domain.Shift;
import pe.com.asistencia.schedule.repository.ScheduleAssignmentRepository;

@Service
public class AttendanceCalendarService {
    private final JdbcClient jdbc;
    private final ScheduleAssignmentRepository schedules;

    public AttendanceCalendarService(JdbcClient jdbc, ScheduleAssignmentRepository schedules) {
        this.jdbc = jdbc;
        this.schedules = schedules;
    }

    public Plan resolve(Long employeeId, Long siteId, LocalDate date) {
        CalendarException exception = exception(siteId, date);
        if (exception != null && ("HOLIDAY".equals(exception.type) || "NON_WORKING_DAY".equals(exception.type))) {
            throw new RuleViolation("NON_WORKING_DAY");
        }
        var applicable = schedules.findApplicable(employeeId, date);
        if (applicable.isEmpty()) throw new RuleViolation("NO_ACTIVE_SCHEDULE");
        if (applicable.size() > 1) throw new RuleViolation("MULTIPLE_ACTIVE_SCHEDULES");
        Shift regular = applicable.getFirst().getSchedule().getShifts().stream()
            .filter(value -> value.getDayOfWeek() == date.getDayOfWeek().getValue()).findFirst().orElse(null);
        if (exception != null && ("SPECIAL_SCHEDULE".equals(exception.type) || "SPECIAL_WORKING_DAY".equals(exception.type))
            && exception.start != null && exception.end != null) {
            return new Plan(null, exception.start, exception.end, !exception.end.isAfter(exception.start), 0, exception.type);
        }
        if (regular == null) throw new RuleViolation("NON_WORKING_DAY");
        return new Plan(regular, regular.getStartTime(), regular.getEndTime(), regular.isCrossesMidnight(),
            regular.getBreakMinutes(), exception == null ? "REGULAR" : exception.type);
    }

    public boolean isNonWorking(Long siteId, LocalDate date) {
        CalendarException value = exception(siteId, date);
        return value != null && ("HOLIDAY".equals(value.type) || "NON_WORKING_DAY".equals(value.type));
    }

    private CalendarException exception(Long siteId, LocalDate date) {
        List<CalendarException> values = jdbc.sql("select day_type,start_time,end_time from attendance_calendar where site_id=:site and calendar_date=:date and active=true")
            .param("site", siteId).param("date", date)
            .query((rs, row) -> new CalendarException(rs.getString("day_type"), rs.getObject("start_time", LocalTime.class),
                rs.getObject("end_time", LocalTime.class))).list();
        if (values.size() > 1) throw new RuleViolation("MULTIPLE_CALENDAR_EXCEPTIONS");
        return values.isEmpty() ? null : values.getFirst();
    }

    private record CalendarException(String type, LocalTime start, LocalTime end) {}
    public record Plan(Shift shift, LocalTime startTime, LocalTime endTime, boolean crossesMidnight,
                       int breakMinutes, String calendarType) {}
    public static class RuleViolation extends RuntimeException {
        private final String code;
        public RuleViolation(String code) { super(code); this.code = code; }
        public String code() { return code; }
    }
}
