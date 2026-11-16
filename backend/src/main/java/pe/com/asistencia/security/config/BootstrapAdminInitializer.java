package pe.com.asistencia.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.com.asistencia.security.domain.*;
import pe.com.asistencia.security.repository.*;
import java.util.Set;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final String username; private final String password; private final boolean resetExisting; private final UserRepository users; private final RoleRepository roles; private final PasswordEncoder encoder;
    public BootstrapAdminInitializer(@Value("${app.bootstrap-admin.username:}") String username, @Value("${app.bootstrap-admin.password:}") String password, @Value("${app.bootstrap-admin.reset-existing:false}") boolean resetExisting, UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.username=username; this.password=password; this.resetExisting=resetExisting; this.users=users; this.roles=roles; this.encoder=encoder;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        if (username.isBlank() && password.isBlank()) return;
        if (username.isBlank() || password.isBlank()) throw new IllegalStateException("El administrador inicial requiere usuario y contraseña");
        Role admin = roles.findByCodeAndActiveTrue(RoleCode.ADMIN).orElseThrow();
        users.findByUsernameIgnoreCase(username).ifPresentOrElse(user -> {
            if (resetExisting) {
                user.setPasswordHash(encoder.encode(password));
                user.setActive(true);
                user.getRoles().add(admin);
                users.save(user);
            }
        }, () -> users.save(new User(username, encoder.encode(password), Set.of(admin))));
    }
}
