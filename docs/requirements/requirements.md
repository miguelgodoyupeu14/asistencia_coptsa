# Requisitos iniciales

## Objetivo

Construir un sistema multiplataforma para registrar, controlar y supervisar la asistencia de empleados de una empresa.

Este documento recoge únicamente requisitos confirmados. Las reglas específicas se definirán en las fases correspondientes.

## Actores

### Administrador

- Configura el sistema.
- Gestiona empleados, horarios, áreas, cargos y sedes.
- Consulta reportes y auditoría.
- Supervisa el funcionamiento general.

### Supervisor

- Consulta la asistencia, tardanzas e inasistencias del personal a su cargo.
- Revisa incidencias.
- Podrá participar en procesos de aprobación cuando se definan sus reglas.

### Empleado

- Registra su entrada y salida.
- Consulta su horario, historial e incidencias.
- Podrá presentar justificaciones y solicitudes de corrección.

## Capacidades previstas

- Gestión de empleados, usuarios y roles.
- Gestión de áreas, cargos y sedes.
- Gestión de horarios y turnos.
- Registro de entradas y salidas.
- Control de tardanzas, inasistencias, salidas anticipadas y horas trabajadas.
- Gestión de incidencias, justificaciones, permisos y solicitudes de corrección.
- Auditoría y reportes.

Estas capacidades representan el alcance general, no funcionalidades aprobadas para implementación inmediata.

## Alcance por plataforma

### Aplicación web

Estará orientada principalmente a administración y supervisión. Será responsive y deberá poder utilizarse inicialmente desde Safari en iPhone.

### Aplicación móvil

Estará orientada inicialmente al empleado y se distribuirá como APK para Android. Permitirá posteriormente consultar información personal y horarios, registrar asistencia, revisar historial y gestionar incidencias o justificaciones.

## Requisitos técnicos confirmados

- Angular para la aplicación web.
- Flutter para la aplicación móvil.
- Java y Spring Boot para la API REST.
- MySQL como base de datos.
- JWT y Spring Security para la autenticación futura.
- Una misma API para Angular y Flutter.
- Reglas de negocio centralizadas en el backend.
- Secretos y credenciales suministrados mediante variables de entorno.

## Seguridad prevista para fases posteriores

La arquitectura debe permitir incorporar progresivamente:

- Autenticación y JWT.
- Registro o identificación del dispositivo.
- Validación de IP.
- Uso de la hora del servidor.
- Geolocalización y geocerca.
- QR dinámico.
- Auditoría de intentos.

Estas medidas no forman parte de la implementación actual.

## Atributos de calidad

- Organización modular y mantenible.
- Código legible y sin duplicación innecesaria.
- Diseño escalable sin sobreingeniería.
- Aplicación web responsive.
- Separación entre presentación, lógica de negocio y persistencia.
- Ausencia de credenciales o secretos en el repositorio.

## Pendientes de definición

- Reglas laborales y zona horaria aplicable a los cálculos de asistencia.
- Jerarquía y alcance exacto de supervisión.
- Flujos y responsables de aprobación.
- Políticas para horarios, turnos, tolerancias y días no laborables.
- Reglas para correcciones, permisos, incidencias y justificaciones.
- Contenido, filtros y formatos de los reportes.
- Política de conservación de datos y auditoría.
- Requisitos exactos de compatibilidad móvil y web.

Estos puntos deben resolverse antes de implementar las funciones a las que afectan.
