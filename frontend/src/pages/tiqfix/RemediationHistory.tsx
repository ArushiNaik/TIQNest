import { useCallback, useEffect, useState } from 'react';
import { Alert, Badge, Card, ListGroup, Spinner } from 'react-bootstrap';
import { getAttempts } from '../../services/remediation-service';
import type { RemediationAttempt } from '../../types/remediation';
import { formatDate } from '../TicketCard';
import { OUTCOME } from './tiqfix-ui';

interface RemediationHistoryProps {
    ticketId?: number;
    /** Change this value to reload the list. */
    refreshKey: number;
}

export default function RemediationHistory({ ticketId, refreshKey }: RemediationHistoryProps) {
    const [attempts, setAttempts] = useState<RemediationAttempt[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            setAttempts(await getAttempts(ticketId));
            setError(null);
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    }, [ticketId]);

    useEffect(() => {
        load();
    }, [load, refreshKey]);

    return (
        <Card className="shadow-sm tiqfix-history">
            <Card.Header className="bg-white d-flex justify-content-between align-items-center">
                <h2 className="h6 mb-0">🕘 {ticketId ? `Repair history for ticket #${ticketId}` : 'Your recent repairs'}</h2>
                {loading && <Spinner size="sm" animation="border" role="status"><span className="visually-hidden">Loading history</span></Spinner>}
            </Card.Header>
            {error && <Alert variant="danger" className="m-3 mb-0 py-2">{error}</Alert>}
            {!loading && !error && attempts.length === 0 && (
                <Card.Body className="text-center text-muted py-4">
                    <div className="fs-3" aria-hidden="true">🧰</div>
                    No TiqFix attempts yet. Your results will appear here.
                </Card.Body>
            )}
            {attempts.length > 0 && (
                <ListGroup variant="flush">
                    {attempts.map((a) => {
                        const o = OUTCOME[a.outcome];
                        return (
                            <ListGroup.Item key={a.id} className="d-flex justify-content-between align-items-start gap-2">
                                <div>
                                    <div className="fw-semibold">{a.remedyTitle}</div>
                                    <div className="small text-muted">
                                        {formatDate(a.createdAt)}
                                        {a.ticketId != null && !ticketId && <> · Ticket #{a.ticketId}</>}
                                    </div>
                                    {a.feedback && <div className="small fst-italic mt-1">“{a.feedback}”</div>}
                                </div>
                                <Badge bg={o.variant} className="text-nowrap">{o.icon} {o.label}</Badge>
                            </ListGroup.Item>
                        );
                    })}
                </ListGroup>
            )}
            <Card.Footer className="bg-white small text-muted">Outcomes are reported by users, not verified automatically.</Card.Footer>
        </Card>
    );
}
