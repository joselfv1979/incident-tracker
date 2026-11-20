# 🐞 Incident Tracker

[![CI](https://github.com/joselfv1979/incident-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/joselfv1979/incident-tracker/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A web application to **register, assign, track and resolve incidents** for a development or support team.

Anyone can create an account (with email verification) and report a problem. Agents assign it, move it through a status workflow, discuss it in comments (including internal notes) and consult statistics. Every change is kept in a history.

## Features

- **Open registration with email verification**, and JWT authentication.
- **Roles**: `USER` reports and follows their own incidents, `AGENT` works on all of them, `ADMIN` also manages roles.
- **Incidents** with status workflow (`OPEN → IN_PROGRESS → IN_REVIEW → RESOLVED → CLOSED`, with reopening), priority, category and assignee.
- **Search and filters** by text, status, priority, assignee and dates, with pagination. Filters live in the URL.
- **Comments**, including **internal notes** visible only to staff, with editing and deletion.
- **Change history**, shown with the comments in a single timeline.
- **Statistics** for staff: totals, workload per assignee, created vs resolved per day and average resolution time.
- **Interactive API documentation** (OpenAPI and Swagger UI).

## Tech stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Spring Security (JWT), Bean Validation, Spring Mail |
| Database | PostgreSQL 16, Flyway migrations |
| Frontend | Vue 3 (Composition API), TypeScript, Vite, Pinia, Vue Router, pnpm |
| Tests | JUnit 5, Mockito, Testcontainers · Vitest, Vue Test Utils · Playwright |
| Tooling | Docker Compose, GitHub Actions, Dependabot, ESLint, Oxlint, Prettier |

## Quick start

### Requirements

- Java 21 or newer
- Node.js 20.19 or newer and [pnpm](https://pnpm.io) (the version is pinned in `frontend/package.json`)
- Docker or Podman

### Run it

```bash
git clone https://github.com/joselfv1979/incident-tracker.git
cd incident-tracker

# 1. Configuration: edit .env and set at least JWT_SECRET (openssl rand -base64 48)
cp .env.example .env

# 2. PostgreSQL and a development mail server
docker compose up -d db mailpit

# 3. Backend, http://localhost:8080
set -a; source .env; set +a
cd backend && ./mvnw spring-boot:run
```

In another terminal:

```bash
# 4. Frontend, http://localhost:5173
cd frontend
pnpm install
pnpm dev
```

| What | Where |
|------|-------|
| Application | http://localhost:5173 |
| API documentation | http://localhost:8080/swagger-ui.html |
| Emails sent by the application (Mailpit) | http://localhost:8025 |

Register an account in the application and open the verification link from Mailpit. The administrator defined by `ADMIN_EMAIL` and `ADMIN_PASSWORD` is created at startup; it can promote users to agents with `PATCH /api/users/{id}/role` (see the API documentation).

The development servers proxy `/api` to the backend, so the browser only talks to one origin.

## Configuration

The backend reads its configuration from environment variables. `.env.example` lists them with development values, and `.env` is ignored by Git.

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL JDBC URL | none |
| `DB_USER`, `DB_PASSWORD` | Database credentials | none |
| `DB_NAME` | Database name used by Docker Compose | `incidents` |
| `JWT_SECRET` | Key that signs the tokens, at least 32 bytes | none |
| `JWT_EXPIRATION` | Token lifetime | `1h` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Administrator created at startup if it does not exist | none |
| `MAIL_HOST`, `MAIL_PORT` | SMTP server | `localhost`, `1025` |
| `MAIL_FROM` | Sender of the emails | `no-reply@incident-tracker.local` |
| `APP_BASE_URL` | Frontend URL used in the emailed links | `http://localhost:5173` |
| `VERIFICATION_TOKEN_TTL` | Lifetime of a verification link | `24h` |
| `VERIFICATION_RESEND_COOLDOWN` | Minimum time between verification emails | `60s` |
| `API_DOCS_ENABLED` | Serve the OpenAPI specification and Swagger UI | `true` |
| `SERVER_PORT` | Backend port | `8080` |

The values in `.env.example` are for local development. **Any public deployment must use its own random `JWT_SECRET`, a strong administrator password and a real SMTP server.**

## Project structure

```
incident-tracker/
├── backend/        Spring Boot REST API (packages: auth, user, incident, comment, history, stats...)
├── frontend/       Vue 3 single-page application
├── e2e/            Playwright end-to-end tests, with their own isolated environment
├── docs/           Project documentation
├── .github/        CI workflow, issue forms, pull request template and Dependabot
└── docker-compose.yml   PostgreSQL and Mailpit for development
```

`docs/PROJECT_OVERVIEW.md` explains the purpose, the structure, the entities and relationships, the learning objectives and the roadmap in more detail.

## API documentation

With the backend running, the interactive documentation is at http://localhost:8080/swagger-ui.html and the OpenAPI specification at http://localhost:8080/v3/api-docs.

To try the protected endpoints, call `POST /api/auth/login`, copy the `accessToken`, click **Authorize** and paste it. Set `API_DOCS_ENABLED=false` to turn the documentation off.

## Running the tests

```bash
# Backend: needs Docker or Podman. It starts its own PostgreSQL with Testcontainers,
# so neither .env nor the development database are needed.
cd backend && ./mvnw verify

# Frontend
cd frontend && pnpm lint && pnpm build && pnpm test:unit --run
```

### End-to-end tests

The `e2e/` folder holds Playwright tests that drive a real browser against the whole system. They use their own isolated environment (PostgreSQL on 5433, Mailpit on 8026, backend on 8081 and frontend on 5174), so they do not touch the development data.

```bash
cd frontend && pnpm install        # once
cd ../e2e
pnpm install
pnpm exec playwright install chromium
pnpm services:up
pnpm test
pnpm report                        # HTML report
pnpm services:down
```

## Status

The functional product is complete: authentication and registration, incident management, history and comments, and statistics. The quality phase is under way (integration tests with Testcontainers, API documentation and end-to-end tests are done).

Still to do before a public deployment: rate limiting, production configuration (SMTP, secrets, HTTPS), Dockerfiles and a user administration screen. The full roadmap is in [`docs/PROJECT_OVERVIEW.md`](docs/PROJECT_OVERVIEW.md).

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) first: it explains the workflow (one branch and one pull request per task, Conventional Commits), the checks that must pass and the project conventions.

## License

This project is licensed under the [MIT License](LICENSE).
