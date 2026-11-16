package pe.com.asistencia.security.service;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import pe.com.asistencia.security.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public DatabaseUserDetailsService(UserRepository users) { this.users = users; }
    @Override public UserDetails loadUserByUsername(String username) {
        var user = users.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        String[] roles = user.getRoles().stream().map(r -> r.getCode().name()).toArray(String[]::new);
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
            .password(user.getPasswordHash()).roles(roles).disabled(!user.isActive()).build();
    }
}
