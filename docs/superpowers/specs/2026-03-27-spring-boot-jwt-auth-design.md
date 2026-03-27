# Spring Boot JWT Auth Design

**Fecha:** 2026-03-27

## Objetivo

Construir una API REST educativa pero bien diseñada con Java 21 y Spring Boot que implemente autenticacion con JWT, refresh tokens y autorizacion basica por roles (`USER`, `ADMIN`), manteniendo una arquitectura simple, dependencias minimas y buenas practicas de ingenieria de software, usando PostgreSQL local y migraciones versionadas con Flyway.

## Alcance

Esta primera iteracion incluye:

- registro de usuarios
- login con emision de `accessToken` y `refreshToken`
- renovacion de tokens mediante refresh token
- logout con revocacion de refresh token
- endpoint autenticado para el usuario actual
- endpoint protegido por rol `ADMIN`
- persistencia con PostgreSQL
- migraciones versionadas con Flyway
- aprovisionamiento local de base de datos y usuario dedicado
- pruebas unitarias e integracion para el flujo principal

Esta primera iteracion no incluye:

- OpenAPI
- verificacion por email
- recuperacion de password
- permisos finos adicionales a roles
- blacklist de access tokens

## Principios de diseno

- Mantener el proyecto pequeno y legible.
- Separar responsabilidades por capas claras.
- No exponer entidades JPA directamente en la API.
- Evitar dependencias innecesarias.
- Mantener la API stateless, excepto por el control de refresh tokens.
- Externalizar configuracion sensible y tiempos de expiracion.
- Gestionar el esquema de base de datos con migraciones reproducibles.

## Arquitectura propuesta

La aplicacion sera un monolito simple con estructura por responsabilidades:

- `controller`: expone endpoints REST versionados.
- `service`: implementa reglas de negocio de autenticacion y usuarios.
- `repository`: acceso a datos con Spring Data JPA.
- `security`: configuracion de Spring Security, generacion y validacion de JWT, filtro de autenticacion.
- `domain` o `model`: entidades y enums del dominio.
- `dto`: contratos de entrada y salida.
- `exception`: manejo centralizado de errores.

## Componentes principales

### 1. Dominio

Entidades iniciales:

- `User`
  - `id` UUID
  - `email`
  - `passwordHash`
  - `role`
  - `createdAt`
  - `updatedAt`

- `RefreshToken`
  - `id` UUID
  - `token`
  - `user`
  - `expiresAt`
  - `revoked`
  - `createdAt`

Enum:

- `Role`
  - `USER`
  - `ADMIN`

### 2. Seguridad

Decisiones:

- `accessToken` sera un JWT firmado con una clave secreta configurable.
- `refreshToken` sera un valor opaco, aleatorio, persistido en base de datos.
- el `accessToken` tendra vida corta.
- el `refreshToken` tendra vida mayor y sera revocable.
- las passwords se almacenaran usando `BCryptPasswordEncoder`.
- Hibernate validara el esquema, pero no lo creara ni modificara automaticamente.

### 3. Servicios

Servicios iniciales:

- `AuthService`
  - registrar usuario
  - autenticar credenciales
  - emitir tokens
  - refrescar sesion
  - revocar refresh token

- `UserService`
  - obtener el usuario autenticado

- `JwtService`
  - generar y validar access tokens

- `RefreshTokenService`
  - crear tokens aleatorios
  - persistirlos
  - validar expiracion y revocacion
  - rotarlos en refresh

## Endpoints iniciales

### Publicos

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

### Protegidos

- `GET /api/v1/users/me`
- `GET /api/v1/admin/ping`

## Contratos funcionales

### Registro

- recibe `email` y `password`
- valida formato y reglas basicas
- rechaza emails duplicados con `409 Conflict`
- crea usuario con rol por defecto `USER`
- retorna una respuesta segura, sin exponer hash ni datos internos

### Login

- recibe `email` y `password`
- valida credenciales
- retorna `accessToken`, `refreshToken`, tipo de token y metadatos basicos del usuario
- ante credenciales invalidas retorna `401 Unauthorized`

### Refresh

- recibe un `refreshToken`
- verifica existencia, expiracion y estado de revocacion
- revoca el token anterior
- emite un nuevo par `accessToken` + `refreshToken`
- si el token es invalido o expiro, retorna `401 Unauthorized`

### Logout

- recibe un `refreshToken`
- marca el refresh token como revocado
- responde sin contenido de negocio adicional

### Usuario actual

- usa el `accessToken` en header `Authorization: Bearer <token>`
- retorna informacion minima del usuario autenticado

### Admin

- requiere rol `ADMIN`
- sirve como endpoint simple de validacion de autorizacion por rol

## Diseno de API

Convenciones:

- prefijo versionado `/api/v1`
- endpoints orientados a recursos y casos de uso concretos
- respuestas JSON consistentes
- codigos HTTP semanticos

Posibles DTOs:

- `RegisterRequest`
- `LoginRequest`
- `RefreshTokenRequest`
- `LogoutRequest`
- `AuthResponse`
- `UserProfileResponse`
- `ErrorResponse`

## Flujo de autenticacion

1. El usuario se registra con email y password.
2. El usuario inicia sesion con credenciales validas.
3. El backend genera un JWT de acceso y un refresh token aleatorio.
4. El cliente usa el JWT para acceder a endpoints protegidos.
5. Cuando expira el JWT, el cliente usa el refresh token.
6. El backend valida el refresh token, revoca el anterior y devuelve nuevos tokens.
7. En logout, el refresh token entregado se marca como revocado.

