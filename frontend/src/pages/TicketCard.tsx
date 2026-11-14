import { Badge, Button, Card } from 'react-bootstrap';
import type { Ticket, TicketPriority, TicketStatus } from '../types/models';

const priorityVariant: Record<TicketPriority, string> = {
    LOW: 'secondary',
    MEDIUM: 'info',
    HIGH: 'warning',
    CRITICAL: 'danger',
};

const statusVariant: Record<TicketStatus, string> = {
    OPEN: 'primary',
    IN_PROGRESS: 'warning',
    RESOLVED: 'success',
    CLOSED: 'dark',
};

export function formatLabel(value: string): string {
    return value.replace('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
}

interface TicketCardProps {
    ticket: Ticket;
    canManage: boolean;
    onEdit: (ticket: Ticket) => void;
    onClose: (ticket: Ticket) => void;
}

export default function TicketCard({ ticket, canManage, onEdit, onClose }: TicketCardProps) {
    const isClosed = ticket.status === 'CLOSED';

    return (
        <Card className="ticket-card">
            <Card.Body>
                <div className="d-flex justify-content-between align-items-start mb-2">
                    <Card.Title className="mb-0">{ticket.title}</Card.Title>
                    <div className="d-flex gap-1">
                        <Badge bg={priorityVariant[ticket.priority]}>{formatLabel(ticket.priority)}</Badge>
                        <Badge bg={statusVariant[ticket.status]}>{formatLabel(ticket.status)}</Badge>
                    </div>
                </div>
                <Card.Text>{ticket.description}</Card.Text>
                {ticket.location && <Card.Text className="text-muted mb-1">📍 {ticket.location}</Card.Text>}
                <Card.Text className="text-muted small mb-1">
                    Ticket #{ticket.ticketId} · Reported {new Date(ticket.createdAt).toLocaleString()}
                </Card.Text>
                <Card.Text className="text-muted small">
                    Assigned to: {ticket.assigneeId || 'Unassigned'}
                    {ticket.slaDeadline && <> · Due {new Date(ticket.slaDeadline).toLocaleString()}</>}
                </Card.Text>
                {canManage && (
                    <div className="d-flex justify-content-end gap-2">
                        <Button variant="outline-primary" size="sm" onClick={() => onEdit(ticket)}>
                            Update
                        </Button>
                        <Button variant="outline-dark" size="sm" disabled={isClosed} onClick={() => onClose(ticket)}>
                            Close Ticket
                        </Button>
                    </div>
                )}
            </Card.Body>
        </Card>
    );
}
