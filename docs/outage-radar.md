# Outage Radar

> When several people report the same problem at the same time, it's probably not their laptop. It's an outage.

The Outage Radar watches incoming tickets. When **3 similar tickets arrive within 5 minutes**, it groups them into one **master incident**, shows a **status banner** on the portal, and keeps duplicate tickets from flooding the agent queue.

---

## The problem

When the VPN (or email, Wi-Fi...) goes down, 40 people open 40 tickets for the same thing. Agents waste time triaging duplicates, the real individual issues get buried, and users keep submitting because nobody told them "we know, we're on it".

## What it does

| | Behavior |
|---|---|
| **Detect** | Every new ticket is checked. If at least **3** tickets with the same **category + affected system** were created in the last **5 minutes**, a master incident is created and all of them are linked to it. |
| **Group** | Once an incident is active, every new matching ticket joins it automatically (`status = LINKED_TO_INCIDENT`) and the incident's `ticketCount` goes up. |
| **Warn** | `GET /api/incidents/active` returns the active outages with a ready-to-display `bannerMessage`. The portal shows it so users don't open more tickets. |
| **Block duplicates** | The same person reporting the same category + system again within 5 minutes gets **409 Conflict** and no ticket is created. |
| **Clean queue** | `GET /api/tickets/queue` only returns `OPEN` / `IN_PROGRESS` tickets, so outage tickets stay out of the agents' way. |
| **Resolve once** | `PATCH /api/incidents/{id}/resolve` closes the incident **and** marks every linked ticket `RESOLVED` in one call. The banner disappears. |

### What counts as "similar"?

Same `category` **and** same `affectedSystem`. The system name is normalized (trimmed + uppercased), so `"vpn"`, `"VPN "` and `"Vpn"` all match. It's intentionally simple and predictable rather than fuzzy text matching.

---

## Flow

```
POST /api/tickets
   │
   ├─ same person + same problem in last 5 min? ──► 409 Conflict (no ticket)
   │
   ├─ save ticket
   │
   └─ OutageRadarService.scan(ticket)
         │
         ├─ ACTIVE incident for this category+system? ──► link ticket, ticketCount++
         │
         └─ else count unlinked similar tickets in last 5 min
               ├─ < 3  ──► nothing, ticket stays OPEN in the queue
               └─ ≥ 3  ──► create Incident, link all of them  (log: OUTAGE DETECTED)
```

Everything happens in the same transaction as the ticket creation, so the response already contains the `incidentId` when the ticket was grouped.

---

## API

### Tickets — `/api/tickets`

| Method | URL | Description |
|---|---|---|
| `POST` | `/api/tickets` | Create a ticket (runs the radar). `201`, `400`, `409` |
| `GET` | `/api/tickets` | All tickets, newest first |
| `GET` | `/api/tickets/queue` | Agent queue: only `OPEN` / `IN_PROGRESS` tickets |
| `GET` | `/api/tickets/{id}` | One ticket. `404` if unknown |

**Create request**
```json
{
  "title": "VPN won't connect",
  "description": "Optional details",
  "category": "VPN",
  "affectedSystem": "vpn",
  "requesterEmail": "alice@corp.com"
}
```
`category` is one of: `NETWORK, EMAIL, VPN, PRINTER, SOFTWARE, HARDWARE, ACCOUNT, OTHER`.

**Ticket response**
```json
{
  "id": 3,
  "title": "VPN won't connect",
  "description": null,
  "category": "VPN",
  "affectedSystem": "VPN",
  "requesterEmail": "carol@corp.com",
  "status": "LINKED_TO_INCIDENT",
  "createdAt": "2026-11-14T16:02:11Z",
  "incidentId": 1
}
```
`incidentId` is `null` for a normal ticket.

### Incidents — `/api/incidents`

| Method | URL | Description |
|---|---|---|
| `GET` | `/api/incidents/active` | Active outages for the banner. Empty list = no banner |
| `GET` | `/api/incidents` | All incidents (active + resolved) |
| `PATCH` | `/api/incidents/{id}/resolve` | Resolve the incident and all its tickets. `404` unknown, `409` already resolved |

**Incident response**
```json
{
  "id": 1,
  "title": "Possible outage: VPN (VPN)",
  "category": "VPN",
  "affectedSystem": "VPN",
  "status": "ACTIVE",
  "ticketCount": 5,
  "detectedAt": "2026-11-14T16:02:11Z",
  "lastReportAt": "2026-11-14T16:04:40Z",
  "resolvedAt": null,
  "bannerMessage": "⚠ VPN outage detected: 5 reports. Our team is working on it, no need to open a new ticket."
}
```

### Errors

