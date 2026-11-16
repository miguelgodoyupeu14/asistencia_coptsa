package pe.com.asistencia.employee.repository;
import org.springframework.data.jpa.repository.JpaRepository; import pe.com.asistencia.employee.domain.Employee; import java.util.Optional;
public interface EmployeeRepository extends JpaRepository<Employee,Long>{boolean existsByCodeIgnoreCase(String code); boolean existsByDocumentTypeIgnoreCaseAndDocumentNumber(String type,String number); Optional<Employee> findByUserUsernameIgnoreCase(String username);}
