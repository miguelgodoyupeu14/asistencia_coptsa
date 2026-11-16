# Estrategia de pruebas

## Validación automatizada

Desde la raíz:

```powershell
powershell -ExecutionPolicy Bypass -File tests/run-all.ps1
```

Valida:

- Backend: las diez migraciones Flyway sobre H2 en modo MySQL, contexto Spring, autorización HTTP, reglas horarias, concurrencia, turnos nocturnos, ausencias, permisos, correcciones y controles de asistencia.
- Web: build de producción y pruebas de componentes con Vitest.
- Móvil: análisis estático y pruebas de widgets.

## Matriz mínima

| Área | Casos obligatorios |
|---|---|
| Autenticación | credenciales válidas e inválidas, JWT ausente, usuario desactivado |
| Autorización | ADMIN global, SUPERVISOR limitado por sede/área, EMPLOYEE solo datos propios |
| Preparación | usuario, empleado, sede, área, cargo, horario y ambas asignaciones vigentes |
| Marcación | entrada/salida, duplicados, salida sin entrada, día no laborable, ventanas y turno nocturno |
| Seguridad | dispositivo pendiente/aprobado, GPS ausente/fuera/dentro, QR inválido/vencido/reemplazado/válido, BSSID ausente/válido |
| Solicitudes | rechazo comentado, justificación, corrección con recálculo y permiso futuro |
| Cierre | ausencia automática, permiso completo como `EXCUSED`, salida faltante como `INCOMPLETE`, idempotencia |
| Reportes | rango, empleado, sede, área, métricas y alcance del supervisor |
| Auditoría | resultado, motivo, sede, tipo, canal, correlación y evidencia técnica |

## Prueba física

Los tests automatizados verifican el procesamiento backend, pero no pueden certificar sensores físicos. En iPhone/Safari deben comprobarse cámara y GPS bajo HTTPS. Safari no expone el BSSID; ese control se prueba con la aplicación Android. Cada control debe activarse individualmente antes de combinar políticas.

## Docker y API real

```powershell
docker compose up -d --build
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

La base debe registrar Flyway V1–V10. La web debe responder en el puerto 4200 y `/api` debe ser encaminado por Nginx. Para pruebas destructivas del escenario de seguridad use `tests/live-security-flow.ps1` solamente sobre datos de piloto.

## APK de piloto

```powershell
cd mobile
flutter build apk --debug --dart-define=API_URL=https://servidor.example/api/v1
```

El APK debug sirve para instalación controlada. Una publicación requiere keystore, firma release, dominio/certificado definitivos y revisión de privacidad.
