import { Badge, Button, Card } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import type { Ticket, TicketPriority, TicketStatus } from '../../types/models';
import SlaBadge from '../dashboard/SlaBadge';
import { FeatureIcon } from '../../brand';

const statusVariant: Record<TicketStatus, string> = {
    OPEN: 'primary',
    IN_PROGRESS: 'warning',
    LINKED_TO_INCIDENT: 'danger',
    RESOLVED: 'success',
    CLOSED: 'dark',
};

const priorityVariant: Record<TicketPriority, string> = {
    LOW: 'light',
    MEDIUM: 'secondary',
    HIGH: 'warning',
    CRITICAL: 'danger',
};

export function formatLabel(value: string): string {
    return value.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
}

export function formatDate(value: string | null | undefined): string {
    if (!value) return 'unknown date';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? 'unknown date' : date.toLocaleString();
}

export default function TicketCard({ ticket, onOpen }: { ticket: Ticket; onOpen?: (id: number) => void }) {
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
                <div className="d-flex flex-wrap gap-1 mb-2">
                    {ticket.priority && (
                        <Badge bg={priorityVariant[ticket.priority]} text={ticket.priority === 'LOW' || ticket.priority === 'HIGH' ? 'dark' : undefined}>
                            {formatLabel(ticket.priority)} priority
                        </Badge>
                    )}
                    <SlaBadge ticket={ticket} />
                    {ticket.assignee && <Badge bg="info" text="dark">👤 {ticket.assignee}</Badge>}
                </div>
                {ticket.description && <Card.Text>{ticket.description}</Card.Text>}
                <Card.Text className="text-muted mb-1">💻 {ticket.affectedSystem}</Card.Text>
                <div className="d-flex justify-content-between align-items-end gap-2">
                    <Card.Text className="text-muted small mb-0">
                        Ticket #{ticket.id} · Reported {formatDate(ticket.createdAt)}
                        {ticket.incidentId != null && <> · Part of incident #{ticket.incidentId}</>}
                    </Card.Text>
                    <div className="d-flex gap-2">
                        {onOpen && (
                            <Button size="sm" variant="outline-secondary" className="text-nowrap" onClick={() => onOpen(ticket.id)}>
                                Details
                            </Button>
                        )}
                        {ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED' && (
                            <Link
                                to={`/fix?ticketId=${ticket.id}`}
                                className="btn btn-sm btn-outline-primary text-nowrap"
                                aria-label={`Try TiqFix for ticket ${ticket.id}`}
                            >
                                <FeatureIcon feature="fix" className="me-1" />Try TiqFix
                            </Link>
                        )}
                    </div>
                </div>
            </Card.Body>
        </Card>
    );
}
