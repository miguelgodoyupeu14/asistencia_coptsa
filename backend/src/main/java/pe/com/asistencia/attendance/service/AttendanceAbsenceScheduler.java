package pe.com.asistencia.attendance.service;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttendanceAbsenceScheduler {
    private final AttendanceService attendance;
    private final ZoneId zone;

    public AttendanceAbsenceScheduler(AttendanceService attendance, @Value("${app.time-zone}") String zone) {
        this.attendance = attendance; this.zone = ZoneId.of(zone);
    }

    @Scheduled(cron = "${app.attendance.absence-job-cron:0 */15 * * * *}", zone = "${app.time-zone:America/Lima}")
    public void processClosedWorkdays() {
        LocalDate today = LocalDate.now(zone);
        for (int daysAgo = 0; daysAgo <= 7; daysAgo++) attendance.processAbsences(today.minusDays(daysAgo));
    }
}
