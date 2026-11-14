import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { Button, Form } from 'react-bootstrap';
import type { User, UserRole } from '../types/models';

interface UserFormProps {
    user?: User | null;   // set when editing an existing user
    onSave: (data: User, userId: string | null) => void;
    onCancel: () => void;
}

const emptyForm: User = {
    userId: '',
    name: '',
    email: '',
    role: 'EMPLOYEE',
    password: '',
};

export default function UserForm({ user, onSave, onCancel }: UserFormProps) {
    const [formData, setFormData] = useState<User>(emptyForm);
    const isEditing = !!user;

    useEffect(() => {
        if (user) {
            setFormData({
                userId: user.userId || '',
                name: user.name || '',
                email: user.email || '',
                role: user.role || 'EMPLOYEE',
                password: '', // never prefill; leave blank to keep the current password
            });
        }
    }, [user]);

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        const userData: User = { ...formData, role: formData.role as UserRole };
        if (isEditing && !userData.password) {
            delete userData.password;
        }
        onSave(userData, user ? user.userId : null);
    };

    return (
        <div className="form-container">
            <h2>{isEditing ? 'Edit User' : 'Add User'}</h2>
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
                    <Form.Label>{isEditing ? 'New password (leave blank to keep current)' : 'Password'}</Form.Label>
                    <Form.Control
                        type="password"
                        name="password"
                        value={formData.password}
                        onChange={handleChange}
                        required={!isEditing}
                    />
                </Form.Group>

                <div className="d-flex gap-2">
                    <Button variant="primary" type="submit">
                        {isEditing ? 'Save Changes' : 'Add User'}
                    </Button>
                    <Button variant="outline-secondary" type="button" onClick={onCancel}>
                        Cancel
                    </Button>
                </div>
            </Form>
        </div>
    );
}
