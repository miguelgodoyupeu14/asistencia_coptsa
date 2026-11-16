package pe.com.asistencia.security.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.com.asistencia.security.api.UserController.*;
import pe.com.asistencia.security.domain.*;
import pe.com.asistencia.security.repository.*;
import java.util.*;

@Service
@Transactional
public class UserService {
    private final UserRepository users; private final RoleRepository roles; private final PasswordEncoder encoder;
    public UserService(UserRepository users, RoleRepository roles, PasswordEncoder encoder) { this.users=users; this.roles=roles; this.encoder=encoder; }
    @Transactional(readOnly=true) public List<UserResponse> list() { return users.findAll().stream().map(this::response).toList(); }
    @Transactional(readOnly=true) public UserResponse get(Long id) { return response(find(id)); }
    public UserResponse create(CreateUserRequest request) {
        if (users.existsByUsernameIgnoreCase(request.username())) throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        Set<Role> assigned = new HashSet<>();
        request.roles().forEach(code -> assigned.add(roles.findByCodeAndActiveTrue(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no disponible: " + code))));
        var user = new User(request.username(), encoder.encode(request.password()), assigned);
        user.setPhotoUrl(request.photoUrl());
        return response(users.save(user));
    }
    public UserResponse setStatus(Long id, boolean active) { var user=find(id); user.setActive(active); return response(user); }
    public void setPassword(Long id, String password) { find(id).setPasswordHash(encoder.encode(password)); }
    private User find(Long id) { return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado")); }
    private UserResponse response(User u) { return new UserResponse(u.getId(),u.getUsername(),u.getPhotoUrl(),u.isActive(),u.getRoles().stream().map(Role::getCode).collect(java.util.stream.Collectors.toSet()),u.getCreatedAt(),u.getLastLoginAt()); }
}
