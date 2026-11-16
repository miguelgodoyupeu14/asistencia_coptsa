package pe.com.asistencia.security.api;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.http.MediaType; import org.springframework.test.web.servlet.MockMvc; import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt; import pe.com.asistencia.security.domain.User; import pe.com.asistencia.security.repository.UserRepository; import java.util.Set;
@SpringBootTest @AutoConfigureMockMvc class SecurityEndpointsTests {
 @Autowired MockMvc mvc;
 @Autowired UserRepository users;
 @Test void protectedEndpointRejectsAnonymousUser()throws Exception{mvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());}
 @Test void loginRejectsInvalidCredentials()throws Exception{mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"missing\",\"password\":\"invalid-password\"}")).andExpect(status().isUnauthorized());}
 @Test void healthEndpointIsPublic()throws Exception{mvc.perform(get("/actuator/health")).andExpect(status().isOk());}
 @Test void unknownAuthenticatedApiRouteReturnsNotFoundInsteadOfInternalError()throws Exception{users.save(new User("route-tester","{noop}password",Set.of()));mvc.perform(get("/api/v1/route-that-does-not-exist").with(jwt().jwt(token->token.subject("route-tester")))).andExpect(status().isNotFound());}
}