## Manejo de errores

La API tendra un manejador global de excepciones con un formato uniforme. Casos iniciales:

- `400 Bad Request` para payload invalido
- `401 Unauthorized` para autenticacion fallida o tokens invalidos
- `403 Forbidden` para acceso sin permisos suficientes
- `404 Not Found` para recursos no existentes cuando aplique
- `409 Conflict` para email duplicado
- `500 Internal Server Error` para errores no controlados

Formato base esperado:

- `timestamp`
- `status`
- `error`
- `message`
- `path`

## Persistencia

Base de datos inicial: PostgreSQL local.

Razones:

- acerca la iteracion inicial a un entorno real
- permite aprender el flujo completo con una base de datos relacional real
- evita diferencias innecesarias entre desarrollo y una futura evolucion
- se integra bien con migraciones versionadas

Repositorios esperados:

- `UserRepository`
- `RefreshTokenRepository`

Reglas de persistencia:

- `email` debe ser unico
- `refreshToken.token` debe ser unico
- se debe poder consultar refresh tokens por valor y por usuario
- las claves primarias de negocio seran `UUID`
- `role` se persistira como texto, no como ordinal

## Modelo de datos inicial

### Tabla `users`

- `id UUID PRIMARY KEY`
- `email VARCHAR(...) NOT NULL UNIQUE`
- `password_hash VARCHAR(...) NOT NULL`
- `role VARCHAR(...) NOT NULL`
- `created_at TIMESTAMP WITH TIME ZONE NOT NULL`
- `updated_at TIMESTAMP WITH TIME ZONE NOT NULL`

### Tabla `refresh_tokens`

- `id UUID PRIMARY KEY`
- `token VARCHAR(...) NOT NULL UNIQUE`
- `user_id UUID NOT NULL`
- `expires_at TIMESTAMP WITH TIME ZONE NOT NULL`
- `revoked BOOLEAN NOT NULL`
- `created_at TIMESTAMP WITH TIME ZONE NOT NULL`

Restricciones e indices iniciales:

- foreign key de `refresh_tokens.user_id` hacia `users.id`
- indice por `users.email`
- indice por `refresh_tokens.user_id`
- unique index por `refresh_tokens.token`

Relaciones:

- un usuario puede tener multiples refresh tokens
- un refresh token pertenece a un unico usuario

## Aprovisionamiento local de PostgreSQL

Para desarrollo local en esta maquina:

- crear usuario PostgreSQL dedicado `jwt_app`
- crear base de datos `jwt_auth_db`
- asignar ownership de la base a `jwt_app`
- configurar la aplicacion para usar esas credenciales

Esta creacion de usuario y base se hace una sola vez. La evolucion del esquema se realizara exclusivamente con Flyway.

## Migraciones

Flyway sera el mecanismo oficial de creacion y evolucion del esquema.

Decisiones:

- migracion inicial `V1__create_users_and_refresh_tokens.sql`
- cambios futuros mediante nuevas migraciones versionadas
- no usar `spring.jpa.hibernate.ddl-auto=update`
- usar `spring.jpa.hibernate.ddl-auto=validate`

## Configuracion

Configuracion externalizada en `application.yml`:

- secreto JWT
- expiracion de access token
- expiracion de refresh token
- URL JDBC de PostgreSQL
- usuario y password de la base de datos
- configuracion de Flyway
- logging basico

No se deben hardcodear secretos en el codigo fuente.

## Seguridad en Spring

La configuracion de seguridad debe:

- deshabilitar CSRF para API stateless
- usar politica `STATELESS`
- permitir acceso publico a endpoints de auth
- proteger el resto de endpoints
- aplicar autorizacion por rol en rutas o metodos
- registrar un filtro JWT antes del mecanismo de autenticacion por defecto

## Testing

Cobertura minima esperada en esta iteracion:

- pruebas unitarias para `AuthService`
- pruebas unitarias para `JwtService` o utilidades de token
- pruebas de integracion para:
  - arranque correcto con Flyway
  - registro exitoso
  - login exitoso
  - refresh exitoso
  - acceso denegado sin token
  - acceso permitido con rol correcto

Objetivo del testing:

- verificar comportamiento funcional clave
- detectar regresiones tempranas
- mantener la implementacion pequena pero confiable

## Bootstrapping de roles

El endpoint publico de registro nunca permitira elegir `ADMIN`.

Para esta iteracion, el usuario administrador se creara mediante datos de arranque controlados solo para desarrollo local y pruebas. La estrategia sera:

- crear un usuario `ADMIN` inicial en el arranque de la aplicacion si no existe
- activar ese bootstrap solo en un perfil no productivo
- usar credenciales configurables desde propiedades

En tests de integracion tambien se podra sembrar el rol `ADMIN` directamente con datos de prueba cuando sea mas simple y aislado.

## Evolucion prevista

La arquitectura queda preparada para una segunda fase con:

- OpenAPI
- endurecimiento de secretos y perfiles
- auditoria
- mejores politicas de revocacion

## Criterios de exito

La primera iteracion se considerara exitosa si:

- un usuario puede registrarse e iniciar sesion
- los tokens funcionan correctamente
- el refresh token se rota y revoca de forma segura
- un endpoint autenticado responde con el usuario actual
- un endpoint de admin exige rol `ADMIN`
- PostgreSQL local queda creado con usuario dedicado para la app
- Flyway crea el esquema inicial de forma reproducible
- la solucion mantiene codigo simple, probado y facil de extender
