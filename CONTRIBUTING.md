# Contributing to Incident Tracker

Thanks for your interest in the project. This guide explains how to set up the environment, how changes are made and what a good pull request looks like.

## Contents

1. [Getting set up](#getting-set-up)
2. [Workflow](#workflow)
3. [Commits and pull requests](#commits-and-pull-requests)
4. [Code style](#code-style)
5. [Tests](#tests)
6. [Database changes](#database-changes)
7. [API changes](#api-changes)
8. [Dependencies](#dependencies)
9. [Security](#security)
10. [Documentation](#documentation)
11. [Reporting problems and proposing features](#reporting-problems-and-proposing-features)

---

## Getting set up

Requirements:

- Java 21 or newer
- Node.js 20.19 or newer (CI uses 24) and [pnpm](https://pnpm.io); the exact version is in `frontend/package.json` (`packageManager`)
- Docker or Podman, for the development services and for the tests

```bash
git clone https://github.com/joselfv1979/incident-tracker.git
cd incident-tracker
cp .env.example .env          # then edit it: at least JWT_SECRET
docker compose up -d db mailpit
```

Run the backend and the frontend in two terminals:

```bash
# Terminal 1: backend (http://localhost:8080)
set -a; source .env; set +a
cd backend && ./mvnw spring-boot:run

# Terminal 2: frontend (http://localhost:5173)
cd frontend && pnpm install && pnpm dev
```

Emails sent by the application (such as the verification link) do not leave your machine: open Mailpit at http://localhost:8025 to read them. The interactive API documentation is at http://localhost:8080/swagger-ui.html.

Never put real credentials in `.env.example`. The `.env` file is ignored by Git.

## Workflow

- `main` is always releasable and is protected: changes reach it only through a pull request whose checks pass.
- **One branch per task**, created from an up-to-date `main`, named `<type>/<short-description>`:

  | Type | Use for |
  |------|---------|
  | `feat/` | A new feature |
  | `fix/` | A bug fix |
  | `docs/` | Documentation only |
  | `test/` | Tests only |
  | `chore/` | Tooling, configuration, dependencies |
  | `ci/` | Continuous integration |

- Keep branches small and focused. A branch that mixes unrelated things is hard to review.
- Pull requests are merged with **squash and merge**, so every pull request becomes a single commit on `main`.

```bash
git switch main && git pull --prune
git switch -c feat/my-change
# ... work, commit, push ...
git push -u origin feat/my-change
```

After the merge, delete your local branch. Git may say it is "not fully merged" because of the squash: that is expected, and `git branch -D` is safe once the pull request shows as merged.

## Commits and pull requests

Commit messages and pull request titles follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(backend): add incident comments
fix(frontend): keep the session when the server is unavailable
docs: update the contributing guide
```

The title of the pull request becomes the commit message on `main`, so write it carefully.

Fill in the pull request template: what changes, why, how to test it, and the checklist. If the change is visible, add a screenshot. Link the related issue with `Closes #<number>`.

Before opening a pull request, run the checks of the part you changed (see [Tests](#tests)). The same checks run in CI, and all of them must pass.

## Code style

- **EditorConfig** (`.editorconfig`) defines indentation, line endings and encoding for every file. Install the plugin for your editor if it does not support it natively.
- **Backend (Java)**: 4-space indentation, constructor injection, DTOs as records, and business rules in services, not in controllers. Do not return entities from the API.
- **Frontend (TypeScript and Vue)**: Composition API with `<script setup lang="ts">`. ESLint, Oxlint and Prettier are configured, and `pnpm lint` fixes what it can: check `git diff` afterwards. Render user text as text, never with `v-html`.
- Write code, comments, commit messages and documentation in **English**.

## Tests

| Part | Command | Notes |
|------|---------|-------|
| Backend | `cd backend && ./mvnw verify` | Needs Docker or Podman: the integration tests start a throwaway PostgreSQL with Testcontainers. They do not need `.env` or the development database |
| Frontend | `cd frontend && pnpm lint && pnpm build && pnpm test:unit --run` | `pnpm build` also checks the types |
| End to end | `cd e2e && pnpm services:up && pnpm test && pnpm services:down` | Uses its own isolated environment. See the README |

What to test:

- A new rule or behavior comes with a test that fails without it.
- Rules about who can do what (permissions and visibility) deserve a test for the allowed case **and** the forbidden one.
- A bug fix starts with a test that reproduces the bug.
- Add an end-to-end test only for a flow that matters to users and crosses the browser, the frontend and the backend. They are slow, so keep them few.

A green build does not prove that something is visible on screen. Open the application and try your change.

The end-to-end environment turns rate limiting off (`RATE_LIMIT_ENABLED=false`),
because the tests register and log in many users from a single address.

## Database changes

The schema is managed by Flyway, in `backend/src/main/resources/db/migration/`.

- **Never edit a migration that is already merged.** Flyway stores a checksum of each one and fails if it changes. Add a new `V<next>__description.sql` instead.
- Use `V<number>__short_description.sql`, with two underscores.
- Keep a migration small and, when it adds a constraint to existing data, say in the pull request how existing rows are handled.
- Hibernate only validates the schema (`ddl-auto=validate`): update the entity and the migration together.
- The migration tests apply every migration to an empty database, so a broken one fails the build.

## API changes

- Document every endpoint with `@Operation` (a summary, and a description when there are rules) and the relevant error responses with `@ApiResponse`. Mark public endpoints with `@SecurityRequirements`.
- A new endpoint must be added to the list in `OpenApiDocumentationTest`, which fails when an endpoint has no summary.
- Enforce permissions in the backend. The interface hides what a user cannot do, but it is the backend that must refuse it.
- When a user asks for something they must not see, answer `404` instead of `403`, so the response does not reveal that it exists.
- Do not expose more than the client needs: user data in responses is limited to id, name and role.

## Dependencies

Every dependency is code you must trust, so add them sparingly.

- Prefer the platform and the libraries already in the project before adding a new one.
- **pnpm** is configured with a release quarantine (`minimumReleaseAge`) and blocks dependency install scripts unless they are listed in `allowBuilds`. Do not relax these settings to install something: choose a version that is old enough, and explain any exception in the pull request.
- Commit the lockfile (`pnpm-lock.yaml`). CI installs with `--frozen-lockfile`.
- The Maven dependencies not managed by Spring Boot (for example springdoc) have their version set in `pom.xml`.
- Dependabot opens pull requests for Maven, pnpm, GitHub Actions and Docker Compose. Read the release notes of major versions before merging, and let the CI decide for the rest.

## Security

- **Never commit secrets**: passwords, tokens, keys or `.env` files. Test and CI values must be obviously throwaway.
- If you find a vulnerability, do **not** open a public issue. Report it privately through the repository's *Security* tab (*Report a vulnerability*).
- Think about abuse when you add an endpoint that is public: who can call it, how often, and what it reveals.
- A new public endpoint that can be abused (it creates data, sends email or checks
  credentials) must be covered by the rate limiter. See `RateLimitConfig`.

## Documentation

- Update the README and `docs/` when a change affects how the project is set up, run or used.
- `docs/PROJECT_OVERVIEW.md` describes the purpose, structure, entities and roadmap. Keep it in sync when they change.

## Reporting problems and proposing features

Use the issue forms:

- **Bug report**: what happened, the steps to reproduce it, what you expected and the environment. Remove any secret from logs before pasting them.
- **Feature request**: the problem, the proposed solution and how to know it is done.

For a large change, open an issue first so it can be discussed before you invest the time.
