import { useEffect, useState } from 'react';
import { Alert, Button, Container } from 'react-bootstrap';
import { getData, postData } from '../../services/api-services';
import { API_URL_USERS } from '../../services/config';
import type { User, UserRequest } from '../../types/models';
import UserForm from './UserForm';
import UserList from './UserList';

type Mode = 'list' | 'create';

export default function UserPage() {
    const [users, setUsers] = useState<User[]>([]);
    const [mode, setMode] = useState<Mode>('list');
    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [saveError, setSaveError] = useState<string | null>(null);

    const fetchUsers = async () => {
        try {
            setUsers(await getData<User[]>(API_URL_USERS));
            setLoadError(null);
        } catch (err) {
            console.error('Error fetching users:', err);
            setLoadError((err as Error).message);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const handleAddNew = () => {
        setSaveError(null);
        setMode('create');
    };

    const handleSaveUser = async (data: UserRequest) => {
        try {
            await postData<User>(API_URL_USERS, data);
            await fetchUsers();
            setMode('list');
        } catch (err) {
            console.error('Error saving user:', err);
            setSaveError((err as Error).message);
        }
    };

    if (isLoading) return <p className="center">Loading users...</p>;

    return (
        <Container className="mt-4">
            {mode === 'list' && (
                <>
                    <div className="d-flex justify-content-between align-items-center mb-3">
                        <h2 className="mb-0">Users</h2>
                        <Button variant="success" onClick={handleAddNew}>Add User</Button>
                    </div>
                    {loadError && (
                        <Alert variant="danger">
                            {loadError}{' '}
                            <Alert.Link onClick={fetchUsers}>Try again</Alert.Link>
                        </Alert>
                    )}
                    <UserList users={users} />
                </>
            )}

            {mode === 'create' && (
                <UserForm error={saveError} onSave={handleSaveUser} onCancel={() => setMode('list')} />
            )}
        </Container>
    );
}
