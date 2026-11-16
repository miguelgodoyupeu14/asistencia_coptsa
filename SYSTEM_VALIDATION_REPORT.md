# Informe de validación del sistema

Fecha: 23 de octubre de 2026 · zona funcional: `America/Lima`.

## Dictamen

El sistema queda listo para un **piloto controlado de software**. No se certifica como producción ni se declaran aprobados GPS, cámara, BSSID o APK en hardware que no estuvo conectado. La integridad principal se validó en Spring/H2 compatible con MySQL y el despliegue real se reconstruyó sobre MySQL 8.4 con Flyway V11.

## Reglas agregadas o completadas

- Separación de marcación personal, contingencia y corrección.
- Contingencia de admin/supervisor con alcance, motivos controlados, comentario obligatorio para `OTHER`, canal manual, actor, sede, hora efectiva/registrada y evento de negocio.
- Contexto diario basado en ventana real, secuencia, QR activo y motivos precisos.
- Precisión GPS obligatoria cuando hay geocerca y límite configurable de 100 m.
- Estados diferenciados de dispositivo y revocación con autor/fecha/motivo; aprobar reemplazo revoca el anterior.
- Política Web/iPhone sin requisito de dispositivo y rechazo explícito cuando la sede exige BSSID.
- QR validado al marcar, con `NO_ACTIVE_QR`, `QR_REQUIRED`, `QR_INVALID`, `QR_SITE_MISMATCH`, `QR_INACTIVE` y `QR_EXPIRED`.
- Calendario por sede para feriado, no laborable, día laborable especial y horario especial; el job no crea ausencias falsas.
- Corrección aprobada puede crear una jornada inexistente usando asignación/horario históricos y guarda request, revisor y motivo.
- Asignación organizacional `PERMANENT`/`TEMPORARY`; la temporal exige fecha final y motivo y prevalece durante su vigencia.
- Android envía canal `ANDROID` y precisión GPS; web eliminó la entrada manual del token QR.
- Pantalla responsive de contingencia, foto/iniciales del empleado, estado/turno, motivos y bloqueo funcional.
- Gestión visual de dispositivo revocado y advertencia de incompatibilidad Wi‑Fi con Safari.

## Contradicciones corregidas

| Antes | Ahora |
|---|---|
| Ausencia de teléfono podía tentar a relajar controles normales | ruta manual separada, atribuida y auditada |
| Contexto habilitaba por estado, no siempre por hora | usa el mismo validador de ventanas |
| GPS ausente y GPS impreciso se confundían | códigos y evidencia distintos |
| QR reemplazado/otra sede podían perder su causa exacta | precedencia y comparación de sede corregidas |
| Corrección exigía una jornada ya existente | puede crearla con configuración histórica válida |
| Feriado se infería solo del día semanal | calendario operativo evita ausencia falsa |
| Cambio temporal de sede chocaba con la permanente | asignación temporal explícita y prioritaria |
| Dispositivo aprobado anterior seguía activo | se revoca al aprobar reemplazo |

## Evidencia ejecutada

| Capa | Línea base | Resultado final |
|---|---:|---:|
| Backend | 30 | **51 PASS**, 0 fallos, 0 errores, 0 omitidas |
| Angular | 6 | **6 PASS**, build producción PASS |
| Flutter | 1 | **1 PASS**, `flutter analyze --no-pub` sin incidencias |
| Total de tests | 37 | **58 PASS** |

Pruebas nuevas relevantes: ventanas de entrada/salida parametrizadas, cinco fronteras de geocerca, códigos QR, feriado sin ausencia, contingencia atribuida/auditada. Se conservaron concurrencia, turno nocturno, permisos, ausencia idempotente, solicitudes, seguridad HTTP e IDOR/alcance existentes.

## Validación Docker/MySQL

- `docker compose up -d --build`: PASS.
- MySQL: saludable.
- Flyway: 11 migraciones validadas; V11 aplicada de versión 10 a 11 sin borrar datos.
- API `/actuator/health`: HTTP 200, `UP`.
- Web `http://localhost:4200`: HTTP 200.
- Login `admin/admin`: aceptado contra la API reconstruida.
- Endpoints autenticados de sedes y dispositivos: respondieron correctamente.

Advertencias no bloqueantes: Flyway utilizado declara soporte probado hasta MySQL 8.1 aunque la base es 8.4; Angular informa presupuesto SCSS excedido y dependencia CommonJS `qrcode`.

## Estado por escenario

- Empleado sin teléfono/batería/avería: contingencia en tiempo real; no bypass personal.
- Llegada tarde válida: se acepta y mide.
- Demasiado tarde: corrección, no hora escrita libremente.
- Sistema caído: corrección con `SYSTEM_OUTAGE`, conservando `effectiveAt`/`recordedAt`.
- Sin internet: no hay confirmación ni asistencia local.
- GPS desactivado/impreciso/fuera: códigos distintos, sin punch y con intento.
- QR ausente/vencido/inactivo/otra sede: rechazo específico.
- Teléfono nuevo/robado: `PENDING` o `REVOKED`, con administración y trazabilidad.
- Salida olvidada: `INCOMPLETE` y corrección.
- Doble request: un solo punch por constraint/transacción.
- Turno nocturno: misma `workDate`.
- Feriado/permiso: no ausencia injustificada.
- Cambio temporal de sede: asignación autorizada con vigencia y motivo.
- Dos sedes/horarios equivalentes: rechazo; no se elige al azar.
- Ausencia justificada: `ABSENT → EXCUSED` con historia.
- Supervisor: solo empleados de alcance; toda modificación requiere motivo.
- Acceso a otro empleado: las rutas personales resuelven identidad desde JWT.

## Pendientes y riesgos

1. `PENDING_PHYSICAL_DEVICE_TEST`: Safari/iPhone HTTPS con cámara y GPS, Android con APK/cámara/GPS/BSSID y prueba real dentro/fuera de UPeU.
2. Falta una clave de idempotencia HTTP; hoy la BD evita duplicados y devuelve conflicto controlado.
3. La generación simultánea de QR no usa lock explícito por sede; dos administradores concurrentes requieren una prueba/serialización adicional.
4. La política de sede no tiene aún historial de vigencias independiente; los punches conservan sede/canal/evidencia, pero una auditoría regulatoria puede exigir versionado de política.
5. El detector de mock location no se implementó y no se afirma que web pueda detectar GPS falso.
6. Reconocimiento facial no fue agregado: exige decisión legal, privacidad, prueba de vida y gestión biométrica.
7. Faltan pruebas automatizadas dedicadas para todas las combinaciones de dispositivo, asignación temporal, horario especial y corrección sin jornada; están listadas en `TEST_SCENARIOS.md`.

## Archivos de evidencia

- `BUSINESS_RULES_AUDIT.md`: auditoría previa y brechas.
- `ATTENDANCE_FLOW.md`: comportamiento actual.
- `ATTENDANCE_DECISION_MATRIX.md`: decisión por situación.
- `TEST_SCENARIOS.md`: cobertura y hardware pendiente.

El sistema compila y pasa pruebas, pero el criterio de cierre permanece condicionado al piloto físico y a la aceptación de RR. HH./seguridad sobre ventanas, tolerancias, privacidad y política de cada sede.
