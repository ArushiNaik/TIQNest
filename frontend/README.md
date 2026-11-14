# Frontend (TIQNest)

React + Vite + TypeScript, with React Bootstrap.

## Run it

1. Start the database: `docker compose up -d` (repo root).
2. Start the backend on port 8080 (IntelliJ, or `./gradlew :backend:bootRun` from the repo root).
3. Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The backend only accepts requests from port 5173 (CORS), so stop any other dev server using that port first.

## Demo logins (from `backend/src/main/resources/data.sql`)

| Role | Email | Password |
|---|---|---|
| Admin | Crhistine@demo.com | pwd |
| Employee | Amin@demo.com | pwd |
| Employee | Bahaman@demo.com | pws |

Emails are case-sensitive for now.

## Where things live

- `src/services/config.ts`: every backend URL.
- `src/services/api-services.ts`: `getData` / `postData`, which add the `Authorization: Bearer <token>` header.
- `src/services/auth.ts`: the signed-in session (token + user) in localStorage.
- `src/types/models.ts`: interfaces that mirror the backend DTOs. Keep the field names identical to the Java records.

Sessions are kept in the backend's memory, so restarting the backend signs everyone out.
