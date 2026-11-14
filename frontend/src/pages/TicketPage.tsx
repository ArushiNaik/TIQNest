import { useEffect, useState } from 'react';
import { Button, Container } from 'react-bootstrap';
import { getData, patchData, postData } from '../services/api-services';
import { getCurrentUser } from '../services/auth';
import { API_URL_TICKETS, API_URL_USERS } from '../services/config';
import type { Ticket, User } from '../types/models';
import TicketForm, { type TicketFormData } from './TicketForm';
import TicketList from './TicketList';

type Mode = 'list' | 'create' | 'edit';

export default function TicketPage() {
    const [tickets, setTickets] = useState<Ticket[]>([]);
    const [users, setUsers] = useState<User[]>([]);
    const [mode, setMode] = useState<Mode>('list');
    const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const currentUser = getCurrentUser();
    const isAdmin = currentUser?.role === 'ADMIN';

    const fetchTickets = async () => {
        try {
            const data = await getData<Ticket[]>(API_URL_TICKETS);
            // Employees only see the tickets they reported; admins see everything.
            const visible = isAdmin ? data : data.filter((t) => t.reporterId === currentUser?.userId);
            const newestFirst = [...visible].sort(
                (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
            );
            setTickets(newestFirst);
        } catch (err) {
            console.error('Error fetching tickets:', err);
            setError((err as Error).message);
        } finally {
            setIsLoading(false);
        }
    };

    const fetchUsers = async () => {
        try {
            setUsers(await getData<User[]>(API_URL_USERS));
        } catch (err) {
            // Not fatal: reporter names fall back to their IDs.
            console.warn('Could not load users:', err);
        }
    };

    useEffect(() => {
        fetchTickets();
        fetchUsers();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleReportNew = () => {
        setSelectedTicket(null);
        setMode('create');
    };

    const handleUpdateTicket = (ticket: Ticket) => {
        setSelectedTicket(ticket);
        setMode('edit');
    };

    const handleCloseTicket = async (ticket: Ticket) => {
        try {
            await patchData(`${API_URL_TICKETS}/${ticket.ticketId}`, { status: 'CLOSED' });
            await fetchTickets();
        } catch (err) {
            console.error('Error closing ticket:', err);
            alert('Could not close the ticket: ' + (err as Error).message);
        }
    };

    const handleSaveTicket = async (data: TicketFormData, ticketId: string | null) => {
        try {
            if (ticketId) {
                await patchData(`${API_URL_TICKETS}/${ticketId}`, {
                    status: data.status,
                    priority: data.priority,
                    assigneeId: data.assigneeId || null,
                });
            } else {
                await postData(API_URL_TICKETS, {
                    title: data.title,
                    description: data.description,
                    location: data.location,
                    priority: data.priority,
                    reporterId: currentUser?.userId,
                });
            }
            await fetchTickets();
            setMode('list');
        } catch (err) {
            console.error('Error saving ticket:', err);
            alert((err as Error).message || 'There was an error saving the ticket.');
        }
    };

    if (error) return <p className="center text-danger">Error: {error}</p>;
    if (isLoading) return <p className="center">Loading tickets...</p>;

    const admins = users.filter((u) => u.role === 'ADMIN');

    return (
        <Container className="mt-4">
            {mode === 'list' && (
                <>
                    <div className="d-flex justify-content-between align-items-center">
                        <h2 className="mb-0">{isAdmin ? 'All Tickets' : 'My Tickets'}</h2>
                        <Button variant="success" onClick={handleReportNew}>
                            Report a Problem
                        </Button>
                    </div>
                    <TicketList
                        tickets={tickets}
                        users={users}
                        canManage={isAdmin}
                        onEdit={handleUpdateTicket}
                        onClose={handleCloseTicket}
                    />
                </>
            )}

            {mode === 'create' && (
                <TicketForm admins={admins} onSave={handleSaveTicket} onCancel={() => setMode('list')} />
            )}

            {mode === 'edit' && selectedTicket && (
                <TicketForm
                    ticket={selectedTicket}
                    admins={admins}
                    onSave={handleSaveTicket}
                    onCancel={() => setMode('list')}
                />
            )}
        </Container>
    );
}
