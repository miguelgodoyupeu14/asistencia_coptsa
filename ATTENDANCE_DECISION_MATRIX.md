# Matriz de decisión de asistencia

Fecha de corte: 23 de octubre de 2026. Hora oficial: servidor, zona `America/Lima`.

| Situación | ¿Marcación personal? | Resultado / código | Flujo posterior | Evidencia persistida |
|---|---:|---|---|---|
| Identidad, sede, turno, ventana y controles válidos | Sí | `ENTRY` o `EXIT` | Ninguno | jornada, punch, intento aceptado |
| Llegada tarde dentro de ventana | Sí | `OPEN`, `lateMinutes > 0` | Revisión posterior si corresponde | punch y métricas |
| Llegada después de la ventana | No | `CHECK_IN_WINDOW_CLOSED` | solicitud `CORRECTION` | intento rechazado; ajuste si se aprueba |
| Salida anticipada | Sí | `COMPLETED`, `earlyExitMinutes > 0` | incidencia/justificación opcional | punch y métricas |
| Salida sin entrada | No | `EXIT_WITHOUT_ENTRY` | solicitud `CORRECTION` | intento rechazado |
| Olvidó marcar salida | No automática | jornada `INCOMPLETE` | corrección aprobada | punch `CORRECTION`, request y before/after |
| Doble entrada/salida | No segunda vez | `DUPLICATE_ENTRY` / `DUPLICATE_EXIT` | Ninguno | un único punch por constraint |
| Sin teléfono, batería, daño, pérdida o falla de app | No | flujo personal no se relaja | contingencia de supervisor en tiempo real | `MANUAL_SUPERVISOR/ADMIN`, actor y motivo |
| Contingencia en fecha pasada/futura o fuera de ventana | No | rechazo funcional | corrección/regularización | solicitud y ajuste auditado |
| Caída general del sistema | No en tiempo real | no existe marcación hasta confirmación | corrección con motivo `SYSTEM_OUTAGE` | `effectiveAt` distinto de `recordedAt` |
| Sin conexión del cliente | No | mensaje de servicio no disponible | reintento o contingencia | ninguna asistencia falsa |
| GPS ausente | No | `GPS_REQUIRED` | habilitar permiso o contingencia | intento rechazado |
| GPS impreciso | No | `LOCATION_LOW_ACCURACY` | solicitar nueva lectura | coordenadas y precisión del intento |
| Fuera de geocerca | No | `OUTSIDE_GEOFENCE` | no hay “marcar de todas formas” | intento rechazado |
| QR requerido pero no generado | No | `NO_ACTIVE_QR` | admin genera QR | intento rechazado |
| QR no enviado con QR activo | No | `QR_REQUIRED` | escanear con cámara | intento rechazado |
| QR vencido | No | `QR_EXPIRED` | escanear el nuevo | intento rechazado |
| QR de otra sede | No | `QR_SITE_MISMATCH` | escanear QR de sede vigente | intento rechazado |
| QR reemplazado/inactivo | No | `QR_INACTIVE` | escanear el nuevo | intento rechazado |
| Cámara denegada | No | mensaje cliente `CAMERA_PERMISSION_REQUIRED` | conceder permiso o contingencia | no se ofrece token manual |
| Dispositivo nuevo | No en Android si es requerido | `DEVICE_PENDING` | aprobación admin | dispositivo `PENDING` |
| Dispositivo rechazado | No | `DEVICE_REJECTED` | registrar otro según procedimiento | intento rechazado |
| Dispositivo revocado/robado | No | `DEVICE_REVOKED` | contingencia y reemplazo | revocador, fecha y motivo |
| Dispositivo de otro empleado | No | `DEVICE_OWNERSHIP_MISMATCH` | usar equipo propio | intento rechazado |
| Wi‑Fi ausente/incorrecto en Android requerido | No | `WIFI_REQUIRED` / `INVALID_WIFI` | conectarse a red autorizada | BSSID del intento |
| Wi‑Fi requerido desde Safari/Web | No | `WIFI_UNAVAILABLE_ON_WEB` | cambiar política/canal autorizado | intento rechazado |
| Asignación temporal autorizada vigente | Sí | usa sede temporal | vuelve a permanente al vencer | vigencia, tipo y motivo |
| Dos sedes temporales o permanentes aplicables | No | `MULTIPLE_ACTIVE_ASSIGNMENTS` | admin corrige | no se elige al azar |
| Sin horario / dos horarios | No | `NO_ACTIVE_SCHEDULE` / `MULTIPLE_ACTIVE_SCHEDULES` | admin corrige | intento rechazado |
| Turno nocturno | Sí | salida conserva `workDate` de inicio | Ninguno | una sola jornada |
| Día semanal no laborable | No | `NON_WORKING_DAY` | Ninguno | no genera ausencia |
| Feriado o excepción no laborable | No | `NON_WORKING_DAY` | Ninguno | calendario y sin ausencia falsa |
| Horario especial | Sí | ventana de excepción | Ninguno | calendario por sede/fecha |
| Permiso completo aprobado | Según política | cierre `EXCUSED` | Ninguno | permiso aprobado y auditoría |
| Sin marcaciones al cierre | No | `ABSENT` | justificación | jornada e incidencia automática únicas |
| Entrada sin salida al cierre | No | `INCOMPLETE` | corrección | incidencia `MISSING_EXIT` |
| Justificación aprobada/rechazada | N/A | `EXCUSED` / conserva `ABSENT` | Ninguno | solicitud, revisor y ajuste |
| Empleado A consulta B | No | 403/alcance denegado | Ninguno | auditoría HTTP |

## Precedencia

La UI consulta primero el contexto del día. Al enviar, el backend vuelve a validar identidad, asignación efectiva (temporal sobre permanente), política, calendario/horario, ventana, secuencia y unicidad. Un error nunca crea un punch; los intentos personales rechazados quedan registrados.
