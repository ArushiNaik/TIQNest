import { useState, type ChangeEvent, type FormEvent } from 'react';
import { Alert, Button, Form } from 'react-bootstrap';
import type { CreateTicketRequest, TicketCategory } from '../types/models';
import { formatLabel } from './TicketCard';

const categories: TicketCategory[] = ['NETWORK', 'EMAIL', 'VPN', 'PRINTER', 'SOFTWARE', 'HARDWARE', 'ACCOUNT', 'OTHER'];

interface TicketFormProps {
    requesterEmail: string;
    error: string | null;
    fieldErrors: Record<string, string>;
    onSave: (data: CreateTicketRequest) => void;
    onCancel: () => void;
}

export default function TicketForm({ requesterEmail, error, fieldErrors, onSave, onCancel }: TicketFormProps) {
    const [formData, setFormData] = useState<CreateTicketRequest>({
        title: '',
        description: '',
        category: 'NETWORK',
        affectedSystem: '',
        requesterEmail,
    });

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        onSave(formData);
    };

    return (
        <div className="form-container">
            <h2>Report a Problem</h2>
            {error && <Alert variant="danger">{error}</Alert>}
            <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3">
                    <Form.Label>What is the problem?</Form.Label>
                    <Form.Control
                        name="title"
                        value={formData.title}
                        onChange={handleChange}
                        placeholder="e.g. Wi-Fi keeps disconnecting"
                        maxLength={200}
                        isInvalid={!!fieldErrors.title}
                        required
                    />
                    <Form.Control.Feedback type="invalid">{fieldErrors.title}</Form.Control.Feedback>
                </Form.Group>
                <Form.Group className="mb-3">
                    <Form.Label>Category</Form.Label>
                    <Form.Select name="category" value={formData.category} onChange={handleChange}>
                        {categories.map((c) => (
                            <option key={c} value={c}>{formatLabel(c)}</option>
                        ))}
                    </Form.Select>
                </Form.Group>
                <Form.Group className="mb-3">
                    <Form.Label>Affected system</Form.Label>
                    <Form.Control
                        name="affectedSystem"
                        value={formData.affectedSystem}
                        onChange={handleChange}
                        placeholder="e.g. Office Wi-Fi, Outlook, Printer 2nd floor"
                        maxLength={100}
                        isInvalid={!!fieldErrors.affectedSystem}
                        required
                    />
                    <Form.Control.Feedback type="invalid">{fieldErrors.affectedSystem}</Form.Control.Feedback>
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
                        maxLength={2000}
                    />
                </Form.Group>
                <Form.Text className="d-block mb-3">Reported as {requesterEmail}</Form.Text>

                <div className="d-flex gap-2">
                    <Button variant="primary" type="submit">Submit Ticket</Button>
                    <Button variant="outline-secondary" type="button" onClick={onCancel}>Cancel</Button>
                </div>
            </Form>
        </div>
    );
}
