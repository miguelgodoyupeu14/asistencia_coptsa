package pe.com.asistencia.attendance.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AttendanceSecurityServiceTests {
    @Test void acceptsEquivalentWifiBssidFormats() {
        assertThat(AttendanceSecurityService.wifiMatches("aa:bb:cc:dd:ee:ff", "AA-BB-CC-DD-EE-FF")).isTrue();
    }

    @Test void rejectsMissingOrDifferentWifiBssid() {
        assertThat(AttendanceSecurityService.wifiMatches("AA:BB:CC:DD:EE:FF", null)).isFalse();
        assertThat(AttendanceSecurityService.wifiMatches("AA:BB:CC:DD:EE:FF", "11:22:33:44:55:66")).isFalse();
    }

    @Test void calculatesGeofenceDistanceInMeters() {
        assertThat(AttendanceSecurityService.distance(-12.046374, -77.042793, -12.046374, -77.042793)).isZero();
        assertThat(AttendanceSecurityService.distance(-12.046374, -77.042793, -12.047274, -77.042793)).isBetween(99.0, 101.5);
    }

    @ParameterizedTest(name = "distancia aproximada {0} m dentro={1}")
    @CsvSource({"0,true", "50,true", "99,true", "100,true", "101,false"})
    void definesGeofenceBoundaryAsInclusive(double targetMeters, boolean expectedInside) {
        double latitude = -12.046374;
        double longitude = -77.042793;
        double latitudeOffset = targetMeters / 111_195d;
        double measured = AttendanceSecurityService.distance(latitude, longitude, latitude + latitudeOffset, longitude);
        assertThat(measured <= 100d).isEqualTo(expectedInside);
    }
}
