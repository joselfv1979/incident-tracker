# Incident Tracker

A web application to **register, assign, track and resolve incidents** for a development or support team.

## Overview

Incident Tracker centralizes incident management in a single place: any user can report a problem, the team assigns it to an owner, progress is tracked through comments and status changes, and once resolved the incident remains available for analysis through statistics.

### Key features

- **Incident registration** with title, description, priority, category and attachments.
- **Assignment** to an owner (and reassignment).
- **Status workflow**: `OPEN → IN_PROGRESS → IN_REVIEW → RESOLVED → CLOSED` (with the ability to reopen).
- **Comments** to keep a record of progress.
- **Change history** (audit trail for status, assignee and priority changes).
- **Search and filters** by status, priority, assignee, dates and free text.
- **Statistics**: incidents by status, priority and assignee, mean time to resolution, trends over time.
- **Authentication and roles**: `ADMIN`, `AGENT`, `USER`.

## Tech stack

| Layer    | Technology                                                                                  |
| -------- | ------------------------------------------------------------------------------------------- |
| Backend  | Java 21, Spring Boot 3, Spring Web, Spring Data JPA, Spring Security (JWT), Bean Validation |
| Database | PostgreSQL 16, Flyway (migrations)                                                          |
| Frontend | Vue 3 (Composition API), TypeScript, Vite, Pinia, Vue Router, pnpm                                |
| Quality  | JUnit 5, Mockito, Testcontainers · Vitest, Vue Test Utils, ESLint, Oxlint, Prettier · Playwright                   |
| API docs | springdoc-openapi (Swagger UI)                                                              |
| DevOps   | Docker, Docker Compose, GitHub Actions                                                      |

## Repository structure

```
incident-tracker/
├── backend/                  # Spring Boot REST API
│   ├── src/main/java/com/example/incidents/
│   │   ├── config/           # Security, CORS, OpenAPI
│   │   ├── auth/             # Login, JWT, roles
│   │   ├── user/
│   │   ├── incident/         # Entity, repository, service, controller, DTOs
│   │   ├── comment/
│   │   ├── history/          # Change audit trail
│   │   ├── stats/            # Statistics endpoints
│   │   └── common/           # Exceptions, utilities, pagination
│   ├── src/main/resources/
│   │   └── db/migration/     # Flyway scripts (V1__init.sql, ...)
│   └── pom.xml
├── frontend/                 # Vue 3 + TypeScript SPA
│   ├── src/
│   │   ├── api/              # HTTP client and services
│   │   ├── components/
│   │   ├── views/            # Login, list, detail, statistics
│   │   ├── stores/           # Pinia
│   │   ├── router/
│   │   ├── types/
│   │   └── composables/
│   └── package.json
├── docs/                     # Architecture decisions, diagrams
├── docker-compose.yml
├── .github/workflows/        # CI
└── README.md
```

## Data model (draft)

```
User(id, name, email, password_hash, role, created_at)
Incident(id, title, description, status, priority, category,
         reporter_id, assignee_id, created_at, updated_at, resolved_at)
Comment(id, incident_id, author_id, body, created_at)
IncidentHistory(id, incident_id, actor_id, field, old_value, new_value, created_at)
```

## REST API (draft)

| Method   | Endpoint                       | Description                             |
| -------- | ------------------------------ | --------------------------------------- |
| POST     | `/api/auth/login`              | Authenticate, returns a JWT             |
| GET      | `/api/incidents`               | Paginated list with filters             |
| POST     | `/api/incidents`               | Create an incident                      |
| GET      | `/api/incidents/{id}`          | Get details                             |
| PUT      | `/api/incidents/{id}`          | Update                                  |
| PATCH    | `/api/incidents/{id}/status`   | Change status                           |
| PATCH    | `/api/incidents/{id}/assignee` | Assign an owner                         |
| GET/POST | `/api/incidents/{id}/comments` | List / add comments                     |
| GET      | `/api/incidents/{id}/history`  | Change history                          |
| GET      | `/api/stats/summary`           | Totals by status, priority and assignee |
| GET      | `/api/stats/resolution-time`   | Mean time to resolution                 |
| GET      | `/api/users`                   | Get users                               |
| GET      | `/api/users/assignable`        | Get users' roles                        |
| PATCH    | `/api/users/{id}/role`         | Change user's role                      |

