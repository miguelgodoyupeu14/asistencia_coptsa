package pe.com.asistencia.organization.repository;
import org.springframework.data.jpa.repository.JpaRepository; import pe.com.asistencia.organization.domain.Area;
public interface AreaRepository extends JpaRepository<Area,Long>{ boolean existsByCodeIgnoreCase(String code); }
