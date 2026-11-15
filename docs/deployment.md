# Deployment

Netlify can only host the **frontend** (static files). The **backend** (Spring Boot) and **Postgres** must run on a
platform that runs servers, e.g. [Render](https://render.com) (free tier) or Railway.

```
Browser ──> Netlify (React)  ──VITE_API_BASE_URL──>  Render (Spring Boot, Dockerfile)  ──>  Postgres
```

## 1. Database

Create a managed Postgres (Render Postgres or Neon). Render shows a URL like
`postgres://user:password@host:5432/dbname`; Spring needs it split into the three variables below, in JDBC form.

## 2. Backend (Render example)

New **Web Service** → connect this repo → **Runtime: Docker** (uses the root `Dockerfile`) → root directory: repo root.

| Variable | Example |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://host:5432/dbname` |
| `SPRING_DATASOURCE_USERNAME` | `user` |
| `SPRING_DATASOURCE_PASSWORD` | `password` |
| `CORS_ALLOWED_ORIGINS` | `https://your-site.netlify.app` (comma-separate several; add `http://localhost:5173` for local dev against it) |
| `ANTHROPIC_API_KEY` | optional, enables TiqVision AI |

`PORT` is set by Render automatically. Check: `https://your-backend.onrender.com/api/triage/status` returns **401**
(up, asking for login). The demo users from `data.sql` are created on first start.

Free Render services sleep when idle: the first request afterwards can take 30-60 s.

## 3. Frontend (Netlify)

`netlify.toml` at the repo root sets the base directory (`frontend`), build command and the SPA redirect.

In **Site configuration → Environment variables** add:

| Variable | Value |
|---|---|
| `VITE_API_BASE_URL` | `https://your-backend.onrender.com/api` |

Then **Deploys → Trigger deploy → Clear cache and deploy site**. Vite bakes this value in at build time, so changing
it has no effect until you redeploy.

## Troubleshooting

| Symptom | Cause |
|---|---|
| "Cannot reach the server" on login | `VITE_API_BASE_URL` missing or not redeployed, or backend asleep / down |
| Browser console: blocked by CORS policy | Netlify URL missing from `CORS_ALLOWED_ORIGINS` (exact match, `https://`, no trailing slash) |
| 404 when refreshing `/vision` or `/fix` | `netlify.toml` not picked up (check the site's base directory settings) |
| Backend crashes on start | Wrong `SPRING_DATASOURCE_*` values (URL must start with `jdbc:postgresql://`) |
