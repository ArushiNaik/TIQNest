import { useState, type ChangeEvent, type FormEvent } from 'react';
import { Alert, Button, Form } from 'react-bootstrap';
import type { UserRequest } from '../../types/models';

interface UserFormProps {
    error: string | null;
    onSave: (data: UserRequest) => void;
    onCancel: () => void;
}

export default function UserForm({ error, onSave, onCancel }: UserFormProps) {
    const [formData, setFormData] = useState<UserRequest>({
        name: '',
        email: '',
        password: '',
        role: 'EMPLOYEE',
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
            <h2>Add User</h2>
            {error && <Alert variant="danger">{error}</Alert>}
            <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3">
                    <Form.Label>Full name</Form.Label>
                    <Form.Control name="name" value={formData.name} onChange={handleChange} required />
                </Form.Group>
                <Form.Group className="mb-3">
                    <Form.Label>Work email</Form.Label>
                    <Form.Control type="email" name="email" value={formData.email} onChange={handleChange} required />
                </Form.Group>
                <Form.Group className="mb-3">
                    <Form.Label>Role</Form.Label>
                    <Form.Select name="role" value={formData.role} onChange={handleChange}>
                        <option value="EMPLOYEE">Employee</option>
                        <option value="ADMIN">Admin</option>
                    </Form.Select>
                </Form.Group>
                <Form.Group className="mb-3">
                    <Form.Label>Password</Form.Label>
                    <Form.Control type="password" name="password" value={formData.password} onChange={handleChange} required />
                </Form.Group>

                <div className="d-flex gap-2">
                    <Button variant="primary" type="submit">Add User</Button>
                    <Button variant="outline-secondary" type="button" onClick={onCancel}>Cancel</Button>
                </div>
            </Form>
        </div>
    );
}
