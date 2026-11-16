import { useEffect, useState, type FormEvent } from 'react';
import { Alert, Button, Form, Modal, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { getCurrentUser } from '../../services/auth';
import { createEscalationTicket } from '../../services/remediation-service';
import type { Ticket, TicketCategory } from '../../types/models';
import type { IssueType, Remedy, TiqFixTicket } from '../../types/remediation';
import { formatLabel } from '../tiqradar/TicketCard';
import { issueOption } from './tiqfix-ui';

const CATEGORIES: TicketCategory[] = ['NETWORK', 'EMAIL', 'VPN', 'PRINTER', 'SOFTWARE', 'HARDWARE', 'ACCOUNT', 'OTHER'];

interface EscalationModalProps {
    show: boolean;
    onHide: () => void;
    ticket: TiqFixTicket | null;
    issueType: IssueType;
    description: string;
    remedy: Remedy | null;
}

export default function EscalationModal({ show, onHide, ticket, issueType, description, remedy }: EscalationModalProps) {
    const user = getCurrentUser();
    const navigate = useNavigate();
    const goToTickets = () => {
        onHide();
        navigate('/tickets');
    };
    const option = issueOption(issueType);
    const [title, setTitle] = useState('');
    const [category, setCategory] = useState<TicketCategory>('OTHER');
    const [system, setSystem] = useState('');
    const [details, setDetails] = useState('');
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [created, setCreated] = useState<Ticket | null>(null);

    useEffect(() => {
        if (!show) return;
        const summary = description.trim();
        setTitle(summary ? summary.slice(0, 120) : `${option.label.replace(/s$/, '')} needs IT help`);
        setCategory(option.category);
        setSystem(option.system);
        setDetails(
            [summary, remedy && remedy.actionType !== 'ESCALATION' ? `TiqFix tried: "${remedy.title}" – not resolved.` : 'TiqFix: no approved self-service fix available.']
                .filter(Boolean)
                .join('\n\n'),
        );
        setError(null);
        setFieldErrors({});
        setCreated(null);
    }, [show]);

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        if (!user) return;
        setSaving(true);
        setError(null);
        setFieldErrors({});
        try {
            setCreated(await createEscalationTicket({
                title: title.trim(),
                description: details.trim(),
                category,
                affectedSystem: system.trim(),
                requesterEmail: user.email,
            }));
        } catch (err) {
            setError((err as Error).message);
            setFieldErrors((err as { fieldErrors?: Record<string, string> }).fieldErrors ?? {});
        } finally {
            setSaving(false);
        }
    };

    return (
        <Modal show={show} onHide={onHide} centered>
            <Modal.Header closeButton>
                <Modal.Title className="h5">🎧 Escalate to IT</Modal.Title>
            </Modal.Header>

            {ticket ? (
                <>
                    <Modal.Body>
                        <p>
                            Your ticket <strong>#{ticket.id} – {ticket.title}</strong> is already in the IT queue.
                        </p>
                        <p className="mb-0 text-muted">
                            Any TiqFix result you report is saved to this ticket’s repair history, so the technician can see what you already tried.
                        </p>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="outline-secondary" onClick={onHide}>Close</Button>
                        <Button variant="primary" onClick={goToTickets}>View my tickets</Button>
                    </Modal.Footer>
                </>
            ) : created ? (
                <>
                    <Modal.Body>
                        <Alert variant="success" className="mb-0" role="status">
                            <strong>Ticket #{created.id} created.</strong> A technician will follow up with you.
                        </Alert>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="outline-secondary" onClick={onHide}>Close</Button>
                        <Button variant="primary" onClick={goToTickets}>View my tickets</Button>
                    </Modal.Footer>
                </>
            ) : (
                <Form onSubmit={handleSubmit} noValidate>
                    <Modal.Body>
                        <p className="text-muted small">This creates a normal help desk ticket for the IT team.</p>
                        {error && <Alert variant="danger" className="py-2">{error}</Alert>}
                        <Form.Group className="mb-3" controlId="esc-title">
                            <Form.Label>Title</Form.Label>
                            <Form.Control value={title} maxLength={200} required isInvalid={!!fieldErrors.title}
                                onChange={(e) => setTitle(e.target.value)} />
                            <Form.Control.Feedback type="invalid">{fieldErrors.title}</Form.Control.Feedback>
                        </Form.Group>
                        <div className="row g-2">
                            <Form.Group className="mb-3 col-sm-6" controlId="esc-category">
                                <Form.Label>Category</Form.Label>
                                <Form.Select value={category} onChange={(e) => setCategory(e.target.value as TicketCategory)}>
                                    {CATEGORIES.map((c) => <option key={c} value={c}>{formatLabel(c)}</option>)}
                                </Form.Select>
                            </Form.Group>
                            <Form.Group className="mb-3 col-sm-6" controlId="esc-system">
                                <Form.Label>Affected system</Form.Label>
                                <Form.Control value={system} maxLength={100} required placeholder="e.g. Outlook"
                                    isInvalid={!!fieldErrors.affectedSystem} onChange={(e) => setSystem(e.target.value)} />
                                <Form.Control.Feedback type="invalid">{fieldErrors.affectedSystem}</Form.Control.Feedback>
                            </Form.Group>
                        </div>
                        <Form.Group controlId="esc-details">
                            <Form.Label>Details</Form.Label>
                            <Form.Control as="textarea" rows={4} maxLength={2000} value={details}
                                onChange={(e) => setDetails(e.target.value)} />
                        </Form.Group>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="outline-secondary" onClick={onHide} disabled={saving}>Cancel</Button>
                        <Button type="submit" variant="danger" disabled={saving || !title.trim() || !system.trim()}>
                            {saving && <Spinner size="sm" animation="border" className="me-2" />}Send to IT
                        </Button>
                    </Modal.Footer>
                </Form>
            )}
        </Modal>
    );
}
