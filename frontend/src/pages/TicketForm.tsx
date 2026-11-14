import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { Button, Form } from 'react-bootstrap';
import type { Ticket, TicketPriority, TicketStatus, User } from '../types/models';
import { formatLabel } from './TicketCard';

const priorities: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const statuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

export interface TicketFormData {
    title: string;
    description: string;
    location: string;
    priority: TicketPriority;
    status: TicketStatus;
    assigneeId: string;
}

interface TicketFormProps {
    ticket?: Ticket | null;        // set when updating an existing ticket
    admins: User[];                // people who can be assigned
    onSave: (data: TicketFormData, ticketId: string | null) => void;
    onCancel: () => void;
}

const emptyForm: TicketFormData = {
    title: '',
    description: '',
    location: '',
    priority: 'MEDIUM',
    status: 'OPEN',
    assigneeId: '',
};

export default function TicketForm({ ticket, admins, onSave, onCancel }: TicketFormProps) {
    const [formData, setFormData] = useState<TicketFormData>(emptyForm);
    const isUpdating = !!ticket;

    useEffect(() => {
        if (ticket) {
            setFormData({
                title: ticket.title || '',
                description: ticket.description || '',
                location: ticket.location || '',
                priority: ticket.priority || 'MEDIUM',
                status: ticket.status || 'OPEN',
                assigneeId: ticket.assigneeId || '',
            });
        }
    }, [ticket]);

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        onSave(formData, ticket ? ticket.ticketId : null);
    };

    return (
        <div className="form-container">
            <h2>{isUpdating ? `Update Ticket #${ticket!.ticketId}` : 'Report a Problem'}</h2>
            <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3">
                    <Form.Label>What is the problem?</Form.Label>
                    <Form.Control
                        name="title"
                        value={formData.title}
                        onChange={handleChange}
                        placeholder="e.g. Wi-Fi not working"
                        readOnly={isUpdating}
                        required
                    />
                </Form.Group>
                <Form.Group className="mb-3">
                    <Form.Label>Details</Form.Label>
                    <Form.Control
                        as="textarea"
                        rows={3}
                        name="description"
                        value={formData.description}
                        onChange={handleChange}
                        placeholder="What happened, and when did it start?"
                        readOnly={isUpdating}
                    />
                </Form.Group>
                <Form.Group className="mb-3">
                    <Form.Label>Location</Form.Label>
                    <Form.Control
                        name="location"
                        value={formData.location}
                        onChange={handleChange}
                        placeholder="e.g. 2nd floor, meeting room B"
                        readOnly={isUpdating}
                    />
                </Form.Group>
                <Form.Group className="mb-3">
                    <Form.Label>Priority</Form.Label>
                    <Form.Select name="priority" value={formData.priority} onChange={handleChange}>
                        {priorities.map((p) => (
                            <option key={p} value={p}>{formatLabel(p)}</option>
                        ))}
                    </Form.Select>
                </Form.Group>

                {isUpdating && (
                    <>
                        <Form.Group className="mb-3">
                            <Form.Label>Status</Form.Label>
                            <Form.Select name="status" value={formData.status} onChange={handleChange}>
                                {statuses.map((s) => (
                                    <option key={s} value={s}>{formatLabel(s)}</option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Assignee</Form.Label>
                            <Form.Select name="assigneeId" value={formData.assigneeId} onChange={handleChange}>
                                <option value="">Unassigned</option>
                                {admins.map((admin) => (
                                    <option key={admin.userId} value={admin.userId}>{admin.name}</option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    </>
                )}

                <div className="d-flex gap-2">
                    <Button variant="primary" type="submit">
                        {isUpdating ? 'Save Changes' : 'Submit Ticket'}
                    </Button>
                    <Button variant="outline-secondary" type="button" onClick={onCancel}>
                        Cancel
                    </Button>
                </div>
            </Form>
        </div>
    );
}
