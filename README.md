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
├── Dockerfile            builds the backend into a container image
├── docker-compose.yml    PostgreSQL 16, pgAdmin, and the app
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

### 2. Start the database (and pgAdmin)

The repo has a `docker-compose.yml` with three services:

| Service | What it is | URL / port |
|---|---|---|
| `db` | PostgreSQL 16 | `localhost:5434`, database `ItHelpDesk`, user `user` / `pwd` |
| `pgadmin` | Web UI to look at the database | http://localhost:9000 (`admin@hackathon2026.com` / `adminpassword`) |
| `app` | The Spring Boot app in a container (optional) | http://localhost:8080 |

```bash
cp .env.example .env        # optional: change values in .env
docker compose up -d        # starts db + pgadmin (not the app)
```

In pgAdmin, add a server with host `db`, port `5432`, user `user`, password `pwd`.

### 3. Run the backend

Option A, while developing: run `ItHelpDeskApplication` in IntelliJ, or from the repo root:

```bash
./gradlew :backend:bootRun          # Windows: gradlew.bat :backend:bootRun
```

If Docker Desktop is running, Spring starts `db` and `pgadmin` for you.

Option B, everything in Docker (for the demo):

```bash
docker compose --profile app up -d --build
```

Stop the IntelliJ run first, because both use port 8080.

The API runs on http://localhost:8080.

Run the tests (they use an in-memory H2 database, no Docker needed):

```bash
./gradlew :backend:test
```

Seed data: put SQL inserts in `backend/src/main/resources/data.sql`. It runs on every start, after Hibernate creates the tables.

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
| GET | `/api/tickets/{id}` | One ticket |
| GET | `/api/tickets/queue` | Agent queue (open tickets not grouped into an incident) |
| PATCH | `/api/tickets/{id}` | Update status, priority or assignee |
| GET | `/api/tickets/{id}/history` | Ticket timeline |
| GET | `/api/incidents` | List incidents with affected counts |
| GET | `/api/incidents/active` | Active outages for the portal banner |
| PATCH | `/api/incidents/{id}/resolve` | Resolve an incident and all its tickets |
| GET | `/api/sla/summary` | SLA counts for the dashboard |
| GET | `/api/sla/alerts` | Tickets at risk or breached |

Details, examples and error format:
- [docs/outage-radar.md](docs/outage-radar.md): incident clustering, banner, duplicate blocking, errors
- [docs/sla-and-audit.md](docs/sla-and-audit.md): SLA timers, ticket updates, history timeline

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
