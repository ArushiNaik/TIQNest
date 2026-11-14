import { useEffect, useState } from 'react';
import { Button, Container } from 'react-bootstrap';
import { deleteData, getData, postData, putData } from '../services/api-services';
import { API_URL_USERS } from '../services/config';
import type { User } from '../types/models';
import UserForm from './UserForm';
import UserList from './UserList';

type Mode = 'list' | 'create' | 'edit';

export default function UserPage() {
    const [users, setUsers] = useState<User[]>([]);
    const [mode, setMode] = useState<Mode>('list');
    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchUsers = async () => {
        try {
            setUsers(await getData<User[]>(API_URL_USERS));
        } catch (err) {
            console.error('Error fetching users:', err);
            setError((err as Error).message);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const handleDelete = async (userId: string) => {
        if (!window.confirm('Delete this user?')) return;
        try {
            await deleteData(`${API_URL_USERS}/${userId}`);
            await fetchUsers();
            setMode('list');
        } catch (err) {
            console.error('Error deleting user:', err);
            alert('Could not delete the user: ' + (err as Error).message);
        }
    };

    const handleEditUser = (user: User) => {
        setSelectedUser(user);
        setMode('edit');
    };

    const handleAddNew = () => {
        setSelectedUser(null);
        setMode('create');
    };

    const handleSaveUser = async (data: User, userId: string | null) => {
        try {
            if (userId) {
                await putData(`${API_URL_USERS}/${userId}`, data);
            } else {
                await postData(API_URL_USERS, data);
            }
            await fetchUsers();
            setMode('list');
        } catch (err) {
            console.error('Error saving user:', err);
            alert('There was an error saving the user. Please try again later.');
        }
    };

    if (error) return <p className="center text-danger">Error: {error}</p>;
    if (isLoading) return <p className="center">Loading users...</p>;

    return (
        <Container className="mt-4">
            {mode === 'list' && (
                <>
                    <div className="d-flex justify-content-between align-items-center mb-3">
                        <h2 className="mb-0">Users</h2>
                        <Button variant="success" onClick={handleAddNew}>Add User</Button>
                    </div>
                    <UserList users={users} onEdit={handleEditUser} onDelete={handleDelete} />
                </>
            )}

            {mode === 'create' && <UserForm onSave={handleSaveUser} onCancel={() => setMode('list')} />}

            {mode === 'edit' && selectedUser && (
                <UserForm user={selectedUser} onSave={handleSaveUser} onCancel={() => setMode('list')} />
            )}
        </Container>
    );
}
