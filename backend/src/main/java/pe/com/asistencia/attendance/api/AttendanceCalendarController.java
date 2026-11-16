package pe.com.asistencia.attendance.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.com.asistencia.security.repository.UserRepository;

@RestController
@RequestMapping("/api/v1/attendance-calendar")
@PreAuthorize("hasRole('ADMIN')")
public class AttendanceCalendarController {
    private final JdbcClient jdbc;
    private final UserRepository users;

    public AttendanceCalendarController(JdbcClient jdbc, UserRepository users) { this.jdbc = jdbc; this.users = users; }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam Long siteId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        if (to.isBefore(from)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
        return jdbc.sql("select id,site_id,calendar_date,day_type,start_time,end_time,description,active,created_by,created_at from attendance_calendar where site_id=:site and calendar_date between :from and :to order by calendar_date")
            .param("site", siteId).param("from", from).param("to", to).query().listOfRows();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(Authentication authentication, @Valid @RequestBody CalendarRequest request) {
        boolean requiresTimes = "SPECIAL_SCHEDULE".equals(request.dayType());
        if (requiresTimes && (request.startTime() == null || request.endTime() == null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SPECIAL_SCHEDULE_REQUIRES_TIMES");
        if (("HOLIDAY".equals(request.dayType()) || "NON_WORKING_DAY".equals(request.dayType()))
            && (request.startTime() != null || request.endTime() != null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NON_WORKING_DAY_MUST_NOT_HAVE_TIMES");
        Long actor = users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow().getId();
        int existing = jdbc.sql("select count(*) from attendance_calendar where site_id=:site and calendar_date=:date")
            .param("site", request.siteId()).param("date", request.date()).query(Integer.class).single();
        if (existing > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "CALENDAR_EXCEPTION_ALREADY_EXISTS");
        jdbc.sql("insert into attendance_calendar(site_id,calendar_date,day_type,start_time,end_time,description,active,created_by) values(:site,:date,:type,:start,:end,:description,true,:actor)")
            .param("site", request.siteId()).param("date", request.date()).param("type", request.dayType())
            .param("start", request.startTime()).param("end", request.endTime()).param("description", request.description())
            .param("actor", actor).update();
        return Map.of("status", "CREATED");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        if (jdbc.sql("update attendance_calendar set active=false where id=:id and active=true").param("id", id).update() == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CALENDAR_EXCEPTION_NOT_FOUND");
    }

    public record CalendarRequest(@NotNull Long siteId, @NotNull LocalDate date,
        @NotBlank @Pattern(regexp = "HOLIDAY|NON_WORKING_DAY|SPECIAL_WORKING_DAY|SPECIAL_SCHEDULE") String dayType,
        LocalTime startTime, LocalTime endTime, @Size(max = 255) String description) {}
}
