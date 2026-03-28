# Informe Tecnico Del Proyecto

## Resumen

`jwt-auth` es una API REST construida con Java 21 y Spring Boot 3.2 para estudiar autenticacion JWT con `access token`, `refresh token`, roles simples y persistencia relacional con PostgreSQL.

El sistema esta pensado como monolito modular para aprendizaje local. La prioridad no es orquestacion distribuida ni despliegue cloud, sino claridad tecnica, buenas practicas y facilidad para recorrer el flujo completo de autenticacion.

## Objetivo Del Proyecto

- exponer una API REST de autenticacion con JWT
- persistir usuarios y refresh tokens en PostgreSQL
- separar responsabilidades por capas
- soportar roles `USER` y `ADMIN`
- documentar la API con OpenAPI / Swagger
- permitir ejecucion local con Maven o Docker Compose

## Stack Tecnologico

### Runtime y framework

- Java 21
- Spring Boot 3.2.4
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Spring Validation
- Spring Boot Actuator

### Persistencia

- PostgreSQL
- Flyway para migraciones versionadas
- H2 solo para pruebas locales rapidas

### Documentacion y operacion

- Springdoc OpenAPI
- Swagger UI
- Docker
- Docker Compose

### Testing

- JUnit 5
- Spring Boot Test
- Spring Security Test
- Testcontainers para una prueba opcional con PostgreSQL real

## Capacidades Funcionales

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/users/me`
- `GET /api/v1/admin/ping`
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

## Estructura Del Codigo

### Capas y paquetes

- `auth`
  Orquesta registro, login, refresh y logout.
- `security`
  Implementa JWT, filtro de autenticacion y configuracion de Spring Security.
- `user`
  Gestiona usuario autenticado, roles, perfil y repositorio.
- `token`
  Gestiona refresh tokens persistidos y su revocacion.
- `config`
  Propiedades, Swagger y bootstrap de admin en `dev`.
- `error`
  Manejo uniforme de excepciones y errores HTTP.
- `admin`
  Endpoints protegidos por rol `ADMIN`.

## Modelo De Datos

### Tabla `users`

- `id` `UUID`
- `email` unico
- `password_hash`
- `role`
- `created_at`
- `updated_at`

### Tabla `refresh_tokens`

- `id` `UUID`
- `token` unico
- `user_id`
- `expires_at`
- `revoked`
- `created_at`

## Configuracion Relevante

### JWT

Configurado en [JwtProperties.java](../../src/main/java/com/example/jwtauth/config/JwtProperties.java):

- `secret`
- `accessTokenExpiration`
- `refreshTokenExpiration`

Valores por defecto actuales en [application.yml](../../src/main/resources/application.yml):

- access token: `PT15M`
- refresh token: `P7D`

### Perfiles

- `dev`
  Usa PostgreSQL local y bootstrap de admin.
- `test`
  Usa H2 por defecto para pruebas locales.
- `prod`
  Desactiva Swagger y bootstrap de admin.

## Seguridad

### Publico

- `/api/v1/auth/**`
- `/actuator/health/**`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`

### Protegido

- `/api/v1/users/**`
- `/api/v1/admin/**`

Restricciones:

- `/api/v1/admin/**` requiere `ROLE_ADMIN`
- el resto de endpoints no publicos requiere autenticacion valida

## Artefactos De Ejecucion

### Local con Maven

La aplicacion puede ejecutarse con:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Local con Docker Compose

El proyecto incluye:

- [docker-compose.yml](../../docker-compose.yml)
- [docker-compose.prod.yml](../../docker-compose.prod.yml)

## Pruebas

### Suite local

- pruebas unitarias para `JwtService`
- pruebas unitarias para `AuthService`
- pruebas de integracion para endpoints principales

### Suite opcional

- una prueba adicional con PostgreSQL real usando Testcontainers
- se activa con `RUN_TESTCONTAINERS_TESTS=true`

## Estado Arquitectonico Actual

El proyecto ya cubre el ciclo completo de autenticacion JWT de forma consistente para fines de estudio:

- emision de access token
- rotacion de refresh token
- autorizacion por rol
- persistencia de tokens
- revocacion de refresh token
- healthchecks y documentacion de API
