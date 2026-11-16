# Flujo funcional de asistencia

Versión 2.0 · 23 de octubre de 2026 · zona oficial `America/Lima`.

## Actores

- `EMPLOYEE`: contexto propio, marcación personal, historial y solicitudes. Su `employeeId` sale del JWT.
- `SUPERVISOR`: solicitudes, incidencias, reportes y contingencias solo sobre empleados de su alcance.
- `ADMIN`: configuración, políticas, QR, dispositivos, calendario, correcciones y contingencias con motivo.
- `SYSTEM`: cierre idempotente de ausencias y jornadas incompletas.

## Preparación

1. Crear usuario activo con rol `EMPLOYEE`.
2. Crear empleado activo y vincularlo al usuario.
3. Crear sede, área y cargo.
4. Crear horario y turnos, incluido cruce de medianoche cuando corresponda.
5. Crear asignación organizacional permanente y asignación de horario con vigencia.
6. Opcionalmente crear una asignación `TEMPORARY` con fin y motivo; durante su vigencia reemplaza a la permanente para asistencia.
7. Configurar por sede GPS, QR, dispositivo y Wi‑Fi. En Safari/Web no se exige dispositivo, pero sí GPS/QR configurados; BSSID requerido bloquea Web explícitamente.
8. Si Android exige dispositivo, registrarlo y aprobarlo. Aprobar un reemplazo revoca el anterior; un equipo robado se marca `REVOKED` sin borrar historia.
9. Si la sede exige QR, generar uno activo. Un QR nuevo invalida al anterior.
10. Registrar feriados, días no laborables u horarios especiales en `/api/v1/attendance-calendar`.

## Contexto de hoy

`GET /api/v1/attendance/context` devuelve hora servidor, `workDate`, sede efectiva, turno, jornada, controles, acciones disponibles y motivo exacto de bloqueo. Angular y Flutter usan esta respuesta para presentar la pantalla, pero el backend repite todas las comprobaciones al marcar.

Estados de control: `PENDING`, `REQUIRED`, `NOT_REQUIRED` y `UNAVAILABLE`. La ausencia de QR activo produce `NO_ACTIVE_QR`; no se habilita entrada/salida por aproximación.

## Marcación personal

```text
JWT → empleado activo → asignación efectiva única → política de sede
    → calendario y horario únicos → ventana → secuencia
    → dispositivo/GPS/QR/Wi‑Fi aplicables → transacción
    → jornada + punch + intento aceptado
```

- Entrada válida crea/actualiza jornada `OPEN` y calcula tardanza.
- Llegar tarde dentro de ventana no bloquea; acumula `lateMinutes`.
- Salida anticipada se acepta; calcula `earlyExitMinutes`.
- Salida exige entrada y conserva el `workDate` de un turno nocturno.
- Una entrada/salida duplicada no persiste por validación y constraint único.
- GPS registra `accuracyMeters`; si supera el límite devuelve `LOCATION_LOW_ACCURACY`.
- QR se valida en el momento del punch: desconocido, otra sede, inactivo y vencido son causas distintas.
- Sin red no existe marcación hasta que el servidor confirme; el cliente nunca muestra éxito local.

## Contingencia en tiempo real

Se usa cuando la persona está presente dentro de su sede/turno/ventana, pero no puede usar el flujo personal.

1. Supervisor abre **Asistencia de contingencia**.
2. Selecciona empleado; se muestra código, foto, sede, turno, estado y hora servidor.
3. Elige entrada o salida y un motivo controlado: dispositivo olvidado, sin batería, averiado, perdido, falla de app u otro.
4. `OTHER` requiere comentario.
5. Backend valida alcance, empleado activo, sede/asignación efectiva, calendario, horario, ventana, secuencia y duplicados.
6. No exige las evidencias personales que motivaron la contingencia.
7. Persiste `MANUAL_SUPERVISOR` o `MANUAL_ADMIN`, `createdBy`, empleado, sede, `effectiveAt`, `recordedAt`, motivo y evento auditable.

No permite fechas pasadas/futuras ni horas elegidas. Fuera de esas reglas se usa corrección.

## Corrección posterior y caída general

El empleado presenta `CORRECTION` con fecha, tipo, hora propuesta y motivo. Supervisor/admin aprueba dentro de su alcance. El servicio puede crear la jornada que faltaba si existían sede y horario históricos válidos, crea un punch `CORRECTION`, vincula `requestId`, conserva `effectiveAt`, `recordedAt`, revisor y before/after, y recalcula métricas.

Una caída general se regulariza por el mismo flujo con motivo `SYSTEM_OUTAGE`; nunca se falsifica la hora de registro. Una solicitud rechazada no modifica asistencia.

## Cierre de jornada

```text
SIN_JORNADA --entrada--> OPEN --salida--> COMPLETED
SIN_JORNADA --fin de ventana--> ABSENT --justificación aprobada--> EXCUSED
OPEN --fin sin salida--> INCOMPLETE --corrección aprobada--> COMPLETED
```

El job solo procesa después de `fin de turno + ventana de salida`. Omite feriados/no laborables, respeta permisos completos y es idempotente. `OPEN` sin salida pasa a `INCOMPLETE`, nunca a `ABSENT`.

## Calendario

- `HOLIDAY` y `NON_WORKING_DAY`: bloquean marcación normal y no crean ausencia.
- `SPECIAL_WORKING_DAY`: usa horario regular o las horas especiales proporcionadas.
- `SPECIAL_SCHEDULE`: exige inicio/fin y reemplaza la ventana del día.

## Datos y auditoría

- `punches.occurred_at` es la hora efectiva; `created_at` es la hora registrada.
- La jornada es única por empleado/fecha y el punch por jornada/tipo.
- Intentos personales aceptados/rechazados guardan causa, sede, acción, canal, IP, hash de dispositivo, GPS, precisión, BSSID y correlación.
- Contingencias guardan evento de negocio con actor, resultado, canal, motivo y timestamps.
- Correcciones conservan solicitud, before/after y aprobador.
- No se borra historia para “arreglar” datos.

## Respuestas al usuario

La API usa `ProblemDetail` con `code`, `detail` y, cuando existe, `correlationId`. Los clientes traducen códigos funcionales. Ejemplos:

- `NO_ACTIVE_SCHEDULE`: no tienes horario vigente.
- `CHECK_IN_WINDOW_CLOSED`: la entrada ya no puede registrarse normalmente; solicita corrección.
- `LOCATION_LOW_ACCURACY`: espera una lectura GPS más precisa.
- `QR_EXPIRED`: vuelve a escanear el QR vigente.
- `DEVICE_PENDING`: el dispositivo espera aprobación.
- `WIFI_UNAVAILABLE_ON_WEB`: esta sede requiere una capacidad no disponible en Safari/Web.

La matriz completa está en [ATTENDANCE_DECISION_MATRIX.md](ATTENDANCE_DECISION_MATRIX.md).
