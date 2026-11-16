package pe.com.asistencia.schedule.repository;
import org.springframework.data.jpa.repository.JpaRepository; import pe.com.asistencia.schedule.domain.Schedule;
public interface ScheduleRepository extends JpaRepository<Schedule,Long>{boolean existsByCodeIgnoreCase(String code);}
