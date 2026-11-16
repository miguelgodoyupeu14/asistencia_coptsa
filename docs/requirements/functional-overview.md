# Especificación funcional actual del Sistema de Asistencia

**Fecha de revisión:** 14 de octubre de 2026  
**Alcance:** comportamiento comprobado en el código, la interfaz, la API y las pruebas actuales.  
**Objetivo:** servir como base para validar el flujo de negocio antes de seguir agregando funcionalidades.

## 1. Propósito del sistema

El sistema administra empleados, su ubicación organizacional, sus horarios y sus registros de entrada y salida. Centraliza las reglas en el backend para que la web y la aplicación móvil utilicen las mismas validaciones.

Los clientes disponibles son:

- **Web Angular:** administración, supervisión y autoservicio del empleado. En iPhone funciona desde Safari mediante HTTPS.
- **Aplicación Flutter Android:** autoservicio del empleado con GPS, cámara para QR y lectura del BSSID del Wi‑Fi.
- **API Spring Boot:** autenticación, autorización, reglas de asistencia y auditoría.
- **MySQL:** persistencia de usuarios, empleados, horarios, marcaciones, solicitudes y auditorías.

El acceso HTTPS actual mediante Tailscale es apropiado para pruebas privadas. Todavía no representa un despliegue público de producción.

## 2. Actores y responsabilidades

### 2.1 Administrador (`ADMIN`)

Puede:

- Crear usuarios y asignarles roles.
- Activar o desactivar usuarios y restablecer contraseñas mediante la API.
- Crear y actualizar empleados.
- Crear sedes, áreas y cargos.
- Crear horarios con turnos por día.
- Asignar al empleado una sede, área y cargo con vigencia.
- Asignar al empleado un horario con vigencia.
- Configurar la seguridad de asistencia por sede.
- Consultar, aprobar o rechazar dispositivos registrados.
- Generar códigos QR temporales.
- Consultar intentos de marcación y auditoría general.
- Consultar y resolver solicitudes de empleados.
- Registrar y cerrar incidencias.
- Consultar reportes de asistencia.
- Ejecutar mediante la API el procesamiento de ausencias de fechas pasadas.

### 2.2 Supervisor (`SUPERVISOR`)

Puede:

- Consultar empleados.
- Consultar horarios.
- Consultar, aprobar o rechazar solicitudes.
- Crear, consultar y cerrar incidencias.
- Consultar reportes de asistencia.

No puede:

- Administrar usuarios o contraseñas.
- Crear empleados.
- Crear sedes, áreas o cargos.
- Configurar geocerca, QR, dispositivos o Wi‑Fi.
- Consultar la auditoría administrativa.

### 2.3 Empleado (`EMPLOYEE`)

Puede:

- Consultar su perfil y horario vigente.
- Registrar entrada y salida.
- Consultar su historial de asistencia.
- Registrar su navegador o dispositivo para aprobación.
- Escanear un QR desde la cámara.
- Crear solicitudes de justificación, permiso o corrección.
- Consultar el estado de sus solicitudes.
- Consultar sus incidencias.

No puede consultar ni modificar datos de otros empleados.

## 3. Preparación obligatoria de un empleado

Para que un empleado pueda marcar asistencia, el administrador debe completar el siguiente orden:

1. **Crear el usuario** con el rol `EMPLOYEE`.
2. **Crear el empleado** y vincularlo al identificador del usuario.
3. **Crear la estructura organizacional:** sede, área y cargo.
4. **Crear el horario** con un turno para cada día laborable.
5. **Asignar organización:** empleado + sede + área + cargo + fecha de inicio.
6. **Asignar horario:** empleado + horario + fecha de inicio.
7. **Configurar la política de seguridad** de la sede.
8. Si se exige dispositivo aprobado, el empleado debe registrarlo y el administrador aprobarlo.

Sin usuario vinculado, sede vigente o horario vigente, la marcación se rechaza.

