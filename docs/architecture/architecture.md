# Arquitectura inicial

## Visión general

El sistema seguirá una arquitectura cliente-servidor. La aplicación web y la aplicación móvil consumirán una única API REST, responsable de aplicar las reglas de negocio y acceder a MySQL.

```text
                    MySQL
                      ↑
                      │
              Spring Boot API REST
                 ↑             ↑
                 │             │
          Angular Web     Flutter Android
```

## Componentes

### Backend

API REST desarrollada con Java y Spring Boot. Será responsable de:

- Autenticación y autorización cuando correspondan sus fases.
- Validación de las solicitudes.
- Aplicación de reglas de negocio.
- Persistencia y consulta de datos.
- Auditoría y generación de información para reportes.

Cuando se inicialice, se organizará aproximadamente en controladores, servicios, repositorios, entidades, DTO, seguridad, configuración y manejo de excepciones. La estructura concreta se definirá en la fase del backend.

### Frontend web

Aplicación Angular responsive para administración y supervisión. No accederá directamente a la base de datos ni duplicará reglas de negocio del backend.

### Aplicación móvil

Aplicación Flutter orientada inicialmente al empleado. Consumirá la misma API que la aplicación web y no accederá directamente a MySQL.

### Base de datos

MySQL almacenará los datos operativos del sistema. Su modelo, restricciones y estrategia de migraciones se definirán durante las fases de datos y configuración.

## Flujo principal

1. El cliente web o móvil envía una solicitud HTTPS a la API.
2. La API autentica, autoriza y valida la solicitud según las funciones implementadas.
3. Los servicios del backend aplican las reglas de negocio.
4. Los repositorios consultan o modifican la base de datos.
5. La API devuelve una respuesta al cliente.

## Principios

- Una única fuente de reglas de negocio en el backend.
- Contratos REST compartidos por los clientes.
- Separación clara de responsabilidades.
- Configuración externa para secretos y valores dependientes del entorno.
- Incorporación gradual de controles de seguridad y auditoría.
- Implementación por fases, evitando dependencias prematuras.

## Límites actuales

Esta arquitectura es conceptual. Aún no se han definido:

- Modelo físico de datos.
- Endpoints ni formatos definitivos de la API.
- Versiones de Java, Spring Boot, Angular, Flutter o MySQL.
- Proveedor o entorno de despliegue.
- Estrategia de migraciones de base de datos.
- Estrategia detallada de autenticación, autorización y renovación de tokens.
- Mecanismos de geolocalización, geocerca, dispositivo o QR.

Estas decisiones se tomarán en las fases correspondientes.
