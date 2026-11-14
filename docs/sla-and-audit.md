# SLA timers and ticket history

Two features built on top of tickets:

- **SLA timers:** every ticket gets a deadline from its priority. The API tells you, live, whether it is on track, at risk, breached or met, so the UI can show a countdown that turns orange and then red.
- **History timeline (audit log):** every change to a ticket is recorded (who, what, before, after, when) and returned as a timeline.

---

## SLA

### Rules

| Priority | Deadline after creation |
|---|---|
| `CRITICAL` | 1 hour |
| `HIGH` | 4 hours |
| `MEDIUM` (default) | 24 hours |
| `LOW` | 72 hours |

- `priority` is optional on `POST /api/tickets`; it defaults to `MEDIUM`.
- When the priority changes (`PATCH`), the deadline is recalculated **from the original creation time**, not from now.

### SLA status

`slaStatus` is computed on every read, never stored, so it is always current without any scheduled job.

| `slaStatus` | Meaning | Suggested color |
|---|---|---|
| `ON_TRACK` | Open, more than 25% of the time left | green |
| `AT_RISK` | Open, less than 25% of the time left | orange |
| `BREACHED` | Deadline passed while open, or resolved late | red |
| `MET` | Resolved or closed before the deadline | grey / green check |

A ticket counts as finished when its status is `RESOLVED` or `CLOSED`. `resolvedAt` is set at that moment (and cleared if the ticket is reopened).

### Endpoints

| Method | URL | Description |
|---|---|---|
| `GET` | `/api/sla/summary` | Counts per SLA status for the dashboard tiles |
| `GET` | `/api/sla/alerts` | Open tickets that are `AT_RISK` or `BREACHED`, most urgent deadline first |

```json
GET /api/sla/summary
{ "onTrack": 12, "atRisk": 3, "breached": 1, "met": 20, "total": 36 }
```
`/api/sla/alerts` returns the usual ticket objects (see below).

### Configuration

`backend/src/main/resources/application.yaml`:
```yaml
sla:
  hours:
    critical: 1
    high: 4
    medium: 24
    low: 72
```
Tip for a live demo: set `critical: 0` so a new `CRITICAL` ticket is breached immediately.

---

## Ticket fields

Every ticket response now looks like this:
```json
{
  "id": 7,
  "title": "Server room too hot",
  "description": null,
  "category": "HARDWARE",
  "affectedSystem": "SERVER ROOM",
  "requesterEmail": "dave@corp.com",
  "status": "IN_PROGRESS",
  "priority": "CRITICAL",
  "assignee": "bob",
  "createdAt": "2026-11-14T17:00:00Z",
  "slaDeadline": "2026-11-14T18:00:00Z",
  "slaStatus": "AT_RISK",
  "resolvedAt": null,
  "incidentId": null
}
```

---

## Updating a ticket

`PATCH /api/tickets/{id}`. Send only the fields you want to change.

```json
{
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "assignee": "bob",
  "actor": "admin@corp.com"
}
```

| Field | Notes |
|---|---|
| `status` | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`. `LINKED_TO_INCIDENT` is refused (400): only the outage radar sets it. |
| `priority` | Recalculates `slaDeadline`. |
| `assignee` | Any name or email. Send `""` to unassign. |
| `actor` | Who made the change, shown in the history. Defaults to `system`. |

Fields that don't change are ignored and don't create history entries. Returns the updated ticket. `404` if the ticket doesn't exist.

---

## History timeline (audit log)

`GET /api/tickets/{id}/history` returns every event for the ticket, oldest first. `404` if the ticket doesn't exist.

```json
[
  { "id": 1, "ticketId": 7, "action": "CREATED",          "oldValue": null,          "newValue": "OPEN",        "actor": "dave@corp.com", "message": "Ticket created with priority MEDIUM, SLA deadline 2026-11-15T17:00:00Z", "createdAt": "..." },
  { "id": 4, "ticketId": 7, "action": "PRIORITY_CHANGED", "oldValue": "MEDIUM",      "newValue": "CRITICAL",    "actor": "admin@corp.com", "message": "Priority changed from MEDIUM to CRITICAL, new SLA deadline 2026-11-14T18:00:00Z", "createdAt": "..." },
  { "id": 5, "ticketId": 7, "action": "ASSIGNED",         "oldValue": null,          "newValue": "bob",         "actor": "admin@corp.com", "message": "Assigned to bob", "createdAt": "..." },
  { "id": 6, "ticketId": 7, "action": "STATUS_CHANGED",   "oldValue": "OPEN",        "newValue": "IN_PROGRESS", "actor": "bob",            "message": "Status changed from OPEN to IN_PROGRESS", "createdAt": "..." }
]
```
`message` is ready to display as-is in the timeline.

### What gets recorded

| `action` | When | `actor` |
|---|---|---|
| `CREATED` | `POST /api/tickets` | the requester's email |
| `STATUS_CHANGED` | `PATCH` with a new status, or the ticket's incident is resolved | `actor` from the request, or `system` |
| `PRIORITY_CHANGED` | `PATCH` with a new priority | `actor` from the request |
| `ASSIGNED` | `PATCH` with a new assignee (or `""` to unassign) | `actor` from the request |
| `LINKED_TO_INCIDENT` | The outage radar groups the ticket into an incident | `system` |

Events are written in the same transaction as the change, so a failed update never leaves a stray history entry.

---

## Try it (PowerShell)

```powershell
$api = "http://localhost:8080/api"