El sistema impide crear asignaciones organizacionales u horarias superpuestas. Durante una marcación también rechaza el caso en que encuentre más de una asignación vigente.

## 4. Flujo diario de asistencia

### 4.1 Entrada

1. El empleado inicia sesión.
2. Abre **Mi asistencia**.
3. Si la sede exige QR, lo escanea con la cámara.
4. El cliente obtiene los datos disponibles:
   - identificador del dispositivo;
   - latitud y longitud;
   - código QR;
   - BSSID del Wi‑Fi, solamente cuando la plataforma lo permite.
5. El empleado pulsa **Registrar entrada**.
6. El backend usa su propia hora oficial en `America/Lima` y ejecuta todas las validaciones.
7. Si se acepta, crea la jornada del día y registra la marcación.

### 4.2 Salida

1. El empleado repite las validaciones de seguridad requeridas.
2. Pulsa **Registrar salida**.
3. El sistema exige una entrada previa del mismo día.
4. Si se acepta, completa la jornada y calcula:
   - minutos trabajados;
   - minutos de tardanza;
   - minutos de salida anticipada.

Los minutos trabajados son la diferencia entre entrada y salida menos el descanso configurado en el turno.

## 5. Orden exacto de validación de una marcación

El backend evalúa las reglas en este orden:

1. La cuenta debe estar vinculada a un empleado.
2. El empleado debe estar activo.
3. Debe existir exactamente una asignación vigente de sede.
4. Si se exige dispositivo, debe estar aprobado.
5. Si se exige geocerca, el cliente debe proporcionar GPS.
6. La sede debe tener coordenadas y radio configurados.
7. La distancia debe estar dentro del radio permitido.
8. Si se exige QR, el código debe existir, estar activo y no haber vencido.
9. Si se exige Wi‑Fi, el BSSID debe coincidir con el autorizado.
10. Debe existir exactamente un horario vigente.
11. El día actual debe estar configurado como laborable.
12. Debe cumplirse la ventana horaria correspondiente.
13. Debe cumplirse la secuencia de entrada y salida.

El primer incumplimiento detiene el proceso y queda registrado como intento rechazado.

## 6. Reglas horarias vigentes

La hora utilizada es la del servidor; cambiar la hora del teléfono no altera el resultado.

Configuración actual:

- La entrada abre **60 minutos antes** del inicio del turno.
- La entrada cierra **120 minutos después** del inicio del turno.
- La salida se permite desde el inicio del turno.
- La salida cierra **180 minutos después** del final del turno.
- La tolerancia para calcular tardanza es actualmente **0 minutos**.
- La tolerancia para calcular salida anticipada es actualmente **0 minutos**.

Ejemplo para un turno de `08:00–17:00`:

- Entrada permitida: `07:00–10:00`.
- Una entrada después de `08:00` genera minutos de tardanza.
- Salida permitida: `08:00–20:00`, siempre que exista una entrada.
- Una salida antes de `17:00` genera minutos de salida anticipada.

También se soportan turnos que terminan después de medianoche.

Reglas de consistencia:

- Máximo una entrada por empleado y fecha.
- Máximo una salida por empleado y fecha.
- No se permite salida sin entrada.
- No se permite salida anterior a la entrada.
- No se permite marcar en un día no laborable.

Estados de jornada existentes: `OPEN`, `COMPLETED`, `INCOMPLETE`, `ABSENT` y `EXCUSED`.

## 7. Métodos de seguridad de asistencia

Cada sede puede activar o desactivar los siguientes controles de manera independiente.

### 7.1 Geocerca GPS

- La sede tiene latitud, longitud y radio en metros.
- El backend calcula la distancia entre el dispositivo y la sede.
- Si no llega una ubicación, responde `GPS_REQUIRED`.
- Si la ubicación supera el radio, responde `OUTSIDE_GEOFENCE`.
- UPeU está configurada actualmente con un radio de 300 metros.

