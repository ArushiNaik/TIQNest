import { useEffect, useState } from 'react';
import { Badge } from 'react-bootstrap';
import type { SlaStatus, Ticket } from '../../types/models';

const variant: Record<SlaStatus, string> = {
    ON_TRACK: 'success',
    AT_RISK: 'warning',
    BREACHED: 'danger',
    MET: 'secondary',
};

function formatDuration(ms: number): string {
    const totalMinutes = Math.floor(Math.abs(ms) / 60000);
    const days = Math.floor(totalMinutes / 1440);
    const hours = Math.floor((totalMinutes % 1440) / 60);
    const minutes = totalMinutes % 60;
    if (days > 0) return `${days}d ${hours}h`;
    if (hours > 0) return `${hours}h ${minutes}m`;
    const seconds = Math.floor(Math.abs(ms) / 1000) % 60;
    return `${minutes}m ${seconds.toString().padStart(2, '0')}s`;
}

function liveStatus(ticket: Ticket, now: number): SlaStatus | null {
    if (!ticket.slaDeadline || !ticket.slaStatus) return ticket.slaStatus;
    if (ticket.slaStatus === 'MET' || ticket.resolvedAt) return ticket.slaStatus;
    const deadline = new Date(ticket.slaDeadline).getTime();
    const created = new Date(ticket.createdAt).getTime();
    if (now > deadline) return 'BREACHED';
    const total = deadline - created;
    return total > 0 && (deadline - now) * 4 < total ? 'AT_RISK' : 'ON_TRACK';
}

export default function SlaBadge({ ticket }: { ticket: Ticket }) {
    const [now, setNow] = useState(() => Date.now());

    useEffect(() => {
        if (ticket.resolvedAt || !ticket.slaDeadline) return;
        const timer = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(timer);
    }, [ticket.resolvedAt, ticket.slaDeadline]);

    const status = liveStatus(ticket, now);
    if (!status || !ticket.slaDeadline) return null;

    const deadline = new Date(ticket.slaDeadline).getTime();
    let label: string;
    if (status === 'MET') label = 'SLA met';
    else if (ticket.resolvedAt) label = 'Resolved late';
    else if (status === 'BREACHED') label = `Overdue by ${formatDuration(now - deadline)}`;
    else label = `${formatDuration(deadline - now)} left`;

    return (
        <Badge
            bg={variant[status]}
            text={status === 'AT_RISK' ? 'dark' : undefined}
            title={`SLA deadline: ${new Date(ticket.slaDeadline).toLocaleString()}`}
        >
            ⏱ {label}
        </Badge>
    );
}
