import { useCallback, useEffect, useState } from 'react';
import { Badge, Button, Card, ListGroup } from 'react-bootstrap';
import { closeAdminReview, getAdminReviews } from '../../services/triage-service';
import type { AdminReview } from '../../types/triage';

/** Admin dashboard card: out-of-scope submissions employees explicitly asked a human to review. */
export default function AdminReviewPanel() {
    const [reviews, setReviews] = useState<AdminReview[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [busy, setBusy] = useState<number | null>(null);

    const load = useCallback(async () => {
        try {
            setReviews(await getAdminReviews());
            setError(null);
        } catch (err) {
            setError((err as Error).message);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    const close = async (id: number) => {
        setBusy(id);
        try {
            await closeAdminReview(id);
            await load();
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setBusy(null);
        }
    };

    const open = reviews.filter((r) => r.status === 'OPEN');

    return (
        <Card className="shadow-sm mb-4">
            <Card.Header className="d-flex justify-content-between align-items-center">
                <span className="fw-semibold">🙋 Review requests <Badge bg="secondary">{open.length}</Badge></span>
                <Button size="sm" variant="outline-light" onClick={load}>Refresh</Button>
            </Card.Header>
            {error && <div className="small text-warning px-3 pt-2">{error}</div>}
            {open.length === 0 ? (
                <Card.Body className="small text-muted">No open review requests. These are submissions TiqVision judged out of IT scope.</Card.Body>
            ) : (
                <ListGroup variant="flush">
                    {open.map((r) => (
                        <ListGroup.Item key={r.id} className="d-flex justify-content-between align-items-start gap-2">
                            <div>
                                <div className="small text-muted">{r.requesterEmail} · {new Date(r.createdAt).toLocaleString()}</div>
                                <div>“{r.submittedText}”</div>
                            </div>
                            <Button size="sm" variant="outline-success" disabled={busy === r.id} onClick={() => close(r.id)}>
                                Mark reviewed
                            </Button>
                        </ListGroup.Item>
                    ))}
                </ListGroup>
            )}
        </Card>
    );
}
