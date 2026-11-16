# Auditoría de reglas de negocio de asistencia

Fecha de corte: 23 de octubre de 2026  
Zona horaria funcional: `America/Lima`  
Alcance revisado: Spring Boot, Angular, Flutter, MySQL/Flyway, seguridad, reportes, auditoría y pruebas.

## 1. Resumen ejecutivo

El sistema existente tiene una base válida y no debe reescribirse. Ya implementa autenticación JWT, empleados, sedes, asignaciones con vigencia, horarios, jornada única por empleado/fecha, entrada/salida, geocerca, QR dinámico, dispositivos, Wi-Fi, solicitudes, incidencias, alcance de supervisores, ausencias idempotentes y correcciones aprobadas.

La principal brecha no es tecnológica sino de separación de procesos. La marcación personal, la contingencia y la corrección todavía no forman tres caminos inequívocos. Además, el contexto del día declara acciones disponibles sin considerar la ventana horaria ni el estado puntual de cada control; esto puede habilitar visualmente una acción que el backend rechazará.

Estado de la línea base antes de esta fase:

| Capa | Archivos de prueba | Pruebas | Resultado |
|---|---:|---:|---|
| Spring Boot | 12 | 30 | PASS |
| Angular | 3 | 6 | PASS |
| Flutter | 1 | 1 | PASS |
| Total | 16 | 37 | 37 PASS / 0 FAIL |

> Actualización posterior a la auditoría: las brechas P0 de contexto horario, contingencia separada, corrección sin jornada, calendario y precisión GPS fueron implementadas. También se agregaron revocación de dispositivos, asignación temporal y códigos QR precisos. El resultado final y los riesgos residuales están en `SYSTEM_VALIDATION_REPORT.md`; la suite final contiene 58 pruebas aprobadas entre las tres capas.

## 2. Modelo de negocio reconstruido

### Actores

- `EMPLOYEE`: consulta su contexto, realiza marcación personal, consulta historial y presenta solicitudes.
- `SUPERVISOR`: revisa empleados, solicitudes e incidencias únicamente dentro de sus alcances de sede/área.
- `ADMIN`: configura organización, horarios, políticas, dispositivos, QR, usuarios y excepciones mayores.
- `SYSTEM`: procesa ausencias y jornadas incompletas después del cierre efectivo.

### Agregados y vigencias

- Usuario y roles.
- Empleado vinculado opcionalmente a un usuario.
- Asignación organizacional con sede, área, cargo, `validFrom` y `validTo`.
- Horario y asignación de horario con vigencia.
- Turno por día de semana, incluido cruce de medianoche.
- Jornada identificada por empleado y `workDate`.
- Marcaciones `ENTRY`/`EXIT`, con una restricción única por jornada y tipo.
- Solicitudes `PERMISSION`, `JUSTIFICATION` y `CORRECTION`.
- Política de seguridad vigente en la sede.
- Intentos aceptados/rechazados y auditoría HTTP.

### Caminos que deben permanecer separados

1. **Flujo normal:** el empleado marca personalmente y cumple los controles configurados.
2. **Contingencia:** un supervisor autorizado registra en tiempo real por una imposibilidad legítima del dispositivo; no suplanta un canal personal.
3. **Corrección posterior:** una solicitud aprobada incorpora una hora efectiva distinta de la hora de registro.
4. **Incidencia:** documenta una anomalía, pero por sí sola no inventa ni modifica una marcación.

## 3. Reglas existentes y verificadas

