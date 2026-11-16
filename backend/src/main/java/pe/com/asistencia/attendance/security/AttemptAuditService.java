package pe.com.asistencia.attendance.security;

import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttemptAuditService {
    private final JdbcClient jdbc;
    public AttemptAuditService(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long employeeId, Long siteId, String punchType, boolean accepted, String reason,
                       String ip, String deviceHash, Double latitude, Double longitude,
                       Double accuracyMeters, String wifiBssid, String channel) {
        jdbc.sql("insert into punch_attempts(employee_id,site_id,punch_type,occurred_at,accepted,reason_code,ip_address,device_hash,latitude,longitude,accuracy_meters,wifi_bssid,channel,correlation_id) values(:employee,:site,:type,:at,:accepted,:reason,:ip,:device,:lat,:lon,:accuracy,:wifi,:channel,:correlation)")
            .param("employee", employeeId).param("at", Instant.now()).param("accepted", accepted)
            .param("site", siteId).param("type", punchType)
            .param("reason", reason).param("ip", ip).param("device", deviceHash)
            .param("lat", latitude).param("lon", longitude).param("accuracy", accuracyMeters).param("wifi", wifiBssid)
            .param("channel", channel).param("correlation", UUID.randomUUID().toString()).update();
    }

    public void record(Long employeeId, Long siteId, String punchType, boolean accepted, String reason,
                       String ip, String deviceHash, Double latitude, Double longitude,
                       String wifiBssid, String channel) {
        record(employeeId, siteId, punchType, accepted, reason, ip, deviceHash, latitude, longitude,
            null, wifiBssid, channel);
    }
}
