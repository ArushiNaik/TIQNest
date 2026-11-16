import { useEffect, useState } from 'react';
import { Alert, Button, Container } from 'react-bootstrap';
import { ApiRequestError, getData, postData } from '../../services/api-services';
import { getCurrentUser, isAdmin } from '../../services/auth';
import { API_URL_TICKETS, API_URL_USERS } from '../../services/config';
import { OPS_REFRESH_EVENT, notifyOpsChanged } from '../../services/ops-service';
import type { CreateTicketRequest, Ticket, User } from '../../types/models';
import TicketDetailsModal from '../dashboard/TicketDetailsModal';
import TicketForm from './TicketForm';
import TicketList from './TicketList';

type Mode = 'list' | 'create';

export default function TicketPage() {
    const [tickets, setTickets] = useState<Ticket[]>([]);
    const [users, setUsers] = useState<User[]>([]);
    const [mode, setMode] = useState<Mode>('list');
    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [saveError, setSaveError] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [openTicketId, setOpenTicketId] = useState<number | null>(null);

    const currentUser = getCurrentUser();
    const admin = isAdmin();

    const fetchTickets = async () => {
        try {
            const data = await getData<Ticket[]>(API_URL_TICKETS);
            const myEmail = currentUser?.email.toLowerCase();
            setTickets(admin ? data : data.filter((t) => t.requesterEmail.toLowerCase() === myEmail));
            setLoadError(null);
        } catch (err) {
            console.error('Error fetching tickets:', err);
            setLoadError((err as Error).message);
        } finally {
            setIsLoading(false);
        }
    };

    const fetchUsers = async () => {
        try {
            setUsers(await getData<User[]>(API_URL_USERS));
        } catch (err) {
            console.warn('Could not load users:', err);
        }
    };

    useEffect(() => {
        fetchTickets();
        if (admin) fetchUsers();
        const refresh = () => fetchTickets();
        window.addEventListener(OPS_REFRESH_EVENT, refresh);
        const timer = setInterval(refresh, 15000);
        return () => {
            window.removeEventListener(OPS_REFRESH_EVENT, refresh);
            clearInterval(timer);
        };
    }, []);

    const handleReportNew = () => {
        setSaveError(null);
        setFieldErrors({});
        setMode('create');
    };

    const handleSaveTicket = async (data: CreateTicketRequest) => {
        try {
            await postData<Ticket>(API_URL_TICKETS, data);
            await fetchTickets();
            notifyOpsChanged();
            setMode('list');
        } catch (err) {
            console.error('Error saving ticket:', err);
            setSaveError((err as Error).message);
            setFieldErrors(err instanceof ApiRequestError ? err.fieldErrors ?? {} : {});
        }
    };

    if (isLoading) return <p className="center">Loading tickets...</p>;

    return (
        <Container className="mt-4">
            {mode === 'list' && (
                <>
                    <div className="d-flex justify-content-between align-items-center">
                        <h2 className="mb-0">{admin ? 'All Tickets' : 'My Tickets'}</h2>
                        <Button variant="success" onClick={handleReportNew}>
                            Report a Problem
                        </Button>
                    </div>
                    {loadError && (
                        <Alert variant="danger" className="mt-3">
                            {loadError}{' '}
                            <Alert.Link onClick={fetchTickets}>Try again</Alert.Link>
                        </Alert>
                    )}
                    <TicketList tickets={tickets} users={users} onOpen={setOpenTicketId} />
                </>
            )}

            {mode === 'create' && currentUser && (
                <TicketForm
                    requesterEmail={currentUser.email}
                    error={saveError}
                    fieldErrors={fieldErrors}
                    onSave={handleSaveTicket}
                    onCancel={() => setMode('list')}
                />
            )}

            <TicketDetailsModal
                ticketId={openTicketId}
                onClose={() => setOpenTicketId(null)}
                onUpdated={() => fetchTickets()}
            />
        </Container>
    );
}
