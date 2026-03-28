# Flujo Entre Capas

## Resumen

La aplicacion sigue una arquitectura por capas simple:

`Controller -> Service -> Repository/Infrastructure`

Spring Security se ejecuta antes de los controladores y decide si una solicitud llega o no a la capa web.

## Flujo General De Una Request

```text
Cliente HTTP
  -> Filtro JWT
  -> Reglas de Spring Security
  -> Controller
  -> Service
  -> Repository
  -> PostgreSQL
```

## Flujo De Registro

### 1. Entrada HTTP

[AuthController.java](../../src/main/java/com/example/jwtauth/auth/AuthController.java) recibe `POST /api/v1/auth/register`.

Responsabilidades:

- validar el cuerpo JSON con Bean Validation
- delegar en `AuthService`
- devolver `201 Created`

### 2. Logica de negocio

[AuthService.java](../../src/main/java/com/example/jwtauth/auth/AuthService.java) ejecuta:

- normalizacion de email
- verificacion de duplicado con `UserRepository`
- hash de password con `PasswordEncoder`
- asignacion inicial de rol `USER`
- persistencia del usuario
- emision de access token y refresh token

### 3. Persistencia

[UserRepository.java](../../src/main/java/com/example/jwtauth/user/UserRepository.java) guarda el usuario.

[RefreshTokenService.java](../../src/main/java/com/example/jwtauth/token/RefreshTokenService.java) crea y persiste el refresh token.

## Flujo De Login

### Paso a paso

1. `AuthController.login()` recibe email y password.
2. `AuthService.login()` busca usuario por email.
3. compara password plano contra `passwordHash` usando BCrypt.
4. si es valido, genera:
   - access token con [JwtService.java](../../src/main/java/com/example/jwtauth/security/JwtService.java)
   - refresh token con [RefreshTokenService.java](../../src/main/java/com/example/jwtauth/token/RefreshTokenService.java)
5. responde `AuthResponse`.

## Flujo De Refresh

### Paso a paso

1. `AuthController.refresh()` recibe un `refreshToken`.
2. `AuthService.refresh()` delega en `RefreshTokenService.validateActiveToken()`.
3. el servicio verifica:
   - existencia
   - no revocado
   - no expirado
4. revoca el refresh token actual.
5. emite un nuevo par de tokens.

### Propiedad importante

La rotacion de refresh token evita que el mismo token siga siendo reusable indefinidamente.

## Flujo De Logout

### Paso a paso

1. `AuthController.logout()` recibe `refreshToken`.
2. `AuthService.logout()` invoca `RefreshTokenService.revoke(rawToken)`.
3. el token se marca como revocado.
4. la respuesta es `204 No Content`.

## Flujo De Un Endpoint Protegido

Tomemos `GET /api/v1/users/me`.

### 1. Filtro JWT

[JwtAuthenticationFilter.java](../../src/main/java/com/example/jwtauth/security/JwtAuthenticationFilter.java):

- lee el header `Authorization`
- verifica prefijo `Bearer `
- valida el token con `JwtService`
- extrae el sujeto
- carga el usuario via `AppUserDetailsService`
- crea un `Authentication` y lo pone en `SecurityContextHolder`

### 2. Reglas de seguridad

[SecurityConfig.java](../../src/main/java/com/example/jwtauth/security/SecurityConfig.java):

- define endpoints publicos
- protege el resto
- exige `ADMIN` en `/api/v1/admin/**`
- usa sesion `STATELESS`

### 3. Controller y service

[UserController.java](../../src/main/java/com/example/jwtauth/user/UserController.java) recibe `Authentication`.

[UserService.java](../../src/main/java/com/example/jwtauth/user/UserService.java):

- toma `authentication.getName()`
- busca el usuario en base de datos
- devuelve `UserProfileResponse`

## Flujo De Errores

[GlobalExceptionHandler.java](../../src/main/java/com/example/jwtauth/error/GlobalExceptionHandler.java) centraliza la salida HTTP.

### Casos principales

- `MethodArgumentNotValidException` -> `400`
- `UnauthorizedException` -> `401`
- `ConflictException` -> `409`
- `Exception` generica -> `500`

### Formato de error

La respuesta contiene:

- timestamp
- status
- error
- message
- path

## Flujo Del Bootstrap De Admin

En perfil `dev`, [AdminBootstrapRunner.java](../../src/main/java/com/example/jwtauth/config/AdminBootstrapRunner.java) se ejecuta al arrancar.

Hace esto:

1. revisa `app.admin-bootstrap.enabled`
2. normaliza email
3. si el admin no existe, lo crea con rol `ADMIN`
4. guarda el password hasheado

Esto no ocurre en `prod`.

## Diagrama De Secuencia Simplificado

```text
Cliente
  -> AuthController
  -> AuthService
  -> UserRepository
  -> JwtService
  -> RefreshTokenService
  -> RefreshTokenRepository
  -> Respuesta HTTP
```

## Puntos Clave Para Estudiar

- la autenticacion real ocurre antes de llegar al controller
- `JwtService` no depende de librerias externas de JWT; implementa HS256 manualmente
- los refresh tokens si se persisten en base de datos
- el access token no se persiste
- la autorizacion por rol depende de `ROLE_` dentro de `AppUserDetailsService`
