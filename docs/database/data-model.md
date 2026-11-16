# Modelo de datos inicial

## Propósito y alcance

Este documento define el modelo conceptual y lógico inicial del sistema. Sirve como base para discutir entidades, responsabilidades y relaciones antes de crear tablas o migraciones.

No define todavía tipos SQL, tamaños de columnas, índices, reglas de borrado, catálogos definitivos ni fórmulas de asistencia.

## Criterios del modelo

- Separar la identidad de acceso (`Usuario`) de la relación laboral (`Empleado`).
- Conservar vigencias históricas en asignaciones que pueden cambiar con el tiempo.
- Conservar cada marcación como un evento original y trazable.
- Mantener los resultados calculados de una jornada separados de sus marcaciones.
- Tratar reportes como consultas o proyecciones, no como entidades operativas iniciales.
- Permitir incorporar controles de dispositivo, ubicación, IP y QR sin modificar el núcleo de asistencia.

Los nombres son lógicos y podrán ajustarse al definir el esquema físico.

## Dominios y entidades

### Identidad y acceso

#### Usuario

Identidad utilizada para autenticarse en el sistema.

Atributos conceptuales: identificador, nombre de acceso o correo, credencial protegida, estado, último acceso y fechas de creación/actualización.

#### Rol

Agrupa capacidades de acceso. Los roles iniciales previstos son administrador, supervisor y empleado, pero su representación definitiva se decidirá en la Fase 5.

Atributos conceptuales: identificador, código, nombre, descripción y estado.

#### UsuarioRol

Relaciona usuarios y roles, permitiendo que un usuario tenga más de un rol si las reglas futuras lo requieren.

### Organización

#### Sede

Representa una ubicación organizacional donde pueden trabajar empleados.

Atributos conceptuales: identificador, código, nombre, dirección, zona horaria, estado y datos opcionales de ubicación.

La zona horaria se asocia conceptualmente a la sede, pero su política definitiva sigue pendiente.

#### Área

Unidad organizacional a la que puede asignarse un empleado.

Atributos conceptuales: identificador, código, nombre, descripción y estado.

#### Cargo

Función o posición laboral de un empleado.

Atributos conceptuales: identificador, código, nombre, descripción y estado.

#### Empleado

Representa a una persona vinculada laboralmente con la organización.

Atributos conceptuales: identificador, código de empleado, tipo y número de documento, nombres, apellidos, datos de contacto, fecha de ingreso, fecha de cese y estado.

Un empleado puede vincularse con cero o un usuario. Esto permite registrar empleados antes de habilitarles acceso.

#### AsignacionEmpleado

Conserva la asignación de sede, área y cargo durante un período de vigencia.

Atributos conceptuales: identificador, empleado, sede, área, cargo, inicio de vigencia, fin de vigencia y estado.

La jerarquía de supervisión no se incluye todavía porque falta definir si depende del área, del cargo o de una relación directa.

### Planificación laboral

#### Horario

Plantilla reutilizable que identifica una planificación laboral.

Atributos conceptuales: identificador, código, nombre, descripción y estado.

#### Turno

Define una franja de trabajo prevista dentro de un horario.

Atributos conceptuales: identificador, horario, día aplicable, hora de inicio, hora de fin, indicador de cruce de medianoche y descanso previsto.

Las tolerancias, turnos partidos, descansos múltiples y días especiales quedan pendientes de definición.

#### AsignacionHorario

Relaciona un empleado con un horario durante un período de vigencia.

Atributos conceptuales: identificador, empleado, horario, inicio de vigencia, fin de vigencia y estado.

### Asistencia

#### JornadaAsistencia

Representa el resultado diario de asistencia de un empleado para una fecha laboral.

Atributos conceptuales: identificador, empleado, fecha laboral, horario o turno aplicado, primera entrada, última salida, minutos trabajados, minutos de tardanza, minutos de salida anticipada, estado y versión de cálculo.

Los valores calculados no sustituyen las marcaciones originales. Su fórmula se definirá en las fases de reglas de asistencia.

#### Marcacion

Evento original de entrada o salida recibido por el sistema.

Atributos conceptuales: identificador, empleado, instante recibido por el servidor, tipo declarado, canal de origen, IP, dispositivo, ubicación, referencia QR y datos técnicos de validación.

