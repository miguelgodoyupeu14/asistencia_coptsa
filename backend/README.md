# Backend

API REST del Sistema Integral de Control de Asistencia.

## Requisitos

- JDK 21 o compatible.
- Maven 3.6.3 o superior, o Maven Wrapper.
- MySQL 8.4 para ejecución local integrada.

## Configuración local

1. Copiar `.env.example` como `.env` en la raíz del repositorio.
2. Reemplazar todos los valores de ejemplo.
3. Iniciar MySQL con `docker compose up -d mysql` desde la raíz.
4. Exportar las variables del archivo `.env` al entorno usado para ejecutar Java.
5. Ejecutar `./mvnw spring-boot:run` desde `backend`.

La aplicación usa UTC para persistencia y serialización. `APP_TIME_ZONE` representa la zona operativa inicial y no cambia el almacenamiento de instantes.

## Pruebas

Ejecutar `./mvnw test`. Las pruebas usan una base H2 aislada y no requieren MySQL.
