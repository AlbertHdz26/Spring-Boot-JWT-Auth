# ADR: Use Spring Boot JWT PostgreSQL Monolith

- Status: accepted
- Date: 2026-03-27

## Context

El proyecto busca enseñar autenticacion JWT de extremo a extremo en una base de codigo manejable localmente. La solucion debe cubrir:

- endpoints REST de autenticacion
- hashing de passwords
- access token JWT
- refresh token persistido
- persistencia relacional
- roles simples
- documentacion y pruebas

Las restricciones principales son:

- Java 21 + Spring Boot
- mantener el codigo simple
- usar dependencias minimas
- priorizar claridad para estudio local

## Decision

Se adopta un monolito modular con Spring Boot, Spring Security, PostgreSQL y Flyway.

Caracteristicas del enfoque:

- API REST con controladores MVC
- JWT HS256 para access tokens
- refresh tokens opacos almacenados en PostgreSQL
- entidades `User` y `RefreshToken` con `UUID`
- autorizacion por roles `USER` y `ADMIN`
- perfiles `dev`, `test`, `prod`
- Swagger para exploracion de la API
- Actuator para healthchecks

## Alternatives Considered

### 1. Sesiones stateful con Spring Security

Ventajas:

- implementacion mas simple

Desventajas:

- no cumple el objetivo de estudiar JWT
- menos alineado con APIs stateless modernas

### 2. OAuth2 Authorization Server completo

Ventajas:

- mas cercano a escenarios enterprise complejos

Desventajas:

- mucha mas complejidad operativa y conceptual
- excesivo para una base de estudio local

### 3. Persistencia solo en memoria

Ventajas:

- arranque rapido

Desventajas:

- no enseña integracion real con base de datos
- no cubre migraciones ni manejo real de refresh tokens

## Consequences

### Positivas

- el flujo completo de autenticacion queda visible en pocos paquetes
- PostgreSQL y Flyway introducen disciplina real de persistencia
- los refresh tokens revocables permiten estudiar seguridad mas alla del JWT basico
- la arquitectura es suficientemente pequena para recorrerse de punta a punta

### Negativas

- `JwtService` implementa JWT manualmente y requiere mas cuidado conceptual
- el proyecto no modela escenarios distribuidos ni federacion de identidad
- la revocacion aplica a refresh tokens, no a access tokens ya emitidos

## Non-Goals

- no construir un authorization server OAuth2 completo
- no soportar permisos finos mas alla de `USER` y `ADMIN`
- no desplegar automaticamente a nube o VPS
- no convertir el proyecto en microservicios

## Implementation Plan

- **Paquetes principales**:
  - `src/main/java/com/example/jwtauth/auth`
  - `src/main/java/com/example/jwtauth/security`
  - `src/main/java/com/example/jwtauth/user`
  - `src/main/java/com/example/jwtauth/token`
  - `src/main/java/com/example/jwtauth/error`
  - `src/main/java/com/example/jwtauth/config`
- **Persistencia**:
  - definir esquema inicial en [V1__create_users_and_refresh_tokens.sql](../../src/main/resources/db/migration/V1__create_users_and_refresh_tokens.sql)
  - mapear entidades JPA `User` y `RefreshToken`
  - usar `ddl-auto=validate`
- **Seguridad**:
  - mantener `SecurityConfig` como punto central de reglas HTTP
  - usar `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`
  - exponer solo auth, Swagger y health como publicos
- **Servicios**:
  - centralizar autenticacion en `AuthService`
  - centralizar refresh tokens en `RefreshTokenService`
  - centralizar JWT en `JwtService`
- **Testing**:
  - tests unitarios para JWT y auth service
  - pruebas de integracion para endpoints principales
  - prueba opcional con PostgreSQL real usando Testcontainers

## Verification

- [x] existe migracion Flyway para `users` y `refresh_tokens`
- [x] `register`, `login`, `refresh` y `logout` funcionan
- [x] `/api/v1/users/me` requiere token valido
- [x] `/api/v1/admin/ping` exige `ADMIN`
- [x] los refresh tokens pueden revocarse
- [x] Swagger y Actuator estan disponibles para uso local

## Related Code

- [AuthService.java](../../src/main/java/com/example/jwtauth/auth/AuthService.java)
- [JwtService.java](../../src/main/java/com/example/jwtauth/security/JwtService.java)
- [SecurityConfig.java](../../src/main/java/com/example/jwtauth/security/SecurityConfig.java)
- [RefreshTokenService.java](../../src/main/java/com/example/jwtauth/token/RefreshTokenService.java)