Los atributos de seguridad avanzada serán opcionales hasta que sus fases sean implementadas. Una corrección no debe sobrescribir silenciosamente la marcación original.

### Incidencias y solicitudes

#### Incidencia

Registra una anomalía o situación relacionada con una jornada o una marcación.

Atributos conceptuales: identificador, empleado, jornada opcional, tipo, descripción, estado, origen y fechas relevantes.

#### Justificacion

Explicación y evidencia presentada para una incidencia.

Atributos conceptuales: identificador, incidencia, solicitante, motivo, evidencia, estado de revisión, revisor, observación y fechas de envío/resolución.

La forma de almacenar archivos se decidirá antes de implementar evidencias.

#### Permiso

Solicitud de ausencia o modificación autorizada de la jornada durante un intervalo.

Atributos conceptuales: identificador, empleado, tipo, inicio, fin, motivo, estado, solicitante, revisor y fechas de envío/resolución.

#### SolicitudCorreccion

Petición para revisar una jornada o marcación sin alterar el registro original.

Atributos conceptuales: identificador, empleado, jornada o marcación relacionada, cambio solicitado, motivo, estado, solicitante, revisor y fechas de envío/resolución.

Los flujos de aprobación y sus estados definitivos se definirán en las fases funcionales correspondientes.

### Auditoría

#### RegistroAuditoria

Traza una acción relevante realizada sobre el sistema.

Atributos conceptuales: identificador, instante, usuario opcional, acción, tipo e identificador del recurso, resultado, IP, datos técnicos y resumen de cambios.

Su contenido, retención y protección se concretarán en la fase de auditoría.

## Relaciones principales

- Un `Usuario` puede tener varios `Rol` mediante `UsuarioRol`.
- Un `Empleado` puede vincularse con un `Usuario`.
- Un `Empleado` puede tener varias `AsignacionEmpleado`, sin solapamientos según las reglas futuras.
- Una asignación organizacional referencia una `Sede`, un `Área` y un `Cargo`.
- Un `Horario` contiene uno o más `Turno`.
- Un `Empleado` puede tener varias `AsignacionHorario`, conservando su historial.
- Un `Empleado` genera `Marcacion` y posee `JornadaAsistencia`.
- Una jornada puede agrupar varias marcaciones.
- Las incidencias y solicitudes pertenecen a un empleado y pueden referenciar una jornada o marcación.
- Un usuario puede originar acciones registradas en `RegistroAuditoria`.

El diagrama resumido se encuentra en `database/diagrams/attendance-domain.mmd`.

## Restricciones lógicas propuestas

- Los códigos de sede, área, cargo, empleado y horario deben ser únicos dentro del alcance que se acuerde.
- El documento de identidad debe evitar duplicados conforme a su tipo y jurisdicción.
- Una asignación debe terminar después de comenzar.
- No debería existir más de una jornada por empleado y fecha laboral, salvo que se confirme un caso de negocio distinto.
- Las marcaciones deben conservar el instante del servidor y no eliminarse como parte de una corrección normal.
- Las fechas de resolución solo aplican a solicitudes revisadas.

Estas restricciones son propuestas y requieren validación antes de convertirse en restricciones físicas.

## Datos derivados

Tardanza, salida anticipada, ausencia y horas trabajadas se obtendrán de las marcaciones, planificación aplicable, permisos y reglas vigentes. No se consideran simples datos ingresados por el cliente.

El diseño conserva una versión o referencia de cálculo en la jornada para facilitar futuras recalculaciones y auditoría. La estrategia exacta se definirá al implementar estas reglas.

## Decisiones pendientes antes del esquema físico

- Estrategia de identificadores y convención física de nombres.
- Zona horaria operativa y definición de fecha laboral para turnos nocturnos.
- Modelo exacto de supervisión y estructura organizacional.
- Posibilidad de múltiples asignaciones simultáneas por empleado.
- Representación de turnos partidos, rotativos y excepciones de calendario.
- Catálogos y estados de incidencias, permisos, justificaciones y correcciones.
- Flujo de aprobación simple o multinivel.
- Almacenamiento y retención de archivos de evidencia.
- Política de eliminación lógica, anonimización y conservación.
- Fuente de feriados y días no laborables.
- Requisitos legales aplicables.

No deben crearse migraciones definitivas hasta revisar estos puntos o documentar supuestos aceptados.
