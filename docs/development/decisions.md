# Decisiones de arquitectura

Este documento registrará decisiones técnicas que afecten la evolución del sistema.

## Plantilla

### ADR-XXX — Título de la decisión

- **Fecha:** AAAA-MM-DD
- **Estado:** Propuesta | Aceptada | Reemplazada
- **Contexto:** Problema o necesidad que origina la decisión.
- **Decisión:** Alternativa elegida.
- **Consecuencias:** Beneficios, limitaciones y trabajo futuro.
- **Alternativas consideradas:** Opciones relevantes descartadas y motivo.

---

### ADR-001 — API REST compartida por los clientes

- **Fecha:** 2026-09-18
- **Estado:** Aceptada
- **Contexto:** Las aplicaciones web y móvil requieren acceso consistente a las mismas funciones y datos.
- **Decisión:** Angular y Flutter consumirán una única API REST desarrollada con Spring Boot.
- **Consecuencias:** Ambos clientes compartirán contratos y comportamiento; la evolución de la API deberá considerar su compatibilidad.
- **Alternativas consideradas:** Backends independientes por cliente, descartados para evitar duplicación y divergencia.

### ADR-002 — Reglas de negocio centralizadas en el backend

- **Fecha:** 2026-09-18
- **Estado:** Aceptada
- **Contexto:** Las reglas de asistencia deben producir resultados consistentes desde cualquier cliente.
- **Decisión:** Las reglas de negocio se implementarán principalmente en los servicios del backend. Los clientes se limitarán a presentación, interacción y validaciones de experiencia de usuario.
- **Consecuencias:** La API será la autoridad sobre los resultados y los clientes no deberán replicar cálculos críticos.
- **Alternativas consideradas:** Implementar reglas en cada cliente, descartado por duplicación y riesgo de inconsistencias.

### ADR-003 — Desarrollo incremental por fases

- **Fecha:** 2026-09-18
- **Estado:** Aceptada
- **Contexto:** El sistema abarca varios dominios y plataformas, y requiere validación progresiva.
- **Decisión:** Implementar y validar una fase antes de avanzar a la siguiente.
- **Consecuencias:** Los cambios serán pequeños y revisables; el roadmap podrá ajustarse cuando aparezcan requisitos confirmados.
- **Alternativas consideradas:** Implementación simultánea de todo el sistema, descartada por aumentar el riesgo y dificultar la revisión.

### ADR-004 — Historial mediante asignaciones con vigencia

- **Fecha:** 2026-09-18
- **Estado:** Propuesta
- **Contexto:** La sede, el área, el cargo y el horario de un empleado pueden cambiar, pero los cálculos históricos deben conservar el contexto aplicable en cada fecha.
- **Decisión:** Modelar estos cambios mediante asignaciones con inicio y fin de vigencia.
- **Consecuencias:** Será posible reconstruir el contexto histórico; deberán definirse reglas para impedir solapamientos inválidos.
- **Alternativas consideradas:** Guardar únicamente la asignación actual en el empleado, opción más simple pero insuficiente para trazabilidad histórica.

### ADR-005 — Marcaciones originales separadas de resultados calculados

- **Fecha:** 2026-09-18
- **Estado:** Propuesta
- **Contexto:** Los cálculos de asistencia pueden cambiar por correcciones, permisos o nuevas reglas, mientras que el evento recibido debe seguir siendo auditable.
- **Decisión:** Conservar `Marcacion` como evento original y almacenar el resultado consolidado en `JornadaAsistencia`.
- **Consecuencias:** Las jornadas podrán recalcularse sin perder evidencia; será necesario controlar versiones y correcciones.
- **Alternativas consideradas:** Sobrescribir una marcación o almacenar solo el resultado diario, descartado por pérdida de trazabilidad.

### ADR-006 — Java 21 y Spring Boot 3.5

- **Fecha:** 2026-09-18
- **Estado:** Aceptada
- **Contexto:** El backend requiere una base estable, mantenible y compatible con herramientas actuales.
- **Decisión:** Compilar con Java 21 LTS y usar la línea estable Spring Boot 3.5.
- **Consecuencias:** Se obtiene soporte para Java moderno sin depender de una versión no LTS; las actualizaciones menores de Spring Boot se mantendrán dentro de la línea 3.5.
- **Alternativas consideradas:** Java 23 como nivel objetivo y Spring Boot 4.1, descartados inicialmente para priorizar compatibilidad y madurez del ecosistema.

### ADR-007 — Instantes persistidos en UTC

- **Fecha:** 2026-09-18
- **Estado:** Aceptada
- **Contexto:** Las marcaciones deben ser inequívocas y el sistema puede incorporar sedes en distintas zonas horarias.
- **Decisión:** Persistir y serializar instantes en UTC, conservando la zona horaria de negocio como configuración o dato de la sede.
- **Consecuencias:** Los clientes y reportes deberán convertir los instantes a la zona aplicable; la fecha laboral no se inferirá solo desde UTC.
- **Alternativas consideradas:** Persistir horas locales sin zona, descartado por ambigüedad y dificultad para auditoría.

### ADR-008 — Angular 21 para la aplicación web

- **Fecha:** 2026-09-18
- **Estado:** Aceptada
- **Contexto:** Angular 22 requiere una versión de Node posterior a la instalada en el entorno, mientras Angular 21 mantiene soporte activo y es compatible con Node 22.20.
- **Decisión:** Inicializar la aplicación web con Angular 21, componentes standalone, routing, SCSS y comprobación estricta de tipos.
- **Consecuencias:** El proyecto puede construirse con el entorno actual y conserva una ruta de actualización posterior.
- **Alternativas consideradas:** Angular 22, pospuesto hasta actualizar Node; Angular 19 global, descartado por estar fuera de soporte activo.

### ADR-009 — Seguridad avanzada activable por sede

- **Fecha:** 2026-09-18
- **Estado:** Aceptada
- **Contexto:** El piloto necesita introducir controles de asistencia gradualmente y las sedes pueden tener capacidades distintas.
- **Decisión:** Dispositivo, geocerca y QR dinámico se configuran por sede y permanecen desactivados por defecto.
- **Consecuencias:** Cada control puede pilotearse de forma independiente; toda aceptación o rechazo queda en `punch_attempts`.
- **Alternativas consideradas:** Activación global simultánea, descartada por riesgo operativo.

### ADR-010 — Despliegue web y API bajo un mismo origen

- **Fecha:** 2026-09-18
- **Estado:** Aceptada
- **Contexto:** La aplicación web y la API requieren una integración sencilla y segura en el piloto.
- **Decisión:** Nginx sirve Angular y redirige `/api` al backend dentro de Docker Compose.
- **Consecuencias:** Se reduce la dependencia de CORS en producción; desarrollo conserva un proxy equivalente.
- **Alternativas consideradas:** Hosts independientes para web y API, posible más adelante si la infraestructura lo requiere.
