package pe.com.asistencia.employee.api;
import org.springframework.http.HttpStatus; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException; import pe.com.asistencia.employee.repository.EmployeeRepository; import java.time.LocalDate;
@RestController @RequestMapping("/api/v1/profile") public class ProfileController {
 private final EmployeeRepository employees; public ProfileController(EmployeeRepository e){employees=e;}
 @GetMapping("/me") public View me(Authentication auth){var e=employees.findByUserUsernameIgnoreCase(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Perfil de empleado no encontrado"));return new View(e.getId(),e.getCode(),e.getFirstNames(),e.getLastNames(),e.getEmail(),e.getPhone(),e.getHireDate(),e.isActive());}
 public record View(Long id,String code,String firstNames,String lastNames,String email,String phone,LocalDate hireDate,boolean active){}
}
