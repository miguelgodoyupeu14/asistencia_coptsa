# Contrato vigente de la API

Base URL: `/api/v1`. JSON, fechas `AAAA-MM-DD` e instantes ISO 8601 UTC. Los recursos protegidos usan `Authorization: Bearer <JWT>`.

## Autenticación y administración

- `POST /auth/login`: emite JWT.
- `/users`: usuarios, estado y restablecimiento administrativo de contraseña (`ADMIN`).
- `/supervisor-scopes`: alcance de supervisores por sede/área (`ADMIN`).
- `/employees`: consulta con alcance para supervisor; escritura administrativa.
- `/organization/sites`, `/areas`, `/positions`: catálogos; escritura administrativa.
- `/employee-assignments`: asignaciones organizacionales y cierre por vigencia.
- `/schedules`, `/schedules/assignments`: horarios, turnos, asignaciones y cierre por vigencia.

## Asistencia

- `GET /attendance/context`: sede, política, turno, jornada y acciones permitidas del empleado autenticado.
- `POST /attendance/punch`: registra `ENTRY` o `EXIT`; la hora siempre proviene del servidor.
- `GET /attendance/me?from=...&to=...`: historial personal.
- `POST /attendance/process-absences?date=...`: reprocesamiento administrativo; el job automático usa la misma lógica.

El payload de marcación admite `channel`, `deviceIdentifier`, `latitude`, `longitude`, `qrCode` y `wifiBssid`. El backend exige únicamente los campos activados en la política de la sede.

## Seguridad de marcación

- `/attendance-security/devices`: registro personal, consulta y decisión administrativa.
- `POST /attendance-security/qr`: QR temporal para sede.
- `GET /attendance-security/attempts`: evidencia de intentos.
- `PATCH /organization/sites/{id}/attendance-security`: política de geocerca, dispositivo, QR y Wi-Fi.

## Operación

- `/requests`: alta personal, consulta propia y revisión con alcance. Tipos: `JUSTIFICATION`, `PERMISSION`, `CORRECTION`.
- `/incidents`: incidencias propias o gestión administrativa/supervisada.
- `GET /reports/attendance-summary`: rango obligatorio; filtros opcionales `employeeId`, `siteId`, `areaId`.
- `/audit`: auditoría administrativa.

## Errores

Se usa `application/problem+json` con `status`, `detail` y `code`. Los fallos funcionales usan códigos estables, por ejemplo `GPS_REQUIRED`, `OUTSIDE_GEOFENCE`, `DEVICE_NOT_APPROVED`, `QR_EXPIRED`, `INVALID_WIFI`, `NO_ACTIVE_SCHEDULE`, `ALREADY_CHECKED_IN` y `CHECK_IN_REQUIRED`. Validación/formato devuelve 400, autenticación 401, autorización 403, inexistente 404, método inválido 405 y conflicto 409. No se exponen stacktraces.

## Invariantes

- `EMPLOYEE` se resuelve desde el JWT; no elige libremente otro `employeeId`.
- `SUPERVISOR` solo accede a empleados dentro de su alcance.
- Una jornada tiene como máximo una entrada y una salida.
- Las correcciones pasan por solicitud/aprobación y conservan auditoría antes/después.
- Los instantes se transmiten en UTC y las reglas laborales se calculan en `America/Lima`.
