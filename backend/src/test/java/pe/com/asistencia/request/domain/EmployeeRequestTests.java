package pe.com.asistencia.request.domain;
import org.junit.jupiter.api.Test; import pe.com.asistencia.employee.domain.Employee; import pe.com.asistencia.security.domain.User; import java.time.LocalDate; import java.util.Set; import static org.assertj.core.api.Assertions.*;
class EmployeeRequestTests {
 @Test void resolvedRequestCannotBeReviewedTwice(){var employee=new Employee("E004","DNI","55667788","Sol","Díaz",null,null,null,LocalDate.now(),null);var reviewer=new User("admin","{noop}secret",Set.of());var request=new EmployeeRequest(employee,RequestType.JUSTIFICATION,"Motivo",null,null,null,null);request.review(reviewer,RequestStatus.APPROVED,"Conforme");assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);assertThatThrownBy(()->request.review(reviewer,RequestStatus.REJECTED,"Cambio")).isInstanceOf(IllegalStateException.class);}
}