## Getting started

### Requirements

- Java 21+
- Node.js 20.19+
- pnpm
- Docker and Docker Compose

### With Docker Compose

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### Local development

```bash
# 1. Database
docker compose up -d db

# 2. Backend
cd backend
./mvnw spring-boot:run

# 3. Frontend
cd frontend
npnpm install
pnpm dev
```

### Environment variables

| Variable                  | Description                  | Example                                      |
| ------------------------- | ---------------------------- | -------------------------------------------- |
| `DB_URL`                  | PostgreSQL JDBC URL          | `jdbc:postgresql://localhost:5432/incidents` |
| `DB_USER` / `DB_PASSWORD` | Database credentials         | `incidents` / `incidents`                    |
| `JWT_SECRET`              | Token signing key            | _(long random string)_   
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Admin credentials     | `admin@example.com` /`admin-password`            |
| `VITE_API_URL`            | API URL used by the frontend | `http://localhost:8080/api`                  |

## Tests

```bash
cd backend && ./mvnw test
cd frontend && pnpm run test
```

---

## 🗺️ Roadmap and task breakdown

> Convention: each phase can be a GitHub **milestone** and each checkbox an **issue**.

### Phase 0 · Repository foundations

- [x] Create the monorepo structure (`backend/`, `frontend/`, `docs/`)
- [ ] Initialize the Spring Boot project (Web, JPA, Security, Validation, Flyway, PostgreSQL, Lombok)
- [x] Initialize the Vue 3 + TS project with Vite, Pinia, Vue Router, ESLint and Prettier
- [ ] `docker-compose.yml` with PostgreSQL
- [ ] `.gitignore`, `.editorconfig`, license
- [ ] Basic CI with GitHub Actions (build + tests for both modules)
- [ ] Issue and pull request templates

### Phase 1 · Database and domain

- [ ] Design the data model and ER diagram in `docs/`
- [ ] Flyway migration `V1__init.sql` (users, incidents, comments, incident_history)
- [ ] JPA entities and repositories
- [ ] Enums for status, priority and role
- [ ] Seed data for development

### Phase 2 · Authentication and users

- [ ] Registration and login with JWT
- [ ] Spring Security configuration and roles
- [ ] Users endpoint (list for assignment)
- [ ] Login view and route guards in the frontend
- [ ] HTTP interceptor with token and expired-session handling

### Phase 3 · Incident management (MVP)

- [ ] Incident CRUD with DTOs and validation
- [ ] Paginated list with filtering and sorting
- [ ] Status change with valid transition rules
- [ ] Assignment and reassignment of owner
- [ ] Global error handling (`@ControllerAdvice`)
- [ ] Frontend: list, filters, create/edit form
- [ ] Frontend: detail view with status change and assignment

### Phase 4 · Comments and history

- [ ] Comment endpoints
- [ ] Automatic history logging on status, priority or assignee changes
- [ ] Frontend: comment thread and history timeline

### Phase 5 · Statistics

- [ ] Aggregate queries (by status, priority, assignee, period)
- [ ] Mean time to resolution calculation
- [ ] Frontend: dashboard with charts (Chart.js or ECharts)

### Phase 6 · Quality and documentation

- [ ] Unit tests for services and business rules
- [ ] Integration tests with Testcontainers
- [ ] Component and store tests (Vitest)
- [ ] E2E tests for the main flows (Playwright)
- [ ] Review OpenAPI documentation
- [ ] Contribution guide (`CONTRIBUTING.md`)

### Phase 7 · Future improvements (backlog)

- [ ] Attachments on incidents
- [ ] Email or webhook notifications
- [ ] Custom labels and categories
- [ ] SLAs and due-date alerts
- [ ] `@user` mentions in comments
- [ ] CSV export
- [ ] Dark mode and internationalization (ES/EN)
- [ ] Deployment (Docker on a VPS or PaaS)

---

## Conventions

- **Branches**: `main` (stable), `develop`, `feature/<description>`, `fix/<description>`
- **Commits**: [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`)
- **PRs**: one issue per PR, with tests and green CI

## License

This project is licensed under the [MIT License](LICENSE).
