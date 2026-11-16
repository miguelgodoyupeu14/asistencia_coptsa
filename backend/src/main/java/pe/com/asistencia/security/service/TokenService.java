package pe.com.asistencia.security.service;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import pe.com.asistencia.security.config.JwtProperties;
import pe.com.asistencia.security.domain.User;
import java.time.Instant;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    public TokenService(JwtEncoder encoder, JwtProperties properties) { this.encoder = encoder; this.properties = properties; }
    public Token create(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.accessTokenMinutes() * 60);
        var roles = user.getRoles().stream().map(r -> r.getCode().name()).sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.issuer()).issuedAt(now).expiresAt(expiresAt)
            .subject(user.getId().toString()).claim("preferred_username", user.getUsername()).claim("roles", roles).build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Token(value, expiresAt);
    }
    public record Token(String value, Instant expiresAt) {}
}
