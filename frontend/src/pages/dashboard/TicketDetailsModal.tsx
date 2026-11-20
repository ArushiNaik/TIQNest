import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Col, Form, Modal, Row, Spinner } from 'react-bootstrap';
import { getData } from '../../services/api-services';
import { getCurrentUser, isAdmin } from '../../services/auth';
import { API_URL_USERS } from '../../services/config';
import { getTicket, getTicketHistory, notifyOpsChanged, updateTicket } from '../../services/ops-service';
import type { AuditEvent, Ticket, TicketPriority, TicketStatus, UpdateTicketRequest, User } from '../../types/models';
import { formatDate, formatLabel } from '../tiqradar/TicketCard';
import SlaBadge from './SlaBadge';
import TicketTimeline from './TicketTimeline';

const editableStatuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
const priorities: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

interface TicketDetailsModalProps {
    ticketId: number | null;
    onClose: () => void;
    onUpdated?: (ticket: Ticket) => void;
}

export default function TicketDetailsModal({ ticketId, onClose, onUpdated }: TicketDetailsModalProps) {
    const admin = isAdmin();
    const [ticket, setTicket] = useState<Ticket | null>(null);
    const [history, setHistory] = useState<AuditEvent[]>([]);
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);
    const [saved, setSaved] = useState(false);

    const [status, setStatus] = useState<TicketStatus>('OPEN');
    const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
    const [assignee, setAssignee] = useState('');

    const fillForm = (t: Ticket) => {
        setTicket(t);
        setStatus(t.status);
        setPriority(t.priority);
        setAssignee(t.assignee ?? '');
    };

    useEffect(() => {
        if (ticketId == null) return;
        let cancelled = false;
        setLoading(true);
        setError(null);
        setSaved(false);
        Promise.all([getTicket(ticketId), getTicketHistory(ticketId)])
            .then(([t, h]) => {
                if (cancelled) return;
                fillForm(t);
                setHistory(h);
            })
            .catch((err) => !cancelled && setError((err as Error).message))
            .finally(() => !cancelled && setLoading(false));
        if (admin) {
            getData<User[]>(API_URL_USERS)
                .then((u) => !cancelled && setUsers(u))
                .catch(() => undefined);
        }
        return () => {
            cancelled = true;
        };
    }, [ticketId, admin]);

    const changes = (): UpdateTicketRequest => {
        if (!ticket) return {};
        const result: UpdateTicketRequest = {};
        if (status !== ticket.status) result.status = status;
        if (priority !== ticket.priority) result.priority = priority;
        if (assignee.trim() !== (ticket.assignee ?? '')) result.assignee = assignee.trim();
        return result;
    };

    const hasChanges = Object.keys(changes()).length > 0;

    const handleSave = async () => {
        if (!ticket || !hasChanges) return;
        setSaving(true);
        setError(null);
        setSaved(false);
        try {
            const updated = await updateTicket(ticket.id, { ...changes(), actor: getCurrentUser()?.email });
            fillForm(updated);
            setHistory(await getTicketHistory(ticket.id));
            setSaved(true);
            onUpdated?.(updated);
            notifyOpsChanged();
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setSaving(false);
        }
    };

    const statusOptions = ticket && !editableStatuses.includes(ticket.status)
        ? [ticket.status, ...editableStatuses]
        : editableStatuses;

    return (
        <Modal show={ticketId != null} onHide={onClose} size="lg" centered>
            <Modal.Header closeButton>
                <Modal.Title>{ticket ? `#${ticket.id} · ${ticket.title}` : 'Ticket'}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                {loading && (
                    <div className="text-center py-4">
                        <Spinner animation="border" size="sm" /> Loading...
                    </div>
                )}
                {error && <Alert variant="danger">{error}</Alert>}
                {saved && <Alert variant="success">Ticket updated.</Alert>}

                {ticket && !loading && (
                    <Row className="g-4">
                        <Col md={6}>
                            <div className="d-flex flex-wrap gap-1 mb-2">
                                <Badge bg="secondary">{formatLabel(ticket.category)}</Badge>
                                <Badge bg="info" text="dark">{formatLabel(ticket.status)}</Badge>
                                <Badge bg="dark">{formatLabel(ticket.priority)}</Badge>
                                <SlaBadge ticket={ticket} />
                            </div>
                            {ticket.description && <p>{ticket.description}</p>}
                            <dl className="small mb-3">
                                <dt>Affected system</dt>
                                <dd>{ticket.affectedSystem}</dd>
                                <dt>Reported by</dt>
                                <dd>{ticket.requesterEmail} · {formatDate(ticket.createdAt)}</dd>
                                <dt>SLA deadline</dt>
                                <dd>{formatDate(ticket.slaDeadline)}</dd>
                                <dt>Assignee</dt>
                                <dd>{ticket.assignee ?? 'Unassigned'}</dd>
                                {ticket.incidentId != null && (
                                    <>
                                        <dt>Incident</dt>
                                        <dd>Part of incident #{ticket.incidentId}</dd>
                                    </>
                                )}
                            </dl>

                            {admin && (
                                <>
                                    <h6>Update ticket</h6>
                                    <Form.Group className="mb-2">
                                        <Form.Label className="small mb-1">Status</Form.Label>
                                        <Form.Select
                                            size="sm"
                                            value={status}
                                            onChange={(e) => setStatus(e.target.value as TicketStatus)}
                                        >
                                            {statusOptions.map((s) => (
                                                <option key={s} value={s} disabled={!editableStatuses.includes(s)}>
                                                    {formatLabel(s)}
                                                </option>
                                            ))}
                                        </Form.Select>
                                    </Form.Group>
                                    <Form.Group className="mb-2">
                                        <Form.Label className="small mb-1">Priority (resets the SLA deadline)</Form.Label>
                                        <Form.Select
                                            size="sm"
                                            value={priority}
                                            onChange={(e) => setPriority(e.target.value as TicketPriority)}
                                        >
                                            {priorities.map((p) => (
                                                <option key={p} value={p}>{formatLabel(p)}</option>
                                            ))}
                                        </Form.Select>
                                    </Form.Group>
                                    <Form.Group className="mb-3">
                                        <Form.Label className="small mb-1">Assignee</Form.Label>
                                        <Form.Control
                                            size="sm"
                                            list="assignee-options"
                                            value={assignee}
                                            onChange={(e) => setAssignee(e.target.value)}
                                            placeholder="Name or email, empty = unassigned"
                                            maxLength={100}
                                        />
                                        <datalist id="assignee-options">
                                            {users.map((u) => (
                                                <option key={u.userId} value={u.email}>{u.name}</option>
                                            ))}
                                        </datalist>
                                    </Form.Group>
                                    <Button size="sm" onClick={handleSave} disabled={!hasChanges || saving}>
                                        {saving ? 'Saving...' : 'Save changes'}
                                    </Button>
                                </>
                            )}
                        </Col>
                        <Col md={6}>
                            <h6>History</h6>
                            <TicketTimeline events={history} />
                        </Col>
                    </Row>
                )}
            </Modal.Body>
        </Modal>
    );
}
