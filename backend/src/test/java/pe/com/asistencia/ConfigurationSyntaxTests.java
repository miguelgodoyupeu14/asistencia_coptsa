package pe.com.asistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class ConfigurationSyntaxTests {

    @Test
    void productionYamlIsValid() {
        try (InputStream input = Files.newInputStream(Path.of("src/main/resources/application.yml"))) {
            Map<String, Object> configuration = new Yaml().load(input);
            assertThat(configuration).containsKeys("spring", "server", "management", "app");
        } catch (Exception exception) {
            throw new AssertionError("application.yml must be valid YAML", exception);
        }
    }
}