Limitación: desde una web no existe protección fuerte contra GPS manipulado. Para producción se recomienda una aplicación nativa con detección de ubicaciones simuladas y verificación de integridad.

### 7.2 Dispositivo aprobado

- El cliente genera un identificador persistente.
- El empleado solicita registrar el dispositivo.
- El registro queda `PENDING`.
- Un administrador lo cambia a `APPROVED` o `REJECTED`.
- Si la sede exige este control, solo un identificador aprobado puede marcar.

Limitación web: el identificador se guarda en el almacenamiento del navegador; no equivale a una identidad criptográfica del hardware.

### 7.3 QR dinámico

- El administrador genera un QR para una sede.
- Su vigencia puede ser de 30 a 3600 segundos.
- Generar uno nuevo desactiva los QR activos anteriores de esa sede.
- El empleado lo escanea con la cámara desde la web HTTPS o la aplicación móvil.
- El backend almacena y compara un hash, no el código visible en texto plano.

El fallo de comparación entre UTC y hora de Lima fue corregido y el flujo válido/inválido fue probado contra el HTTPS real.

### 7.4 Wi‑Fi autorizado

- La política guarda el BSSID del punto de acceso, no solamente el nombre de la red.
- Se normalizan formatos con guiones o dos puntos y diferencias entre mayúsculas y minúsculas.
- Un BSSID ausente o diferente se rechaza.

Limitación: Safari y los navegadores web no permiten leer el BSSID. Este control está destinado a la aplicación Android.

### 7.5 Autenticación, roles y transporte

- Login mediante usuario y contraseña.
- Contraseñas almacenadas con un codificador seguro de Spring Security.
- Token JWT con duración actual de 15 minutos.
- Autorización en backend y protección adicional de rutas en frontend.
- HTTPS privado mediante Tailscale durante las pruebas.
- CORS limitado a orígenes configurados.

## 8. Solicitudes

Tipos disponibles:

- Justificación (`JUSTIFICATION`).
- Permiso (`PERMISSION`).
- Corrección (`CORRECTION`).

Flujo implementado:

1. El empleado crea una solicitud con tipo y motivo.
2. Queda en estado `SUBMITTED`.
3. Administrador o supervisor consulta las pendientes.
4. Puede aprobarla o rechazarla.
5. Se registra quién resolvió, el comentario y la fecha.
6. El empleado ve el resultado en su historial.

La API admite además fechas solicitadas, cambio pedido y referencia de evidencia, pero la interfaz web actual solamente captura tipo y motivo.

Al aprobar, el backend aplica el efecto dentro de la misma transacción: una justificación cambia una ausencia a `EXCUSED`; una corrección crea la marcación propuesta y recalcula la jornada; un permiso queda programado y, si cubre todo el turno, evita que el cierre lo clasifique como ausencia. Cada ajuste conserva estado anterior/posterior y revisor.

## 9. Incidencias

Administrador y supervisor pueden:

- Crear una incidencia manual para un empleado.
- Consultar incidencias por estado.
- Cerrar una incidencia.

El empleado puede consultar sus propias incidencias.

El cierre automático genera incidencias idempotentes para ausencia y salida faltante. Las tardanzas y salidas anticipadas se reflejan en la jornada y el reporte, pero no abren una incidencia automáticamente.

## 10. Ausencias

Existe una operación administrativa que procesa una fecha pasada:

- Busca empleados con horario laborable ese día.
- Si no tienen jornada registrada, crea una jornada con estado `ABSENT`.
- No procesa el día actual ni fechas futuras.

El procesamiento se ejecuta cada 15 minutos, revisa los últimos siete días y espera el cierre real de la ventana, incluidos los turnos nocturnos. También puede invocarse manualmente mediante la API administrativa.

## 11. Reportes

Administrador y supervisor pueden consultar un resumen por rango de fechas con:

- empleado;
- cantidad de días registrados;
- ausencias;
- minutos de tardanza;
- minutos de salida anticipada;
- minutos trabajados.

