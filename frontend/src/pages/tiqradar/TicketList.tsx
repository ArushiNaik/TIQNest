import TicketCard from './TicketCard';
import type { Ticket, User } from '../../types/models';
import './TicketList.css';

interface TicketListProps {
    tickets: Ticket[];
    users: User[];
    onOpen?: (id: number) => void;
}

export default function TicketList({ tickets, users, onOpen }: TicketListProps) {
    if (tickets.length === 0) {
        return <p className="text-center text-muted mt-4">No tickets yet. Report a problem to get started.</p>;
    }

    return (
        <div className="ticket-list">
            {tickets.map((ticket) => {
                const requester = users.find(
                    (user) => user.email.toLowerCase() === ticket.requesterEmail.toLowerCase()
                );
                return (
                    <div className="ticket-item" key={ticket.id}>
                        <div className="reporter-info">
                            Reported by {requester ? requester.name : ticket.requesterEmail}
                        </div>
                        <TicketCard ticket={ticket} onOpen={onOpen} />
                    </div>
                );
            })}
        </div>
    );
}
