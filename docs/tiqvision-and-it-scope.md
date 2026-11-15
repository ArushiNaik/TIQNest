# TiqVision, IT-scope validation and security hardening

## What it does

- **TiqVision** (`/vision`): an employee uploads a screenshot (or records 5 seconds of their screen). Claude reads
  the visible error and drafts a ticket. The employee edits it, then submits it or tries TiqFix. Nothing is created
  before they click Submit. Images stay in memory for the request only: they are never stored or logged.
- **IT-scope gate**: TiqVision, ticket creation and TiqFix free text classify every submission as `IT_SUPPORTED`,
  `IT_UNSUPPORTED`, `NON_IT`, `UNCERTAIN`, `SECURITY_RISK` or `INSUFFICIENT_INFO`. Non-IT requests get HTTP 422
  `NON_IT_ISSUE`. They create no ticket, get no fix and never reach TiqRadar. Employees may opt in to a human
  review, which is stored separately from tickets.
- **Security problems** (phishing, ransomware, stolen devices…) are always escalated, whatever the AI says, and
  employee tickets that mention them are raised to HIGH priority.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `ANTHROPIC_API_KEY` | empty | Enables Claude. Without it, text triage uses deterministic rules and screenshot analysis returns `AI_NOT_CONFIGURED`. |
| `ANTHROPIC_MODEL` | `claude-opus-5-5` | Vision-capable Claude model. |
| `ANTHROPIC_TIMEOUT_SECONDS` | `45` | Per-request timeout (2 retries). |
| `TIQNEST_DEMO_FIXTURES` | `false` | Shows a "Use labelled demo result" button. Its output is clearly marked as simulated. |
| `JPA_DDL_AUTO` | `update` | `update` keeps data across restarts. Set `create` to deliberately rebuild the schema. |
| `SQL_BIND_LOG_LEVEL` | `INFO` | `TRACE` logs SQL parameter values (passwords, ticket text). Debugging only. |

The key is read on the server only and is never sent to the browser.

### Windows + antivirus HTTPS scanning

If AI calls fail with `root cause SunCertPathBuilderException`, your antivirus (Avast, Kaspersky…) is re-signing
HTTPS traffic and Java does not trust its root certificate. Make the JVM use the Windows certificate store
(TLS verification stays on):

```powershell
$env:JAVA_TOOL_OPTIONS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
.\gradlew.bat :backend:bootRun
```

In IntelliJ, add `-Djavax.net.ssl.trustStoreType=Windows-ROOT` to the run configuration's VM options.

## API

| Method & path | Who | Notes |
|---|---|---|
| `GET /api/triage/status` | signed in | `{aiConfigured, demoFixturesEnabled}` |
| `POST /api/triage/classify` `{description}` | signed in | Scope classification. 422 `NON_IT_ISSUE` for non-IT. |
| `POST /api/triage/analyze` (multipart `files`, `description`) | signed in | 1–4 PNG/JPEG/WebP images, 3.5 MB each, 10 MB total, checked from file bytes. 12 requests/min per user. |
| `POST /api/triage/reviews` `{text, classification, confirmed:true}` | signed in | Opt-in human review. One per text per day, max 3 open. |
| `GET /api/triage/reviews` / `PATCH /api/triage/reviews/{id}/close` | admin (employees see their own) | |

Error codes the frontend handles: `NON_IT_ISSUE` (422), `BLANK_UPLOAD`, `TOO_MANY_FILES`, `UNSUPPORTED_IMAGE` (415),
`IMAGE_TOO_LARGE`/`UPLOAD_TOO_LARGE` (413), `AI_NOT_CONFIGURED`/`AI_UNAVAILABLE` (503), `AI_BAD_RESPONSE` (502),
`RATE_LIMITED` (429).

## Authorization changes (backend-enforced)

- `GET /api/tickets`: employees get only their own tickets; admins get all.
- `GET /api/tickets/{id}`, `/history`, remediation history and attempts: owner or admin, otherwise 403.
- `PATCH /api/tickets/{id}`, `GET /api/tickets/queue`, `/api/sla/*`, `GET /api/incidents`,
  `PATCH /api/incidents/{id}/resolve`: admin only. The audit actor is the signed-in admin.
- `POST /api/tickets`: employees always file as themselves (the body's `requesterEmail` is ignored). Admins may file
  on behalf of others (used by the dashboard's outage simulator).
- Passwords: new accounts are BCrypt-hashed. Existing plaintext passwords (the seed users) still work and are
  re-hashed on the next successful login.

## TiqRadar changes

- Counts **distinct employees** (default 3 within 5 minutes), not raw tickets.
- Affected systems are normalized: `VPN`, `vpn client`, `GlobalProtect VPN` → `VPN`; `Outlook`/`Exchange` →
  `OUTLOOK`; `Wi-Fi`/`wireless` → `WIFI`. Generic names (`laptop`, `unknown`…) are never grouped.
- Concurrent reports: scans for the same outage run one at a time until commit, and a unique
  `incidents.active_key` column makes duplicate active incidents impossible at the database level.

## Manual smoke test

1. Sign in as `Bahman@demo.com` / `pwd`.
2. Open **TiqVision**, upload a VPN error screenshot, click **Analyze**.
3. Check the drafted ticket: title, category VPN, visible error code, "Possible cause (not confirmed)".
4. Click **Try TiqFix** → "Recommended fix: Check Windows VPN Settings" → **Open VPN Settings**.
5. Click **Yes, problem solved** → "Resolution reported successfully." appears in history.
6. Back in TiqVision, submit the ticket. Then, as admin (`Christine@demo.com`), use **Dashboard → Simulate outage → VPN outage**.
7. The outage banner appears for everyone. The employee's submit message says the report joined the incident.
8. As admin, open the Dashboard, check the incident and its linked tickets, and click **Mark resolved**.
9. As the employee, type "Can you order pizza?" in TiqVision → "This doesn't look like an IT support issue."
10. No ticket, incident or remediation attempt was created.
11. Click **Request IT admin review**: a confirmation appears first. After confirming, the request shows under
    **Dashboard → Review requests** (admin), and still no ticket exists.
