# ADR: Keep Project Local Study Focused

- Status: accepted
- Date: 2026-03-27

## Context

El proyecto no busca operar como producto SaaS ni como servicio desplegado automaticamente. Su objetivo es aprendizaje tecnico local: entender el flujo de JWT, persistencia, seguridad y pruebas sin ruido operativo innecesario.

En iteraciones previas aparecieron opciones como:

- releases automaticas
- publicacion de imagenes en registries
- despliegues a VPS o nube

Esas capacidades agregan valor en escenarios productivos, pero distraen del objetivo de estudio local.

## Decision

Se mantiene el repositorio orientado a estudio local.

Esto implica:

- conservar CI minima para validar codigo y `docker build`
- eliminar workflows de release automatica
- eliminar publicacion automatica de imagenes
- documentar solo los flujos locales realmente utiles

## Alternatives Considered

### 1. Mantener automatizaciones de release y publish

Ventajas:

- mas cercano a una plataforma real

Desventajas:

- aumenta complejidad operativa
- introduce conceptos que no son necesarios para el objetivo actual

### 2. Eliminar tambien CI basica

Ventajas:

- maxima simplicidad

Desventajas:

- se pierde una verificacion automatica util incluso para estudio

## Consequences

### Positivas

- el repo queda mas facil de entender
- menor ruido en documentacion y workflows
- menos dependencias conceptuales con GitHub como plataforma

### Negativas

- si en el futuro el proyecto evoluciona a despliegue real, habra que reintroducir automatizaciones

## Non-Goals

- no automatizar releases
- no publicar artefactos en registries remotos
- no desplegar automaticamente a nube

## Implementation Plan

- **Workflows**:
  - mantener solo [ci.yml](../../.github/workflows/ci.yml) como pipeline basico
  - eliminar workflows de release
- **Documentacion**:
  - mantener README y docs orientados a ejecucion local
  - documentar Compose, Maven y pruebas
- **Operacion**:
  - usar PostgreSQL local o Docker Compose local
  - evitar dependencias obligatorias con infraestructura externa

## Verification

- [x] el repositorio no depende de releases automaticas para su uso normal
- [x] el CI sigue validando tests y build de imagen local
- [x] la documentacion describe flujos locales y no despliegues remotos

## Related Code

- [ci.yml](../../.github/workflows/ci.yml)
- [README.md](../../README.md)
