import { Badge, Button, Table } from 'react-bootstrap';
import type { User } from '../types/models';

interface UserListProps {
    users: User[];
    onEdit: (user: User) => void;
    onDelete: (userId: string) => void;
}

export default function UserList({ users, onEdit, onDelete }: UserListProps) {
    return (
        <Table striped bordered hover className="bg-white">
            <thead>
                <tr>
                    <th>User ID</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                {users.map((user) => (
                    <tr key={user.userId}>
                        <td>{user.userId}</td>
                        <td>{user.name}</td>
                        <td>{user.email}</td>
                        <td>
                            <Badge bg={user.role === 'ADMIN' ? 'primary' : 'secondary'}>
                                {user.role === 'ADMIN' ? 'Admin' : 'Employee'}
                            </Badge>
                        </td>
                        <td className="d-flex gap-2">
                            <Button variant="outline-primary" size="sm" onClick={() => onEdit(user)}>Edit</Button>
                            <Button variant="outline-danger" size="sm" onClick={() => onDelete(user.userId)}>Delete</Button>
                        </td>
                    </tr>
                ))}
            </tbody>
        </Table>
    );
}
