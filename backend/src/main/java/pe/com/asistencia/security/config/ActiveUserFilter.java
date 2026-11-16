package pe.com.asistencia.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.com.asistencia.security.repository.UserRepository;

@Component
public class ActiveUserFilter extends OncePerRequestFilter {
    private final UserRepository users;

    public ActiveUserFilter(UserRepository users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
            && !users.findByUsernameIgnoreCase(authentication.getName()).map(user -> user.isActive()).orElse(false)) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"type\":\"urn:problem:user-inactive\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"USER_INACTIVE\",\"code\":\"USER_INACTIVE\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
