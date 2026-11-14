import type { AuditAction, AuditEvent } from '../../types/models';
import './ops.css';

const icon: Record<AuditAction, string> = {
    CREATED: '📝',
    STATUS_CHANGED: '🔄',
    PRIORITY_CHANGED: '⚡',
    ASSIGNED: '👤',
    LINKED_TO_INCIDENT: '📡',
};

export default function TicketTimeline({ events }: { events: AuditEvent[] }) {
    if (events.length === 0) {
        return <p className="text-muted small mb-0">No history yet.</p>;
    }

    return (
        <ul className="timeline">
            {events.map((event) => (
                <li key={event.id} className="timeline-item">
                    <span className="timeline-icon" aria-hidden="true">{icon[event.action] ?? '•'}</span>
                    <div>
                        <div>{event.message}</div>
                        <small className="text-muted">
                            {new Date(event.createdAt).toLocaleString()} · by {event.actor}
                        </small>
                    </div>
                </li>
            ))}
        </ul>
    );
}