| Regla | Implementación actual | Estado |
|---|---|---|
| Empleado personal se resuelve desde JWT | Repositorio por username, sin `employeeId` enviado por cliente | COMPLETA |
| Usuario y empleado deben estar activos | Autenticación y `AttendanceService` | COMPLETA |
| Una jornada por empleado y `workDate` | Unique `(employee_id, work_date)` | COMPLETA |
| Una entrada y una salida por jornada | Unique `(attendance_day_id, punch_type)` | COMPLETA |
| Doble request no crea dos marcaciones | Constraint + traducción a conflicto | PARCIAL: falta idempotency key |
| Turno nocturno conserva `workDate` en salida | Resolución de jornada abierta anterior | PARCIAL: depende de que exista entrada |
| Llegada tarde dentro de ventana se registra | `lateMinutes` calculado en servidor | COMPLETA |
| Salida anticipada se permite y calcula | `earlyExitMinutes` | COMPLETA |
| Salida sin entrada se rechaza | `EXIT_WITHOUT_ENTRY` | COMPLETA |
| Entrada/salida fuera de ventana se rechaza | `AttendanceRuleValidator` | COMPLETA |
| Seguridad depende de sede | GPS, dispositivo, QR y Wi-Fi por política | COMPLETA |
| Backend revalida controles | `AttendanceSecurityService` | COMPLETA |
| QR usa token aleatorio y hash | `SecureRandom`, SHA-256 y expiración | COMPLETA |
| QR nuevo invalida el anterior | Update por sede antes de insertar | PARCIAL: falta serialización concurrente |
| QR puede ser usado por varios empleados | No se consume por uso individual | COMPLETA |
| BSSID se normaliza | Mayúsculas y separadores normalizados | COMPLETA |
| Safari no inventa BSSID | Web envía `null` y bloquea si Wi-Fi es obligatorio | COMPLETA |
| Ausencia se procesa después del cierre | Fin de turno + ventana de salida | COMPLETA |
| Job de ausencias es idempotente | Jornada única e incidencia única | COMPLETA |
| Entrada sin salida termina `INCOMPLETE` | Proceso de cierre | COMPLETA |
| Permiso completo evita ausencia injustificada | `approved_permissions` | COMPLETA |
| Justificación aprobada produce `EXCUSED` | `RequestEffectService` | COMPLETA |
| Corrección aprobada crea punch `CORRECTION` | Servicio transaccional y ajuste before/after | PARCIAL |
| Supervisor tiene alcance por sede/área | `supervisor_scopes` | PARCIAL: no aplicado a todo endpoint |
| Errores contienen código funcional | RFC 9457 `ProblemDetail.code` | COMPLETA |
| Hora de puntualidad es del servidor | `Instant.now()` y `America/Lima` | COMPLETA |

## 4. Reglas incompletas o ausentes

### P0 — integridad y decisiones incorrectas

1. **Contexto diario impreciso.** `allowedActions` sólo observa si hay turno/entrada/salida; no calcula apertura o cierre de ventana, controles pendientes ni `blockingReason`. La UI puede mostrar “Entrada disponible” muchas horas después.
2. **No existe marcación de contingencia.** No hay endpoint, entidad o pantalla diferenciada para `MANUAL_SUPERVISOR`/`MANUAL_ADMIN`, motivo controlado, actor creador y auditoría específica.
3. **Corrección sin jornada previa imposible.** Si el empleado olvidó entrada y no existe `attendance_day`, aprobar `CORRECTION` devuelve conflicto. Debe poder crear la jornada histórica usando el horario vigente de esa fecha.
4. **No existe calendario operativo.** Feriados y días especiales producirán ausencias falsas o impedirán jornadas especiales.
5. **Precisión GPS ignorada.** Los clientes conocen `accuracy`, pero el backend no la recibe ni aplica `maxLocationAccuracyMeters`.
6. **Entrada manual del token QR en web.** Angular expone “Ingresar código manualmente”, lo que contradice la exigencia de escaneo por cámara y facilita compartir el token.

### P1 — seguridad y contingencias reales

7. **Estados de dispositivo agregados.** `PENDING`, `REJECTED` y dispositivo de otro empleado terminan como `DEVICE_NOT_APPROVED`; falta `REVOKED`, motivo y auditoría de revocación.
8. **Canal no es un concepto de dominio.** Se valida texto (`WEB|MOBILE|AUTOMATED_TEST`) y no existe `ANDROID`, `MANUAL_SUPERVISOR`, `MANUAL_ADMIN`, `CORRECTION`, `SYSTEM` como catálogo consistente.
9. **`effectiveAt` y `recordedAt` no son explícitos.** `punches.occurred_at` actúa como hora efectiva, pero no existe hora de registro ni actor/motivo estructurado en la marcación.
10. **QR ausente no distingue configuración.** Si la sede exige QR y no hay ninguno activo, se devuelve `QR_REQUIRED`; falta `NO_ACTIVE_QR`.
11. **Códigos QR inconsistentes.** Backend usa `QR_WRONG_SITE`; el contrato requerido es `QR_SITE_MISMATCH`. Debe existir un catálogo único para Angular/Flutter.
12. **Asignación temporal de sede no está tipificada.** Puede modelarse con vigencias, pero falta tipo, razón y flujo autorizado para distinguir traslado temporal.
13. **Política sin vigencia histórica.** Cambiar controles de una sede modifica la interpretación de configuración sin conservar versión; las marcaciones preservan evidencia parcial, pero no snapshot completo.
14. **Ausencia general del sistema sólo se corrige genéricamente.** Falta razón `SYSTEM_OUTAGE` y evidencia explícita de hora efectiva/hora registrada.
15. **Contingencia sospechosa no tiene reglas.** No existe servicio que impida contingencia fuera de turno, histórica/futura, duplicada o fuera del alcance.