Los reportes dependen de que las jornadas y ausencias hayan sido procesadas correctamente.

## 12. Auditoría

El sistema conserva dos niveles principales:

- Auditoría general de solicitudes HTTP y acciones del sistema.
- Auditoría específica de intentos de marcación, aceptados o rechazados.

Un intento de asistencia puede registrar:

- empleado;
- resultado;
- motivo;
- fecha y hora;
- canal web, móvil o prueba;
- IP;
- hash del dispositivo;
- latitud y longitud;
- BSSID del Wi‑Fi.

Solo el administrador accede a estas pantallas.

## 13. Configuración actual del escenario UPeU

- Sede: Universidad Peruana Unión, campus Lima.
- Coordenadas: `-11.989917, -76.841306`.
- Radio: 300 metros.
- Zona horaria: `America/Lima`.
- Empleado de prueba: `empleado.iphone`.
- Horario del escenario: lunes a viernes, `08:00–17:00`, descanso de 60 minutos.
- Geocerca: activada.
- Dispositivo obligatorio: desactivado.
- QR obligatorio: desactivado.
- Wi‑Fi obligatorio: desactivado.

Solo la geocerca está activa para que las pruebas actuales tengan una causa de aceptación o rechazo claramente identificable.

## 14. Funcionalidades no implementadas o incompletas

- Reconocimiento facial y prueba de vida.
- Aplicación nativa para iPhone; actualmente se utiliza Safari HTTPS.
- Detección robusta de GPS falso o dispositivo comprometido.
- Lectura de BSSID desde Safari.
- Carga real de archivos o evidencias; existe solamente una referencia textual en la API.
- Generación automática de incidencias por tardanza o salida anticipada (ausencia y salida faltante sí están cubiertas).
- Notificaciones push, correo o mensajería.
- Recuperación de contraseña por parte del usuario.
- Flujo de aprobación de solicitudes en múltiples niveles.
- Modo quiosco en el que un dispositivo fijo escanea la credencial del empleado.
- Despliegue público productivo con dominio propio, certificados, copias de seguridad y monitoreo.

## 15. Decisiones funcionales que deben confirmarse

Antes de considerar definitivo el producto conviene decidir:

1. ¿La asistencia exigirá geocerca, QR y dispositivo simultáneamente o se configurarán combinaciones por sede?
2. ¿El Wi‑Fi será obligatorio o solamente una señal adicional de confianza?
3. ¿Cuánto antes y después del turno se permitirá marcar?
4. ¿Se debe impedir una salida demasiado temprana o solamente registrarla como salida anticipada?
5. ¿Qué debe ocurrir cuando se aprueba una justificación?
6. ¿Quién puede corregir una entrada o salida y qué doble aprobación requiere?
7. ¿Las ausencias se generan automáticamente al terminar cada día?
8. ¿Una tardanza debe producir automáticamente una incidencia?
9. ¿Se permitirán empleados con más de una sede o turno el mismo día?
10. ¿Se necesita una aplicación nativa iOS además de Android?
11. ¿Se requiere reconocimiento facial o bastan dispositivo, GPS y QR?
12. ¿Cuál será el método principal: marcación personal, QR mostrado por la empresa o modo quiosco?

## 16. Estado de validación

Resultados más recientes:

- 30 pruebas automatizadas del backend aprobadas.
- 3 pruebas automatizadas del frontend aprobadas.
- Análisis estático y prueba Flutter aprobados.
- 26 comprobaciones reales por HTTPS aprobadas.
- Web, API y MySQL activos en Docker.

Las comprobaciones HTTPS incluyeron roles, geocerca con GPS ausente/dentro/fuera, dispositivo pendiente/aprobado, QR inválido/válido, BSSID ausente/válido, horario, salida sin entrada y aprobación de solicitudes.

La cámara y el GPS físico solo pueden cerrarse como prueba de hardware utilizando un dispositivo real; el procesamiento backend de sus datos ya fue comprobado.
