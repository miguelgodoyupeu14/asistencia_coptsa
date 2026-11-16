package pe.com.asistencia.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.asistencia.attendance.domain.Punch;
import pe.com.asistencia.attendance.domain.PunchType;

public interface PunchRepository extends JpaRepository<Punch,Long>{long countByAttendanceDayIdAndType(Long attendanceDayId,PunchType type);}
