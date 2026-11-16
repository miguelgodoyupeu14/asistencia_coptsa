package pe.com.asistencia.attendance.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.com.asistencia.attendance.domain.PunchType;
import pe.com.asistencia.attendance.config.AttendanceRulesProperties;

@Service
public class AttendanceSecurityService {
    private final JdbcClient jdbc;
    private final AttemptAuditService audit;
    private final ZoneId zone;
    private final AttendanceRulesProperties rules;
    public AttendanceSecurityService(JdbcClient jdbc, AttemptAuditService audit, @Value("${app.time-zone}") String zone,
                                     AttendanceRulesProperties rules) { this.jdbc = jdbc; this.audit = audit; this.zone = ZoneId.of(zone); this.rules = rules; }

    public Validation validate(Long employeeId, LocalDate workDate, PunchType punchType, String ip, String deviceId, Double latitude, Double longitude,
                               Double accuracyMeters, String qrCode, String wifiBssid, String channel) {
        String deviceHash = deviceId == null ? null : hash(deviceId);
        var sites = jdbc.sql("select s.id,s.latitude,s.longitude,s.geofence_radius_m,s.require_geofence,s.require_registered_device,s.require_dynamic_qr,s.allowed_wifi_bssid,s.require_wifi from employee_assignments ea join sites s on s.id=ea.site_id where ea.employee_id=:employee and ea.active=true and s.active=true and ea.valid_from<=:today and (ea.valid_to is null or ea.valid_to>=:today) and ea.assignment_type=(case when exists(select 1 from employee_assignments t where t.employee_id=ea.employee_id and t.assignment_type='TEMPORARY' and t.active=true and t.valid_from<=:today and (t.valid_to is null or t.valid_to>=:today)) then 'TEMPORARY' else 'PERMANENT' end) order by ea.valid_from desc")
            .param("employee", employeeId).param("today", workDate)
            .query((rs, n) -> new SitePolicy(rs.getLong("id"), (Double) rs.getObject("latitude", Double.class),
                (Double) rs.getObject("longitude", Double.class), (Integer) rs.getObject("geofence_radius_m", Integer.class),
                rs.getBoolean("require_geofence"), rs.getBoolean("require_registered_device"),
                rs.getBoolean("require_dynamic_qr"), rs.getString("allowed_wifi_bssid"), rs.getBoolean("require_wifi"))).list();
        if (sites.isEmpty()) reject(employeeId, null, punchType, "NO_ACTIVE_SITE_ASSIGNMENT", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if (sites.size() > 1) reject(employeeId, null, punchType, "SITE_ASSIGNMENT_OVERLAP", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        var policy = sites.getFirst();
        boolean requiresDevice = policy.requireDevice && !"WEB".equals(channel);
        if (requiresDevice && (deviceHash == null || deviceId.isBlank()))
            reject(employeeId, policy.id, punchType, "DEVICE_REQUIRED", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if (requiresDevice) validateDevice(employeeId, policy.id, punchType, deviceHash, ip, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if (policy.requireGeofence && (latitude == null || longitude == null))
            reject(employeeId, policy.id, punchType, "GPS_REQUIRED", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if (policy.requireGeofence && accuracyMeters == null)
            reject(employeeId, policy.id, punchType, "LOCATION_ACCURACY_REQUIRED", ip, deviceHash, latitude, longitude, null, wifiBssid, channel);
        if (policy.requireGeofence && accuracyMeters > rules.maxLocationAccuracyMeters())
            reject(employeeId, policy.id, punchType, "LOCATION_LOW_ACCURACY", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if (policy.requireGeofence && (policy.latitude == null || policy.longitude == null || policy.radius == null))
            reject(employeeId, policy.id, punchType, "GEOFENCE_NOT_CONFIGURED", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if (policy.requireGeofence && distance(latitude, longitude, policy.latitude, policy.longitude) > policy.radius)
            reject(employeeId, policy.id, punchType, "OUTSIDE_GEOFENCE", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if (policy.requireQr) validateQr(employeeId, policy.id, punchType, qrCode, ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if (policy.requireWifi && "WEB".equals(channel))
            reject(employeeId, policy.id, punchType, "WIFI_UNAVAILABLE_ON_WEB", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if (policy.requireWifi && (wifiBssid == null || wifiBssid.isBlank()))
            reject(employeeId, policy.id, punchType, "WIFI_REQUIRED", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if (policy.requireWifi && !wifiMatches(policy.allowedWifiBssid, wifiBssid))
            reject(employeeId, policy.id, punchType, "INVALID_WIFI", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        return new Validation(policy.id, deviceHash, "VALIDATED");
    }

    public Validation validate(Long employeeId, LocalDate workDate, PunchType punchType, String ip, String deviceId, Double latitude, Double longitude,
                               String qrCode, String wifiBssid, String channel) {
        return validate(employeeId, workDate, punchType, ip, deviceId, latitude, longitude, 0.0, qrCode, wifiBssid, channel);
    }

    private void validateDevice(Long employeeId, Long siteId, PunchType punchType, String deviceHash, String ip,
                                Double latitude, Double longitude, Double accuracyMeters, String wifiBssid, String channel) {
        var devices = jdbc.sql("select employee_id,status from registered_devices where identifier_hash=:hash")
            .param("hash", deviceHash).query((rs, n) -> new DeviceState(rs.getLong("employee_id"), rs.getString("status"))).list();
        var own = devices.stream().filter(device -> device.employeeId.equals(employeeId)).findFirst();
        if (own.isEmpty()) {
            String reason = devices.isEmpty() ? "DEVICE_NOT_REGISTERED" : "DEVICE_OWNERSHIP_MISMATCH";
            reject(employeeId, siteId, punchType, reason, ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        }
        String status = own.orElseThrow().status;
        if ("PENDING".equals(status)) reject(employeeId, siteId, punchType, "DEVICE_PENDING", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if ("REJECTED".equals(status)) reject(employeeId, siteId, punchType, "DEVICE_REJECTED", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if ("REVOKED".equals(status)) reject(employeeId, siteId, punchType, "DEVICE_REVOKED", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if (!"APPROVED".equals(status)) reject(employeeId, siteId, punchType, "DEVICE_NOT_APPROVED", ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
    }

    private void validateQr(Long employeeId,Long siteId,PunchType punchType,String qrCode,String ip,String deviceHash,Double latitude,Double longitude,Double accuracyMeters,String wifiBssid,String channel){
        if(qrCode==null||qrCode.isBlank()){
            int active=jdbc.sql("select count(*) from dynamic_qr_challenges where site_id=:site and active=true and valid_from<=current_timestamp and expires_at>=current_timestamp").param("site",siteId).query(Integer.class).single();
            reject(employeeId,siteId,punchType,active==0?"NO_ACTIVE_QR":"QR_REQUIRED",ip,deviceHash,latitude,longitude,accuracyMeters,wifiBssid,channel);
        }
        var found=jdbc.sql("select site_id,active,valid_from,expires_at from dynamic_qr_challenges where code_hash=:hash")
            .param("hash",hash(qrCode)).query((rs,n)->new QrState(rs.getLong("site_id"),rs.getBoolean("active"),rs.getTimestamp("valid_from").toInstant(),rs.getTimestamp("expires_at").toInstant())).optional();
        if(found.isEmpty())reject(employeeId,siteId,punchType,"QR_INVALID",ip,deviceHash,latitude,longitude,accuracyMeters,wifiBssid,channel);
        var qr=found.orElseThrow();var now=java.time.Instant.now();
        if(!qr.siteId.equals(siteId))reject(employeeId,siteId,punchType,"QR_SITE_MISMATCH",ip,deviceHash,latitude,longitude,accuracyMeters,wifiBssid,channel);
        if(!qr.active)reject(employeeId,siteId,punchType,"QR_INACTIVE",ip,deviceHash,latitude,longitude,accuracyMeters,wifiBssid,channel);
        if(now.isBefore(qr.validFrom)||now.isAfter(qr.expiresAt))reject(employeeId,siteId,punchType,"QR_EXPIRED",ip,deviceHash,latitude,longitude,accuracyMeters,wifiBssid,channel);
    }

    public Validation validate(Long employeeId, String ip, String deviceId, Double latitude, Double longitude,
                               String qrCode, String wifiBssid, String channel) {
        return validate(employeeId, LocalDate.now(zone), null, ip, deviceId, latitude, longitude, 0.0, qrCode, wifiBssid, channel);
    }

    public void accepted(Long employeeId, Validation validation, PunchType punchType, String ip, Double latitude, Double longitude,
                         Double accuracyMeters, String wifiBssid, String channel) {
        audit.record(employeeId, validation.siteId, punchType.name(), true, validation.reason, ip, validation.deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        if (validation.deviceHash != null) jdbc.sql("update registered_devices set last_used_at=current_timestamp where employee_id=:employee and identifier_hash=:hash")
            .param("employee", employeeId).param("hash", validation.deviceHash).update();
    }

    public void accepted(Long employeeId, Validation validation, PunchType punchType, String ip, Double latitude, Double longitude,
                         String wifiBssid, String channel) {
        accepted(employeeId, validation, punchType, ip, latitude, longitude, null, wifiBssid, channel);
    }

    public void denied(Long employeeId, Long siteId, PunchType punchType, String reason, String ip, String deviceId, Double latitude,
                       Double longitude, Double accuracyMeters, String wifiBssid, String channel) {
        reject(employeeId, siteId, punchType, reason, ip, deviceId == null ? null : hash(deviceId), latitude, longitude, accuracyMeters, wifiBssid, channel);
    }

    public void denied(Long employeeId, Long siteId, PunchType punchType, String reason, String ip, String deviceId, Double latitude,
                       Double longitude, String wifiBssid, String channel) {
        denied(employeeId, siteId, punchType, reason, ip, deviceId, latitude, longitude, null, wifiBssid, channel);
    }

    private void reject(Long employeeId, Long siteId, PunchType punchType, String reason, String ip, String deviceHash, Double latitude,
                        Double longitude, Double accuracyMeters, String wifiBssid, String channel) {
        audit.record(employeeId, siteId, punchType == null ? null : punchType.name(), false, reason, ip, deviceHash, latitude, longitude, accuracyMeters, wifiBssid, channel);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
    }

    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    static boolean wifiMatches(String expected, String actual) { return expected != null && actual != null && normalizeBssid(expected).equals(normalizeBssid(actual)); }
    private static String normalizeBssid(String value) { return value.trim().replace('-', ':').toUpperCase(); }
    static double distance(double lat1, double lon1, double lat2, double lon2) {
        double earth = 6371000, dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earth * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
    public record Validation(Long siteId, String deviceHash, String reason) {}
    private record DeviceState(Long employeeId,String status) {}
    private record SitePolicy(Long id, Double latitude, Double longitude, Integer radius, boolean requireGeofence,
                              boolean requireDevice, boolean requireQr, String allowedWifiBssid, boolean requireWifi) {}
    private record QrState(Long siteId,boolean active,java.time.Instant validFrom,java.time.Instant expiresAt){}
}
