# IT Platform

A simple IT helpdesk for small companies that don't have an IT department. Employees report problems, and admins see everything on a live dashboard.

Built during a one-day hackathon. Demo data is fake.

## What makes it different

- **Incident clustering:** when several people report the same problem, tickets are grouped into one incident ("12 people affected, 2nd floor") instead of 12 separate tickets.
- **SLA timers:** every ticket has a deadline based on its priority, shown as a countdown that turns orange and then red.
- **History timeline:** every change to a ticket is recorded and shown as a timeline.

## Features

- Seeded login with two roles (employee, admin)
- Create, list and update tickets (status, priority, assignee)
- Automatic grouping of similar tickets into incidents
- Admin dashboard with incidents, tickets and SLA status
- Ticket history timeline
- Demo button that submits several reports to show clustering

## Tech stack

| Part | Technology |
|---|---|
| Backend | Java, Spring Boot |
| Frontend | React (TypeScript, kept simple) |
| Database | PostgreSQL |
| Optional | Docker Compose for the database |

## Project structure

```
.
├── backend/              Spring Boot API (Java 21)
├── settings.gradle       Gradle root; includes backend/
├── gradlew               Gradle wrapper (run from the repo root)
├── frontend/             React app (Vite + TypeScript)
├── docker-compose.yml    PostgreSQL 16
├── .env.example          copy to .env for local DB settings
└── README.md
```

Backend modules (inside one app, grouped by feature, in
`backend/src/main/java/com/konate/ithelpdesk/`):

```
ticket/      create, list, update tickets
incident/    groups tickets into incidents
sla/         deadlines and timers
audit/       change history
user/        users and roles
config/      Spring configuration (CORS, security, seed data)
```

## Getting started

### Prerequisites

- Java 21 (or the version used when the project was generated)
- Node.js 20 or newer
- PostgreSQL 15 or newer, or Docker

### 1. Clone the repository

```bash
git clone <repository-url>
cd <repository-folder>
```

### 2. Start the database

Option A: Docker (recommended). The repo has a `docker-compose.yml` with a PostgreSQL 16 service.

```bash
cp .env.example .env        # optional: change values in .env
docker compose up -d db     # start Postgres in the background
docker compose ps           # wait until STATUS shows "(healthy)"
```

Connect with `docker exec -it itplatform-db psql -U itplatform -d itplatform`.
Stop it with `docker compose down` (data is kept in the `db-data` volume; `docker compose down -v` wipes it).

Option B: use a local PostgreSQL install and create a database named `itplatform`.

### 3. Run the backend

The database settings are already in `backend/src/main/resources/application.yaml`
(they match `.env.example`). If Docker is running, the app starts the `db` container for you.

From the repo root:

```bash
./gradlew :backend:bootRun          # Windows: gradlew.bat :backend:bootRun
```

Or in IntelliJ: open `ItHelpDeskApplication` and click the green Run arrow.

The API runs on http://localhost:8080.

Run the tests (they use an in-memory H2 database, no Docker needed):

```bash
./gradlew :backend:test
```

### 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The app runs on http://localhost:5173.

## Demo users

| Role | Name |
|---|---|
| Employee | (set in the seed data) |
| Admin | (set in the seed data) |

## API overview

| Method | Path | Purpose |
|---|---|---|
| GET, POST | `/api/tickets` | List and create tickets |
| PATCH | `/api/tickets/{id}` | Update status or assignee |
| GET | `/api/incidents` | List incidents with affected counts |
| GET | `/api/tickets/{id}/history` | Ticket timeline |

## Team workflow

- `main` always runs. Don't push broken code to it.
- One feature per branch: `feature/<name>`.
- Open a pull request, get one review, then merge.
- Pull from `main` before starting new work.
- Split work by folder (backend vs frontend) to avoid merge conflicts.

## Conventions

- Keep TypeScript simple: plain interfaces and functions, no advanced types.
- Use DTOs between the API and the database entities.
- Never commit passwords or API keys. Use environment variables or a local `.env` file (it is in `.gitignore`).

## Team

- Backend:
- Frontend:
- Database and data:
- Demo and pitch:
