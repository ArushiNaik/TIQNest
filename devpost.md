# TIQNest 

**Project name:** TIQNest
**Tagline:** The IT help desk that spots outages before your inbox does, and fixes the easy stuff in one click.
**Team:** Malick Konate, Ilian Adeleke, Arushi Naik, Jacky Bolap  
**GitHub:** https://github.com/Malick-Konate/hackathon-2026

**Image:** [img.png](img.png)

---

## Inspiration
In a school or a small company, when the Wi-Fi or the VPN goes down, 40 people open 40 tickets for the same thing. The one IT person (or the teacher who "knows computers") drowns in duplicates, and the real individual problems get buried. Meanwhile, half the tickets are things users could fix themselves in 30 seconds, like flushing DNS or reconnecting a VPN, if someone showed them safely how.

## What it does
TIQNest is a help desk with two smart layers on top of normal tickets:

- **Outage Radar.** When 3 similar tickets (same category and affected system) arrive within 5 minutes, TIQNest groups them into one master incident, shows a live banner to every user ("VPN outage detected, no need to open a new ticket"), and blocks the same person from filing the same report twice. Agents see one incident instead of 40 tickets, and resolving it closes every linked ticket at once.
- **TiqFix, one-click safe remediation.** Describe your problem and TiqFix matches it to a vetted fix from a fixed catalogue: VPN, printer, DNS, browser cache. Each fix has a safety level (Safe, Low risk, IT only), a one-click Windows Settings shortcut or a copy-exact command, step-by-step instructions, and a "did it work?" button. If it didn't, you escalate to IT in one click. TiqFix never invents commands and never runs anything on your computer for you.
- **SLA timers and audit trail.** Every ticket gets a deadline from its priority (Critical 1 h, High 4 h, Medium 24 h, Low 72 h) shown as a badge that goes green, orange, red. Every change (created, assigned, priority, status, linked to incident) is recorded and shown as a timeline.
- **Ops dashboard** for admins, with SLA counts, active incidents, ticket details and history, and a demo button that simulates an outage live.

## How we built it
- **Backend:** Java 21, Spring Boot 4, Spring Data JPA, Bean Validation, MapStruct, Lombok, Gradle. One monolith split into feature packages (ticket, incident, sla, audit, remediation, user), each with data, business and presentation layers.
- **Database:** PostgreSQL 16 in Docker Compose (plus pgAdmin), seeded with demo users and tickets.
- **Frontend:** React, TypeScript, Vite, React Bootstrap.
- **Testing:** JUnit tests for the TiqFix matcher and controller, Postman collection for the API.
- We worked in feature branches with pull requests into main.

## Challenges we ran into
- Defining "similar" tickets so the radar is predictable during a live demo: we chose exact category plus normalized system name over fuzzy text matching.
- Making solving genuinely simple: browsers can't (and shouldn't) run commands, so every fix is either a Windows Settings deep link or a copy-exact command the user runs themselves, validated against an allow-list.
- SLA status is computed on every read instead of stored, so it is always current without a scheduled job.
- Merging four people's work in one day.

## Accomplishments that we're proud of
A full working flow end to end: log in, report, watch an outage get detected live, fix a common problem without IT, and see every action in the history.

## What we learned
Layered Spring Boot architecture, JPA relationships, React with TypeScript, Docker Compose, Git branching and PR reviews as a team, and how to use AI coding assistants critically.

## What's next for TIQNest
- **"Show, Don't Type":** upload a screenshot of the error and a vision AI pre-fills the ticket (category, system, error code). This was our third pitched feature; we designed it but did not finish it today.
- Smarter similarity (keywords in titles), real-time banner with WebSockets, real auth (Spring Security/JWT), more TiqFix remedies for macOS.

## Third-party tools
Spring Boot, React, Vite, React Bootstrap, PostgreSQL, Docker, MapStruct, Lombok. All application logic (outage detection, SLA rules, audit log, remediation matching and catalogue, UI) was written by our team during the hackathon. Before 9:30 AM the repo only had the README plan and an empty Spring Initializr project.

## AI Use Statement
We used Claude (chat and Claude Code) as a co-pilot: to plan the architecture, explain Spring Boot and Docker concepts step by step, scaffold files that we then read and edited, debug build and merge errors, and prepare our pitch.  
