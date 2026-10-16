# Quickstart: Monolith Foundation

This guide validates local execution after implementation. It intentionally adds no
catalog, identity, authentication, or cart workflows.

## Prerequisites

- Git
- Docker Engine/Desktop with Docker Compose v2
- A browser

The Compose images provide the pinned Java/Maven and Node/npm toolchains used by the
services. Exact image tags are pinned in the implementation, not inferred from the
host machine.

## Start the stack

From the repository root, copy the example environment file:

```text
Copy-Item .env.example .env
```

On macOS/Linux, use:

```sh
cp .env.example .env
```

Start the stack:

```text
docker compose up --build
```

Use local-only values for placeholders in `.env`; do not use production credentials.
Compose starts a single PostgreSQL database, one Spring Boot backend, and one Vite
frontend. PostgreSQL health gates backend startup; backend readiness gates successful
API validation. No service from the original microservices repository is required.

## Validate health and frontend connectivity

In a second terminal, check both the backend and the frontend-to-backend route.
For Windows PowerShell:

```powershell
curl.exe -fsS http://localhost:8080/actuator/health/readiness
curl.exe -fsS http://localhost:5173/actuator/health/readiness
```

For macOS/Linux:

```sh
curl -fsS http://localhost:8080/actuator/health/readiness
curl -fsS http://localhost:5173/actuator/health/readiness
```

Both requests should report `UP`. The second request passes through the local Vite
development-server proxy to the same backend readiness endpoint; it does not invoke
a Gateway or separate business service.

Open `http://localhost:5173`. The minimal foundation page should show the API
readiness result. Stop the database container and verify the backend no longer
reports ready; restore it and verify readiness returns.

## Run automated checks

Use the repository's documented scripts after implementation. The expected checks
are:

```powershell
mvn -f backend/pom.xml verify
mvn -f backend/pom.xml -Dtest=ModuleBoundariesTest test
npm --prefix frontend ci
npm --prefix frontend test
npm --prefix frontend run build
```

The Maven test suite includes unit and architecture tests. PostgreSQL migration and
health integration tests use Testcontainers and require Docker. The frontend test
suite includes component/API client tests. With the stack running, execute the
automated frontend-to-backend smoke check:

```powershell
npm --prefix frontend run test:smoke
```

The smoke test requests `http://localhost:5173/actuator/health/readiness` through
the local Vite server and expects HTTP success with `status: UP`.

## Environment and teardown

Review [.env.example](../../.env.example) and the root [README.md](../../README.md)
for variable names, defaults/placeholders, and secret-handling notes. The example
must not contain credentials or usable JWT secrets.

Stop the local services with:

```powershell
docker compose down
```

Use `docker compose down -v` only when intentionally deleting the local database
volume and its data.

## Validation record

The Maven verification completed successfully: 7 tests were reported, with 0 failures
and 2 PostgreSQL/Testcontainers tests skipped because Docker is unavailable. The
remaining 5 unit/architecture tests passed, including `ModuleBoundariesTest`. The
frontend unit tests passed (4 tests), its production build succeeded, and `npm audit`
reported no vulnerabilities. The smoke script also passed through the Vite proxy
against a temporary local readiness responder. This does not replace validation
against the real Compose backend: Compose startup, PostgreSQL migration/readiness,
database-down behavior, and the frontend proxy against the Spring Boot service still
need to be verified in an environment with Docker.
