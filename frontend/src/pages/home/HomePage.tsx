import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getData } from '../../services/api-services';
import { getCurrentUser, isAdmin } from '../../services/auth';
import { API_URL_TICKETS } from '../../services/config';
import type { Ticket, TicketPriority } from '../../types/models';
import { formatLabel } from '../tiqradar/TicketCard';
import './HomePage.css';
import { FeatureIcon, Icon } from '../../brand';

const PRIORITY_RANK: Record<TicketPriority, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };

function pickTopTicket(tickets: Ticket[]): Ticket | null {
    const open = tickets.filter((t) => t.status !== 'RESOLVED' && t.status !== 'CLOSED');
    const deadline = (t: Ticket) => (t.slaDeadline ? new Date(t.slaDeadline).getTime() : Infinity);
    open.sort((a, b) =>
        PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority]
        || deadline(a) - deadline(b)
        || new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
    return open[0] ?? null;
}

function useTopTicket() {
    const [ticket, setTicket] = useState<Ticket | null>(null);
    useEffect(() => {
        let cancelled = false;
        const user = getCurrentUser();
        getData<Ticket[]>(API_URL_TICKETS)
            .then((data) => {
                const mine = isAdmin() ? data : data.filter((t) => t.requesterEmail.toLowerCase() === user?.email.toLowerCase());
                if (!cancelled) setTicket(pickTopTicket(mine));
            })
            .catch(() => !cancelled && setTicket(null));
        return () => { cancelled = true; };
    }, []);
    return ticket;
}

function useNow(intervalMs: number) {
    const [now, setNow] = useState(() => Date.now());
    useEffect(() => {
        const timer = window.setInterval(() => setNow(Date.now()), intervalMs);
        return () => window.clearInterval(timer);
    }, [intervalMs]);
    return now;
}

function formatDuration(ms: number): string {
    const minutes = Math.max(1, Math.round(ms / 60000));
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    if (hours < 48) return `${hours}h ${minutes % 60}m`;
    return `${Math.floor(hours / 24)}d ${hours % 24}h`;
}

function deadlineInfo(ticket: Ticket, now: number) {
    if (!ticket.slaDeadline) return null;
    const start = new Date(ticket.createdAt).getTime();
    const end = new Date(ticket.slaDeadline).getTime();
    const elapsed = end > start ? ((now - start) / (end - start)) * 100 : 100;
    const left = end - now;
    return {
        percent: Math.min(100, Math.max(0, Math.round(elapsed))),
        label: left >= 0 ? `${formatDuration(left)} left` : `Overdue by ${formatDuration(-left)}`,
        overdue: left < 0,
    };
}

const MARQUEE_ITEMS = ['Report it', 'Track it live', 'Outage radar', 'SLA timers', 'Full history', 'One-click fixes'];

function Bolt({ className }: { className: string }) {
    return (
        <svg className={className} viewBox="0 0 100 40" aria-hidden="true">
            <path d="M0 26 L44 10 L40 20 L100 6 L52 34 L57 23 Z" fill="currentColor" />
        </svg>
    );
}

export default function HomePage() {
    const [ticketVisible, setTicketVisible] = useState(true);
    const currentUser = getCurrentUser();
    const admin = isAdmin();
    const topTicket = useTopTicket();
    const now = useNow(30000);
    const deadline = topTicket ? deadlineInfo(topTicket, now) : null;

    return (
        <div className="home">
            <div className="home-rings" aria-hidden="true" />

            <Link to="/tickets" className="home-badge" aria-label="Go to tickets">
                <svg viewBox="0 0 120 120" className="home-badge-text" aria-hidden="true">
                    <defs>
                        <path id="badge-circle" d="M60,60 m-46,0 a46,46 0 1,1 92,0 a46,46 0 1,1 -92,0" />
                    </defs>
                    <text>
                        <textPath href="#badge-circle" textLength="286" lengthAdjust="spacing">REPORT · TRACK · RESOLVE · </textPath>
                    </text>
                </svg>
                <span className="home-badge-arrow" aria-hidden="true">↓</span>
            </Link>

            <Link to="/fix" className="home-side-tab" aria-label="Open TiqFix">
                <FeatureIcon feature="fix" />
            </Link>

            <div className="hero-stage">
                <h1 className="hero-title">
                    <span className="hero-line">IT help that</span>
                    <span className="hero-line">just works,</span>
                    <span className="hero-line hero-line-small">no fuss</span>
                </h1>

                {ticketVisible && topTicket && (
                    <div className={`hero-ticket hero-ticket-live${deadline?.overdue ? ' is-overdue' : ''}`} role="region" aria-label="Highest-priority open ticket">
                        <div className="hero-ticket-top">
                            <span>TIQ · {String(topTicket.id).padStart(3, '0')}</span>
                            <button type="button" className="hero-ticket-dismiss" aria-label="Hide this ticket" onClick={() => setTicketVisible(false)}>×</button>
                        </div>
                        <Link to="/tickets" className="hero-ticket-title" title={topTicket.title}>{topTicket.title}</Link>
                        <div className="hero-ticket-meta">
                            {formatLabel(topTicket.priority)} · {topTicket.incidentId != null ? `Part of incident #${topTicket.incidentId}` : formatLabel(topTicket.status)}
                        </div>
                        {deadline && (
                            <>
                                <div className="hero-ticket-bar" aria-hidden="true"><span style={{ width: `${deadline.percent}%` }} /></div>
                                <div className="hero-ticket-foot"><Icon name="bi-stopwatch" className="me-1" />{deadline.label}</div>
                            </>
                        )}
                    </div>
                )}
            </div>

            <p className="hero-subtitle">
                {currentUser ? `Welcome back, ${currentUser.name}. ` : ''}
                Report a problem, follow it live, and know about an outage before you even ask.
            </p>

            <div className="ctaContainer">
                {admin ? (
                    <Link to="/dashboard" className="mainButton">Open dashboard</Link>
                ) : (
                    <Link to="/tickets" className="mainButton">Report a problem</Link>
                )}
                <Link to={admin ? '/tickets' : '/fix'} className="mainButton secondaryButton">
                    {admin ? 'All tickets' : 'Try TiqFix'}
                </Link>
            </div>

            <div className="home-corner home-corner-left">
                <Bolt className="home-bolt" />
                <span>Fixed<br />in minutes</span>
            </div>
            <div className="home-corner home-corner-right">
                <span>Fewer duplicate<br />tickets</span>
                <Bolt className="home-bolt home-bolt-flip" />
            </div>

            <div className="home-marquee" aria-hidden="true">
                <div className="home-marquee-track">
                    {[0, 1].map((copy) => (
                        <div className="home-marquee-group" key={copy}>
                            {MARQUEE_ITEMS.map((item) => (
                                <span key={item}>{item} <i>✦</i></span>
                            ))}
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
