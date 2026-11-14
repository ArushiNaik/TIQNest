import { Badge, Card } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import type { Ticket, TicketStatus } from '../types/models';

const statusVariant: Record<TicketStatus, string> = {
    OPEN: 'primary',
    IN_PROGRESS: 'warning',
    LINKED_TO_INCIDENT: 'danger',
    RESOLVED: 'success',
    CLOSED: 'dark',
};

export function formatLabel(value: string): string {
    return value.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
}

export function formatDate(value: string | null | undefined): string {
    if (!value) return 'unknown date';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? 'unknown date' : date.toLocaleString();
}

export default function TicketCard({ ticket }: { ticket: Ticket }) {
    return (
        <Card className="ticket-card">
            <Card.Body>
                <div className="d-flex justify-content-between align-items-start mb-2">
                    <Card.Title className="mb-0">{ticket.title}</Card.Title>
                    <div className="d-flex gap-1">
                        <Badge bg="secondary">{formatLabel(ticket.category)}</Badge>
                        <Badge bg={statusVariant[ticket.status] ?? 'secondary'}>{formatLabel(ticket.status)}</Badge>
                    </div>
                </div>
                {ticket.description && <Card.Text>{ticket.description}</Card.Text>}
                <Card.Text className="text-muted mb-1">💻 {ticket.affectedSystem}</Card.Text>
                <div className="d-flex justify-content-between align-items-end gap-2">
                    <Card.Text className="text-muted small mb-0">
                        Ticket #{ticket.id} · Reported {formatDate(ticket.createdAt)}
                        {ticket.incidentId != null && <> · Part of incident #{ticket.incidentId}</>}
                    </Card.Text>
                    {ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED' && (
                        <Link
                            to={`/fix?ticketId=${ticket.id}`}
                            className="btn btn-sm btn-outline-primary text-nowrap"
                            aria-label={`Try TiqFix for ticket ${ticket.id}`}
                        >
                            🛠️ Try TiqFix
                        </Link>
                    )}
                </div>
            </Card.Body>
        </Card>
    );
}
