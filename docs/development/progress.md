# Progreso del proyecto

**FASE ACTUAL:** Cierre de remediación y validación integral

**Estado:** Completada y validada

## Alcance actual

- Empaquetado de API y web, orquestación Compose y guía operativa del piloto.
- APK Android debug y configuración externa de firma para release.
- Configuración web de asignaciones organizacionales y de horario.
- Políticas por sede para dispositivo, geocerca, QR y BSSID Wi‑Fi.
- QR visual escaneable, auditoría de motivos y APK piloto compilada.
- Colección Postman, estrategia de pruebas y checklist de piloto.

## Fuera del alcance técnico automatizable

- Aceptación final de reglas laborales por RR. HH.
- Pruebas de cámara, GPS y Wi-Fi con los dispositivos físicos objetivo.
- Firma y publicación release en una tienda.
- Infraestructura pública definitiva, respaldo/restauración y monitoreo operacional.

## Fases completadas

- Fase 0 — Estructura inicial del proyecto.
- Fase 1 — Documentación técnica inicial.
- Fase 2 — Diseño conceptual y lógico inicial del modelo de datos.
- Fase 3 — Inicialización del backend Spring Boot.
- Fase 4 — Configuración de MySQL y Flyway.
- Fase 5 — Usuarios, roles y permisos básicos.
- Fase 6 — Autenticación y autorización JWT.
- Fase 7 — Sedes, áreas y cargos.
- Fase 8 — Gestión básica de empleados.
- Fase 9 — Horarios, turnos y asignaciones con vigencia.
- Fase 10 — Registro básico de asistencia y jornadas.
- Fase 11 — Reglas de tardanzas, salidas e inasistencias.
- Fase 12 — Incidencias, justificaciones, permisos y correcciones.
- Fase 13 — Auditoría HTTP persistente.
- Fase 14 — Reporte agregado de asistencia.
- Fase 15 — Inicialización de Angular.
- Fase 16 — Autenticación, guard e interceptor Angular.
- Fase 17 — Layout y navegación responsive.
- Fase 18 — Módulos administrativos web.
- Fase 19 — Gestión de asistencia web.
- Fase 20 — Reportes web.
- Fase 21 — Inicialización de Flutter.
- Fase 22 — Autenticación Flutter y almacenamiento seguro.
- Fase 23 — Inicio y perfil del empleado.
- Fase 24 — Registro móvil de asistencia.
- Fase 25 — Historial y horario móvil.
- Fase 26 — Solicitudes móviles.
- Fase 27 — Validación de dispositivo e intentos.
- Fase 28 — Geolocalización y geocerca por sede.
- Fase 29 — QR dinámico temporal.
- Fase 30 — Auditoría avanzada de intentos.
- Fase 31 — Pruebas automatizadas multiplataforma.
- Fase 32 — Integración API–MySQL real: Flyway, health, login JWT y persistencia validados.
- Fase 33 — Preparación técnica y documental del piloto.

## Validación final

El 16 de octubre de 2026 se ejecutó la validación integral: 30 pruebas backend, build y 3 pruebas Angular, análisis y 1 prueba Flutter, todas aprobadas. Docker Compose quedó reconstruido con MySQL saludable, API `UP`, web HTTP 200 y Flyway V1–V10. Se regeneró `mobile/releases/asistencia-mobile-pilot-debug.apk` para el HTTPS del piloto. El dictamen y los límites de certificación están en `SYSTEM_VALIDATION_REPORT.md`.
