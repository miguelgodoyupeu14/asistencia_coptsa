# Escenarios de prueba

Leyenda: `AUTO_PASS` ejecutado automáticamente; `BUILD_PASS` compilación/análisis; `PENDING_PHYSICAL_DEVICE_TEST` requiere sensor o equipo real; `MANUAL_PENDING` requiere aceptación humana/operativa.

## Resultado automatizado actual

| Capa | Antes | Ahora | Resultado |
|---|---:|---:|---|
| Spring Boot | 30 | 51 | 51 PASS, 0 FAIL |
| Angular | 6 | 6 | 6 PASS, build producción PASS |
| Flutter | 1 | 1 | 1 PASS; `flutter analyze --no-pub` sin incidencias |

## Casos de backend

| Grupo | Casos cubiertos | Evidencia | Estado |
|---|---|---|---|
| Ventana de entrada | 06:59, 07:00, 07:59, 08:00, 08:01, 09:59, 10:00, 10:01 | `AttendanceRuleValidatorTests` parametrizado | AUTO_PASS |
| Ventana de salida | 07:59, 08:00, 16:59, 17:00, 20:00, 20:01 | test parametrizado | AUTO_PASS |
| Geocerca | centro, 50 %, radio−1, radio, radio+1 | test parametrizado de Haversine; límite inclusivo | AUTO_PASS |
| QR | válido, desconocido, vencido, inactivo, otra sede, uso por varios empleados | integración de seguridad | AUTO_PASS |
| Wi‑Fi | formato equivalente, ausente, distinto | unitario + servicio | AUTO_PASS |
| Jornada | puntual, tardía, salida anticipada, salida sin entrada, duplicados | dominio/validator/integración | AUTO_PASS |
| Concurrencia | dos entradas simultáneas | unique jornada/tipo | AUTO_PASS |
| Turno nocturno | salida al día siguiente y cierre | validator + resolución | AUTO_PASS |
| Ausencias | `ABSENT`, idempotencia, permiso completo `EXCUSED`, jornada incompleta | integración | AUTO_PASS |
| Calendario | feriado no produce ausencia falsa | integración | AUTO_PASS |
| Solicitudes | permiso futuro, corrección, aprobación/rechazo, alcance | integración API/servicio | AUTO_PASS |
| Contingencia | entrada admin, canal manual, actor, motivo, sede y evento | `AttendanceContingencyIntegrationTests` | AUTO_PASS |
| Seguridad HTTP | login inválido, anónimo, ruta inexistente | MockMvc | AUTO_PASS |

## Casos funcionales pendientes de automatización específica

- Cada estado de dispositivo (`PENDING`, `REJECTED`, `REVOKED`, ownership) tiene validación backend implementada; faltan casos parametrizados dedicados para todos en una sola tabla.
- Corrección que crea una jornada inexistente está implementada; falta un test de integración dedicado con sede y horario histórico.
- Asignación temporal sobre permanente está implementada; falta prueba dedicada de precedencia y de dos temporales inválidas.
- `SPECIAL_SCHEDULE` se resuelve desde calendario; falta prueba dedicada de jornada y métricas con turno especial.
- La generación concurrente de dos QR se apoya en transacción de aplicación, pero no tiene lock por sede; se documenta como riesgo residual.
- No existe idempotency key HTTP; la integridad está protegida por constraints y conflicto funcional.

## Pruebas físicas y manuales

| Escenario | Procedimiento | Estado |
|---|---|---|
| Safari/iPhone HTTPS | abrir portal HTTPS, permitir GPS/cámara, escanear QR y marcar | PENDING_PHYSICAL_DEVICE_TEST |
| GPS dentro/fuera UPeU | tomar lecturas reales con precisión y comparar radio | PENDING_PHYSICAL_DEVICE_TEST |
| Cámara QR web | QR visible en laptop, escaneo desde iPhone | PENDING_PHYSICAL_DEVICE_TEST |
| Android APK | instalar, aprobar dispositivo, GPS + QR + BSSID | PENDING_PHYSICAL_DEVICE_TEST |
| BSSID real | red autorizada Android y red distinta | PENDING_PHYSICAL_DEVICE_TEST |
| Contingencia visual | supervisor selecciona empleado/foto, motivo y marca | MANUAL_PENDING |
| Caída completa | detener API/BD, restaurar y aprobar corrección `SYSTEM_OUTAGE` | MANUAL_PENDING |
| Reporte posterior | corregir/justificar y comparar reporte | MANUAL_PENDING |

Nunca se clasifica un caso físico como aprobado solo porque el payload equivalente pasó por API.
