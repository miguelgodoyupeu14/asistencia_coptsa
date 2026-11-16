package pe.com.asistencia.security.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import pe.com.asistencia.security.repository.UserRepository;
import pe.com.asistencia.security.service.TokenService;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final TokenService tokens;
    public AuthController(AuthenticationManager authenticationManager, UserRepository users, TokenService tokens) {
        this.authenticationManager = authenticationManager; this.users = users; this.tokens = tokens;
    }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        var user = users.findByUsernameIgnoreCase(request.username()).orElseThrow();
        user.markLogin(Instant.now());
        users.save(user);
        var token = tokens.create(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresAt());
    }
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String accessToken, String tokenType, Instant expiresAt) {}
}