# create a CRITICAL ticket
$t = Invoke-RestMethod -Method Post -Uri "$api/tickets" -ContentType "application/json" `
  -Body '{"title":"Server room too hot","category":"HARDWARE","affectedSystem":"server room","requesterEmail":"dave@corp.com","priority":"CRITICAL"}'
$t | Select-Object id, priority, slaDeadline, slaStatus

# assign it and start working on it
Invoke-RestMethod -Method Patch -Uri "$api/tickets/$($t.id)" -ContentType "application/json" `
  -Body '{"assignee":"bob","status":"IN_PROGRESS","actor":"admin@corp.com"}'

# resolve it
Invoke-RestMethod -Method Patch -Uri "$api/tickets/$($t.id)" -ContentType "application/json" `
  -Body '{"status":"RESOLVED","actor":"bob"}'

# timeline + SLA dashboard
Invoke-RestMethod "$api/tickets/$($t.id)/history" | Format-Table action, oldValue, newValue, actor, message -AutoSize
Invoke-RestMethod "$api/sla/summary"
Invoke-RestMethod "$api/sla/alerts"
```

---

## For the frontend

- **Countdown:** use `slaDeadline` for the timer and `slaStatus` for the color. Re-fetch the list every 30 to 60 s so the status changes from green to orange to red without a page reload.
- **Dashboard tiles:** `GET /api/sla/summary`. **"Needs attention" list:** `GET /api/sla/alerts`.
- **Ticket detail page:** `GET /api/tickets/{id}/history` and render each `message` with its `createdAt` and `actor`.
- **Edit form:** `PATCH /api/tickets/{id}` with only the changed fields and `actor` = the logged-in user.
- `frontend/src/types/models.ts` needs a few updates to match the backend: ids are numbers (`id`, not `ticketId`), `TicketStatus` also has `LINKED_TO_INCIDENT`, the assignee field is `assignee` (a name or email, not a user id), and there are new fields `slaStatus`, `resolvedAt`, `category`, `affectedSystem`, `requesterEmail`.

---

## Code map

```
backend/src/main/java/com/konate/ithelpdesk/
├── sla/
│   ├── SlaPolicy            deadline per priority (config) + static statusOf(ticket)
│   ├── SlaStatus            ON_TRACK, AT_RISK, BREACHED, MET
│   ├── SlaService           summary counts, alerts list
│   ├── SlaController        /api/sla/summary, /api/sla/alerts
│   └── SlaSummaryResponse
├── audit/
│   ├── DataLayer/           AuditEvent (entity), AuditAction, AuditEventRepository
│   ├── BusinessLayer/       AuditService (record + history)
│   ├── PresentationLayer/   AuditController (/api/tickets/{id}/history), AuditEventResponse
│   └── Mapper/              AuditEventMapper (MapStruct)
└── ticket/
    ├── DataTier/            Ticket (+ priority, slaDeadline, assignee, resolvedAt), TicketPriority
    ├── BusinessLayer/       TicketService (create, update + audit calls)
    └── PresentationLayer/   UpdateTicketRequest, TicketController (PATCH /{id})
```
Audit hooks also live in `incident/BusinessLayer/OutageRadarService` (grouping) and `IncidentService.resolve()` (bulk resolve).

**Database:** new table `audit_events` (`ticket_id`, `action`, `old_value`, `new_value`, `actor`, `message`, `created_at`, indexed on `ticket_id, created_at`). New columns on `tickets`: `priority`, `sla_deadline`, `assignee`, `resolved_at`.

## Known limits

- No real authentication yet: `actor` is whatever the client sends.
- SLA counts wall-clock time (no business hours, no pause while waiting on the user).
- `/api/sla/summary` and `/alerts` load all tickets in memory. Fine for a demo; with thousands of tickets this would move to a SQL query.
