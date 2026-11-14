import { Badge, Table } from 'react-bootstrap';
import type { User } from '../types/models';

export default function UserList({ users }: { users: User[] }) {
    return (
        <Table striped bordered hover className="bg-white">
            <thead>
                <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>User ID</th>
                </tr>
            </thead>
            <tbody>
                {users.map((user) => (
                    <tr key={user.userId}>
                        <td>{user.name}</td>
                        <td>{user.email}</td>
                        <td>
                            <Badge bg={user.role === 'ADMIN' ? 'primary' : 'secondary'}>
                                {user.role === 'ADMIN' ? 'Admin' : 'Employee'}
                            </Badge>
                        </td>
                        <td className="text-muted small">{user.userId}</td>
                    </tr>
                ))}
            </tbody>
        </Table>
    );
}
