# Sistema Integral de Control de Asistencia

Plataforma web y móvil para administrar empleados, horarios, marcaciones, solicitudes, incidencias y reportes. Las reglas se ejecutan en una API Spring Boot compartida por Angular y Flutter; MySQL conserva los datos y Flyway controla el esquema.

## Componentes

- `backend/`: API REST Java 21, Spring Boot, Spring Security/JWT, JPA y Flyway.
- `frontend-web/`: portal Angular responsive para administración, supervisión y autoservicio.
- `mobile/`: cliente Flutter Android con GPS, cámara QR, dispositivo y BSSID Wi-Fi.
- `database/`: diagramas y material de datos.
- `docs/`: requisitos, arquitectura, contrato, operación y pruebas.
- `tests/`: validación integral y flujo de seguridad contra una instancia desplegada.

## Inicio local con Docker

1. Copiar `.env.example` a `.env` y cambiar secretos antes de usar datos reales.
2. Ejecutar `docker compose up -d --build`.
3. Abrir `http://localhost:4200`.
4. Verificar `http://localhost:8080/actuator/health`.

En desarrollo, el ejemplo permite crear `admin` / `admin` en el primer arranque. Una cuenta ya existente **no** se restablece automáticamente. Para una recuperación deliberada se puede arrancar una sola vez con `APP_BOOTSTRAP_ADMIN_RESET=true` y volverlo inmediatamente a `false`.

## Desarrollo

- Backend: `cd backend; .\mvnw.cmd spring-boot:run`
- Web: `cd frontend-web; npm start`
- Android/emulador: `cd mobile; flutter run --dart-define=API_URL=http://10.0.2.2:8080/api/v1`

Para un teléfono físico, la URL debe ser HTTPS o una dirección accesible desde el dispositivo. El APK incorpora la URL indicada al compilar.

## Validación

```powershell
powershell -ExecutionPolicy Bypass -File tests/run-all.ps1
```

Este comando ejecuta las pruebas del backend, el build y las pruebas Angular, y el análisis y las pruebas Flutter. Consulte [SYSTEM_VALIDATION_REPORT.md](SYSTEM_VALIDATION_REPORT.md) para el resultado final y [ATTENDANCE_FLOW.md](ATTENDANCE_FLOW.md) para el flujo funcional.
