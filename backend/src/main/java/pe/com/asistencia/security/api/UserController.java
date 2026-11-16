package pe.com.asistencia.security.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.com.asistencia.security.domain.RoleCode;
import pe.com.asistencia.security.service.UserService;
import java.time.Instant;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }
    @GetMapping public java.util.List<UserResponse> list() { return service.list(); }
    @GetMapping("/{id}") public UserResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public UserResponse create(@Valid @RequestBody CreateUserRequest request) { return service.create(request); }
    @PatchMapping("/{id}/status") public UserResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) { return service.setStatus(id, request.active()); }
    @PatchMapping("/{id}/password") @ResponseStatus(HttpStatus.NO_CONTENT) public void password(@PathVariable Long id, @Valid @RequestBody PasswordRequest request) { service.setPassword(id, request.password()); }
    public record CreateUserRequest(@NotBlank @Size(max=120) String username, @NotBlank @Size(min=10,max=100) String password, @Size(max=1000) String photoUrl, @NotEmpty Set<RoleCode> roles) {}
    public record StatusRequest(@NotNull Boolean active) {}
    public record PasswordRequest(@NotBlank @Size(min=10,max=100) String password) {}
    public record UserResponse(Long id, String username, String photoUrl, boolean active, Set<RoleCode> roles, Instant createdAt, Instant lastLoginAt) {}
}
