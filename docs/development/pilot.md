# Preparación del piloto

## Prerrequisitos

- Servidor con Docker y Docker Compose.
- Acceso HTTPS mediante proxy inverso o balanceador.
- DNS y certificado válidos.
- Dispositivo Android para instalar el APK.
- Política empresarial definida para horarios, tolerancias y evidencias.

## Configuración

1. Crear `.env` a partir de `.env.example`.
2. Generar contraseñas únicas para MySQL.
3. Generar `JWT_SECRET` aleatorio de al menos 32 bytes y codificarlo en Base64.
4. Configurar el administrador inicial solo durante el primer arranque; después retirar sus variables.
5. Configurar zona horaria, orígenes CORS y puertos.
6. Publicar web y API únicamente detrás de HTTPS.
7. Compilar el APK con la URL HTTPS del piloto mediante `--dart-define=API_URL=...`.
8. Crear `mobile/android/key.properties` desde su ejemplo y apuntarlo a un keystore de release almacenado fuera del repositorio.

## Carga inicial recomendada

1. Sedes con coordenadas verificadas.
2. Áreas y cargos.
3. Horarios y turnos.
4. Usuarios y empleados.
5. Asignaciones organizacionales y de horario.
6. Dispositivos, si la validación estará activa.

## Activación gradual de seguridad

1. Autenticación y hora del servidor.
2. Dispositivo registrado.
3. Geocerca con radio conservador y observación de intentos.
4. QR dinámico.
5. Ajuste de políticas con evidencia del piloto.

## Validaciones implementadas

- **Dispositivo:** la APK genera un identificador criptográficamente aleatorio, lo almacena de forma segura y el administrador debe aprobarlo. No es IMEI y cambia si se eliminan los datos de la aplicación.
- **GPS/geocerca:** Android obtiene coordenadas de alta precisión y el backend calcula la distancia a la sede.
- **QR:** administración genera un QR visual temporal; cada QR nuevo invalida el anterior de la sede.
- **Wi‑Fi:** Android envía el BSSID del punto de acceso y el backend lo compara con el BSSID autorizado de la sede.
- **Hora:** siempre proviene del servidor.
- **Asignaciones obligatorias:** el empleado necesita una sede y un horario activos para la fecha de la marcación.
- **Día laborable:** el horario debe contener un turno para el día actual.
- **Ventana de entrada:** por defecto abre 60 minutos antes y cierra 120 minutos después del inicio del turno.
- **Ventana de salida:** abre al inicio del turno y cierra 180 minutos después del final.
- **Secuencia:** se exige entrada antes de salida y solo se admite una entrada y una salida por jornada.
- **Auditoría:** intentos aceptados y rechazados registran causa, canal, ubicación, dispositivo, IP y BSSID.

La web admite dispositivo del navegador, GPS y código QR pegado manualmente. Los navegadores no permiten leer el BSSID de Wi‑Fi; esa política debe probarse con la APK.

## Biometría facial

No se presenta una captura de selfie como reconocimiento facial. Una implementación real necesita consentimiento, cifrado de plantillas biométricas, comparación de identidad, prueba de vida y política de conservación. Para el piloto se usan controles combinados de usuario, dispositivo, geocerca, Wi‑Fi y QR.

## Orden de configuración en la web

1. Crear usuario `EMPLOYEE`.
2. Crear empleado vinculado mediante `userId`.
3. Crear sede con coordenadas, área, cargo y horario.
4. Abrir **Asignaciones** y relacionar organización y horario.
5. El empleado registra su dispositivo desde la APK.
6. El administrador lo aprueba en **Seguridad**.
7. Configurar y activar controles de la sede gradualmente.
8. Generar QR, escanearlo y marcar.

No se recomienda activar todos los controles el primer día.

## Operación

- Revisar salud de API y base de datos.
- Respaldar MySQL diariamente y probar restauración.
- Revisar intentos rechazados y auditoría.
- Mantener sincronización horaria del servidor.
- Definir responsables para solicitudes e incidencias.
- Conservar un procedimiento alternativo de marcación ante caída del servicio.

## Criterios de salida del piloto

- Usuarios piloto pueden autenticar y marcar consistentemente.
- Cálculos de asistencia coinciden con casos verificados por RR. HH.
- Flujos de aprobación tienen responsables y tiempos acordados.
- No existen errores críticos abiertos.
- Respaldo y restauración están probados.
- Seguridad, privacidad y conservación de datos fueron aprobadas por la organización.
