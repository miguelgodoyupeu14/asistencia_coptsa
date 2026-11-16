package pe.com.asistencia.attendance.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="app.attendance")
public record AttendanceRulesProperties(int entryToleranceMinutes,int exitToleranceMinutes,int entryOpenBeforeMinutes,int entryCloseAfterMinutes,int exitCloseAfterMinutes,int maxLocationAccuracyMeters){public AttendanceRulesProperties{if(entryToleranceMinutes<0||exitToleranceMinutes<0||entryOpenBeforeMinutes<0||entryCloseAfterMinutes<0||exitCloseAfterMinutes<0)throw new IllegalArgumentException("Las reglas de asistencia no pueden ser negativas");if(maxLocationAccuracyMeters<=0)throw new IllegalArgumentException("La precisión máxima de ubicación debe ser positiva");}}