### P2 — consistencia transversal

16. Flutter tiene una única prueba visual de login y no prueba sensores, contexto ni errores.
17. Angular tiene seis pruebas y ninguna cubre asistencia, cámara, GPS, permisos o códigos de rechazo.
18. El manifest principal Android permite tráfico HTTP; esto no debe llegar a release.
19. La actividad de usuario debe verificarse en cada request sensible o debe documentarse la ventana de JWT de 15 minutos.
20. `X-Forwarded-For` se confía sin una lista explícita de proxies confiables.
21. La auditoría HTTP no sustituye eventos de negocio con actor, sujeto, motivo y before/after.
22. La política de un solo dispositivo principal aprobado no está implementada.
23. No hay protección transaccional para dos asignaciones o dos QR activos creados concurrentemente.
24. Reportes no exponen de forma clara canal, correcciones, contingencias y estado original de una ausencia justificada.

## 5. Contradicciones encontradas

| Contradicción | Riesgo | Resolución requerida |
|---|---|---|
| La UI dice acción disponible pero el validador puede declarar ventana cerrada | Confusión y pruebas inconsistentes | Contexto calculado por servidor |
| Web exige QR por cámara pero permite escribir el token | Compartición remota del QR | Eliminar entrada manual |
| `occurred_at` representa hora real y hora de registro a la vez | Auditoría falsa en correcciones | Separar `effective_at`/`recorded_at` |
| Canal `MOBILE` no distingue Android de contingencia | Reportes ambiguos | Catálogo de canales |
| Dispositivo no aprobado agrupa tres estados | Mensaje/operación incorrectos | Códigos por estado |
| Corrección exige jornada, pero olvido total de entrada no crea jornada | Caso obligatorio bloqueado | Crear jornada histórica al aprobar |
| Calendario semanal genera ausencias en feriado | Datos falsos | Calendario de excepciones |

## 6. Funcionalidades que existen pero no están demostradas E2E

- Entrada aceptada real con GPS + QR + dispositivo contra MySQL.
- Salida aceptada y cálculo final completo.
- Escaneo físico del QR desde Android y Safari.
- BSSID real desde Android.
- Corrección aprobada reflejada en reporte.
- Justificación aprobada reflejada en reporte.
- Alcance de supervisor aplicado a todos los listados/operaciones.
- Turno nocturno completo atravesando medianoche en API real.
- Job programado ejecutado por el scheduler, no sólo invocado desde test.
- Concurrencia real HTTP y reintento con idempotency key.

## 7. Decisiones que pueden producir datos inconsistentes

- Confiar en `allowedActions` actual para habilitar el botón sin mostrar la ventana real.
- Modificar horarios o sedes sin conservar correctamente sus vigencias.
- Aprobar una corrección sobre jornada cerrada sin registrar actor, motivo y hora de registro en el propio punch.
- Permitir códigos QR escritos manualmente.
- Cambiar política de sede sin snapshot o versión aplicable al intento.
- Aceptar precisión GPS desconocida o extremadamente baja como si fuera una coordenada confiable.
- Tratar una caída general como una marcación normal realizada en el momento.
- Usar contingencia futura sin comprobar sede, alcance, turno, ventana y secuencia.

## 8. Orden de implementación adoptado

1. Contexto diario y catálogo consistente de códigos/canales.
2. Precisión GPS y seguridad QR/dispositivo sin atajos.
3. Marcación de contingencia con alcance, motivos y auditoría.
4. Corrección posterior con `effectiveAt`/`recordedAt` explícitos.
5. Calendario de excepciones y ausencia segura.
6. Reportes y clientes Angular/Flutter.
7. Matriz de pruebas automatizadas y validaciones físicas pendientes.

## 9. Criterios que no se declararán aprobados sin hardware

- Cámara y escaneo QR físico en iPhone/Android.
- Ubicación real y precisión reportada por el teléfono.
- Detección de ubicación simulada en Android, si la plataforma la expone.
- Lectura de BSSID real en Android.
- Notificaciones en segundo plano en iPhone.

Estos casos deberán figurar como `PENDING_PHYSICAL_DEVICE_TEST` hasta ejecutarse en un dispositivo real.