Every error uses the same shape (`config/GlobalExceptionHandler`):
```json
{
  "timestamp": "2026-11-14T16:05:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Some fields are invalid",
  "path": "/api/tickets",
  "fieldErrors": { "requesterEmail": "must be a well-formed email address" }
}
```
| Status | When |
|---|---|
| `400` | Invalid fields (`fieldErrors` lists them), malformed JSON, unknown category, bad id in URL |
| `404` | Ticket / incident / endpoint not found |
| `409` | Duplicate report from the same person, or incident already resolved |
| `500` | Unexpected error (details only in the server log) |

---

## Configuration

In `backend/src/main/resources/application.yaml`:
```yaml
outage-radar:
  threshold: 3        # similar tickets needed to declare an outage
  window-minutes: 5   # time window for the threshold AND for the duplicate check
```
Both have these defaults in code, so the block is optional. Lower `threshold` to `2` for a quicker live demo.

---

## Try it (PowerShell)

Start the app first (`.\gradlew :backend:bootRun`; Postgres starts from docker-compose).

```powershell
# 1. Three people report the VPN -> the 3rd response has incidentId = 1
foreach ($u in "alice","bob","carol") {
  Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/tickets -ContentType "application/json" `
    -Body "{`"title`":`"VPN down`",`"category`":`"VPN`",`"affectedSystem`":`"vpn`",`"requesterEmail`":`"$u@corp.com`"}"
}

# 2. Banner
Invoke-RestMethod http://localhost:8080/api/incidents/active

# 3. Alice again -> 409 (wrapped to see the message)
try {
  Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/tickets -ContentType "application/json" `
    -Body '{"title":"VPN still down","category":"VPN","affectedSystem":"VPN","requesterEmail":"alice@corp.com"}'
} catch { $_.ErrorDetails.Message }

# 4. Agent queue -> no VPN tickets in it
Invoke-RestMethod http://localhost:8080/api/tickets/queue

# 5. Fix it -> incident + tickets RESOLVED, banner gone
Invoke-RestMethod -Method Patch http://localhost:8080/api/incidents/1/resolve
Invoke-RestMethod http://localhost:8080/api/incidents/active
```
Tip: Windows PowerShell hides the body of 4xx responses; use `try { ... } catch { $_.ErrorDetails.Message }` or `curl.exe` to see it.

Reset the data between runs:
```powershell
docker compose exec -T db psql -U user -d ItHelpDesk -c "TRUNCATE tickets, incidents RESTART IDENTITY CASCADE;"
```

---

## For the frontend

- Poll `GET /api/incidents/active` every ~10 s. If the list is not empty, show a banner per incident with `bannerMessage`.
- After `POST /api/tickets`, if the response has an `incidentId`, tell the user their report was added to the known outage.
- On `409`, show `message` from the error body instead of a generic error.
- Agent dashboard: use `/api/tickets/queue` for the work list and `/api/incidents` for an outages panel with a "Resolve" button.
- CORS is open for `http://localhost:5173` (Vite dev server).

---

## Code map

```
backend/src/main/java/com/konate/ithelpdesk/
├── ticket/
│   ├── DataTier/          Ticket (entity), TicketCategory, TicketStatus, TicketRepository
│   ├── BusinessLayer/     TicketService     (duplicate check, calls the radar, queue)
│   ├── PresentationLayer/ TicketController, CreateTicketRequest, TicketResponse
│   └── Mapper/            TicketMapper      (MapStruct)
├── incident/
│   ├── DataLayer/         Incident (entity), IncidentStatus, IncidentRepository
│   ├── BusinessLayer/     OutageRadarService (detection), IncidentService (active list, resolve)
│   ├── PresentationLayer/ IncidentController, IncidentResponse
│   └── Mapper/            IncidentMapper    (MapStruct, builds bannerMessage)
└── config/
    ├── GlobalExceptionHandler  (one error format for all endpoints)
    └── ApiError
```

**Database** (created by Hibernate, `ddl-auto: update`):
- `tickets`: plus `incident_id` (FK → `incidents.id`, null when not grouped), indexed on `(category, affected_system, created_at)` for the radar query.
- `incidents`: `status`, `ticket_count`, `detected_at`, `last_report_at`, `resolved_at`.

---

## Known limits / next ideas

- **Concurrency:** two tickets arriving in the exact same instant could create two incidents for the same outage. Fine for a demo; a unique constraint or a lock would fix it in production.
- **Similarity is exact** (category + system). Next step: keyword matching on titles, or letting agents merge incidents manually.
- **Banner is polled**, not pushed. Server-Sent Events or WebSockets would make it instant.
- Resolved incidents don't auto-reopen; a new burst after resolution creates a new incident.
