# Spring Boot JWT Auth

API REST con Java 21 y Spring Boot para autenticacion JWT con `access token`, `refresh token`, roles `USER` y `ADMIN`, PostgreSQL y migraciones Flyway.

## Stack

- Java 21
- Spring Boot 3.2
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- OpenAPI / Swagger UI
- Spring Boot Actuator
- JUnit 5

## Funcionalidad

- Registro de usuario
- Login con JWT
- Refresh token con rotacion
- Logout con revocacion de refresh token
- Endpoint autenticado `/api/v1/users/me`
- Endpoint admin `/api/v1/admin/ping`
- Bootstrap de admin en perfil `dev`
- Healthcheck HTTP en `/actuator/health`
- Probes de Actuator en `/actuator/health/liveness` y `/actuator/health/readiness`

## Estructura

- `src/main/java/com/example/jwtauth/auth`: servicios y controladores de autenticacion
- `src/main/java/com/example/jwtauth/security`: JWT y configuracion de seguridad
- `src/main/java/com/example/jwtauth/user`: usuarios, roles y endpoint de perfil
- `src/main/java/com/example/jwtauth/token`: refresh tokens
- `src/main/resources/db/migration`: migraciones Flyway

## Perfiles

### `dev`

Usa defaults locales para desarrollo:

- PostgreSQL en `jdbc:postgresql://localhost:5432/jwt_auth_db`
- usuario `jwt_app`
- password por defecto `change-me-dev-password`
- `JWT_SECRET` por defecto local
- bootstrap de admin habilitado

### `test`

Usa H2 para tests locales rapidos y tiene una integracion opcional con PostgreSQL real via Testcontainers.

### `prod`

Requiere variables explicitas y desactiva:

- Swagger UI
- OpenAPI docs
- bootstrap de admin

## Variables de entorno

### Base / prod

```bash
export DB_URL=jdbc:postgresql://localhost:5432/jwt_auth_db
export DB_USERNAME=jwt_app
export DB_PASSWORD=change-me-dev-password
export JWT_SECRET=replace-with-a-long-random-secret
```

### Opcionales para `dev`

```bash
export ADMIN_EMAIL=admin@example.com
export ADMIN_PASSWORD=ChangeMe123!
```

## Base de datos local

Si todavia no existe la base:

```bash
sudo -i -u postgres
psql -d postgres -c "CREATE ROLE jwt_app WITH LOGIN PASSWORD 'change-me-dev-password';"
psql -d postgres -c "CREATE DATABASE jwt_auth_db OWNER jwt_app;"
exit
```

## Ejecutar en desarrollo

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

La aplicacion arranca en `http://localhost:8080`.

## Ejecutar con Docker Compose

Esto levanta dos contenedores:

- `app`: la aplicacion Spring Boot
- `postgres`: la base de datos PostgreSQL

Primero crea tu archivo `.env` a partir del ejemplo:

```bash
cp .env.example .env
```

Comando:

```bash
docker compose up --build
```

Accesos:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- PostgreSQL: `localhost:5432`

Variables configurables en `.env.example`:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_PORT`
- `APP_PORT`
- `SPRING_PROFILES_ACTIVE`
- `JWT_SECRET`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`

Mejoras del compose de desarrollo:

- usa `env_file: .env`
- mantiene PostgreSQL y app en contenedores separados
- permite cambiar puertos sin editar el YAML
- sigue exponiendo Swagger porque usa perfil `dev`
- valida salud de PostgreSQL y de la app
- usa `readiness` de Actuator para marcar la app como lista

Detener:

```bash
docker compose down
```

Detener y borrar volumen de datos:

```bash
docker compose down -v
```

## Ejecutar con Docker Compose para prod

Primero crea tu archivo `.env.prod`:

```bash
cp .env.prod.example .env.prod
```

Luego levanta la variante de produccion:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up --build -d
```

Notas:

- usa perfil `prod`
- Swagger y `/v3/api-docs` quedan desactivados
- no publica PostgreSQL en un puerto del host
- no habilita bootstrap de admin
- mantiene healthcheck HTTP en `/actuator/health`
- usa `readiness` para validar que la app ya puede recibir trafico

Detener:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down
```

## Ejecutar tests

```bash
mvn test
```

Requisito solo para la integracion con PostgreSQL real:

- Docker debe estar disponible porque las pruebas de integracion levantan PostgreSQL con Testcontainers

Para incluir la prueba con PostgreSQL real:

```bash
RUN_TESTCONTAINERS_TESTS=true mvn test
```

## CI

El repositorio incluye [ci.yml](/home/albert/dev/java/multiagent_codex_01/.github/workflows/ci.yml) para GitHub Actions.

Hace esto en cada `push` a `main` y en cada `pull_request`:

- checkout del codigo
- Java 21 con `Temurin`
- cache de dependencias Maven
- `mvn -B test`
- `docker build -t jwt-auth:ci .`

Cuando hay `push` a `main`, ademas:

- autentica contra `ghcr.io`
- publica la imagen Docker en `ghcr.io/<owner>/<repo>`
- ejecuta tambien la prueba opcional con Testcontainers porque define `RUN_TESTCONTAINERS_TESTS=true`

## Releases

El repositorio incluye [release.yml](/home/albert/dev/java/multiagent_codex_01/.github/workflows/release.yml).

Funcionamiento:

- si haces `push` de un tag como `v1.0.0`, GitHub crea una release
- las notas de la release se generan automaticamente
- tambien puedes dispararlo manualmente con `workflow_dispatch`

Flujo recomendado:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Swagger / OpenAPI

Con la app corriendo en `dev`:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Healthcheck

- Health: `http://localhost:8080/actuator/health`
- Liveness: `http://localhost:8080/actuator/health/liveness`
- Readiness: `http://localhost:8080/actuator/health/readiness`
- En `dev` queda publico para Docker Compose
- En `prod` sigue publico, pero solo expone el estado general

## Endpoints principales

### Auth

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

### Protegidos

- `GET /api/v1/users/me`
- `GET /api/v1/admin/ping`

## Ejemplos

### Registro

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!"}'
```

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"ChangeMe123!"}'
```

### Perfil actual

```bash
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access-token>"
```

## Commits recientes

- `6f4dd8b` `feat: implement jwt auth api with postgresql`
- `abacec2` `feat: add openapi swagger support`
- `4767893` `chore: harden secrets and profile configuration`
