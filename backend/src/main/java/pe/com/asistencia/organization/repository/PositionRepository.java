package pe.com.asistencia.organization.repository;
import org.springframework.data.jpa.repository.JpaRepository; import pe.com.asistencia.organization.domain.Position;
public interface PositionRepository extends JpaRepository<Position,Long>{ boolean existsByCodeIgnoreCase(String code); }
