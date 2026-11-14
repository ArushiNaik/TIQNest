import TicketCard from './TicketCard';
import type { Ticket, User } from '../types/models';
import './TicketList.css';

interface TicketListProps {
    tickets: Ticket[];
    users: User[];
    canManage: boolean;
    onEdit: (ticket: Ticket) => void;
    onClose: (ticket: Ticket) => void;
}

export default function TicketList({ tickets, users, canManage, onEdit, onClose }: TicketListProps) {
    if (tickets.length === 0) {
        return <p className="text-center text-muted mt-4">No tickets yet. Report a problem to get started.</p>;
    }

    return (
        <div className="ticket-list">
            {tickets.map((ticket) => {
                const reporter = users.find((user) => user.userId === ticket.reporterId);
                return (
                    <div className="ticket-item" key={ticket.ticketId}>
                        <div className="reporter-info">
                            Reported by {reporter ? reporter.name : `user ${ticket.reporterId}`}
                        </div>
                        <TicketCard ticket={ticket} canManage={canManage} onEdit={onEdit} onClose={onClose} />
                    </div>
                );
            })}
        </div>
    );
}
