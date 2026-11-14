import { useEffect, useState } from 'react';
import { Alert, Button, Card, Form, Spinner } from 'react-bootstrap';
import { recordAttempt } from '../../services/remediation-service';
import type { RemediationAttempt, RemediationOutcome, Remedy } from '../../types/remediation';

interface RemediationFeedbackProps {
    remedy: Remedy;
    ticketId?: number;
    onRecorded: (attempt: RemediationAttempt) => void;
    onEscalate: () => void;
}

export default function RemediationFeedback({ remedy, ticketId, onRecorded, onEscalate }: RemediationFeedbackProps) {
    const [note, setNote] = useState('');
    const [submitting, setSubmitting] = useState<RemediationOutcome | null>(null);
    const [recorded, setRecorded] = useState<RemediationAttempt | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        setRecorded(null);
        setError(null);
        setNote('');
    }, [remedy.remedyId, ticketId]);

    const submit = async (outcome: RemediationOutcome) => {
        setSubmitting(outcome);
        setError(null);
        try {
            const attempt = await recordAttempt({
                remedyId: remedy.remedyId,
                ticketId,
                outcome,
                feedback: note.trim() || undefined,
            });
            setRecorded(attempt);
            onRecorded(attempt);
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setSubmitting(null);
        }
    };

    if (recorded) {
        if (recorded.outcome === 'SUCCESS') {
            return (
                <Alert variant="success" className="tiqfix-feedback-done shadow-sm" role="status">
                    <Alert.Heading className="h5">🎉 Resolution reported successfully.</Alert.Heading>
                    <p className="mb-1">Thanks! We’ve recorded that <strong>{remedy.title}</strong> solved your issue
                        {ticketId ? <> for ticket #{ticketId}</> : null}.</p>
                    <p className="small text-muted mb-0">This is your report – TiqFix doesn’t verify repairs automatically.</p>
                </Alert>
            );
        }
        return (
            <Alert variant={recorded.outcome === 'FAILED' ? 'warning' : 'secondary'} className="shadow-sm" role="status">
                <Alert.Heading className="h5">
                    {recorded.outcome === 'FAILED' ? 'Thanks for letting us know.' : 'Got it – we’ve noted that.'}
                </Alert.Heading>
                <p>
                    {recorded.outcome === 'FAILED'
                        ? 'This fix didn’t solve it, so let’s get a technician involved. Your TiqFix attempt is saved so IT knows what you already tried.'
                        : 'If the problem continues, a technician can help.'}
                </p>
                <Button variant="danger" onClick={onEscalate}>🎧 Escalate to IT</Button>
            </Alert>
        );
    }

    return (
        <Card className="tiqfix-feedback shadow-sm">
            <Card.Body className="p-4">
                <h3 className="h5 mb-1">Did this solve your issue?</h3>
                <p className="text-muted small">Let us know after you’ve tried the steps above. Your answer is saved to your history.</p>
                <Form.Group className="mb-3" controlId="tiqfix-feedback-note">
                    <Form.Label className="small">Add a note (optional)</Form.Label>
                    <Form.Control
                        as="textarea"
                        rows={2}
                        maxLength={1000}
                        value={note}
                        placeholder="e.g. Reconnected after signing in again"
                        onChange={(e) => setNote(e.target.value)}
                    />
                </Form.Group>
                {error && <Alert variant="danger" className="py-2">{error}</Alert>}
                <div className="d-flex flex-wrap gap-2 align-items-center">
                    <Button variant="success" size="lg" disabled={!!submitting} onClick={() => submit('SUCCESS')}>
                        {submitting === 'SUCCESS' ? <Spinner size="sm" animation="border" /> : '👍'} Yes, problem solved
                    </Button>
                    <Button variant="outline-danger" size="lg" disabled={!!submitting} onClick={() => submit('FAILED')}>
                        {submitting === 'FAILED' ? <Spinner size="sm" animation="border" /> : '👎'} No, still having issues
                    </Button>
                    <Button variant="link" className="text-muted" disabled={!!submitting} onClick={() => submit('UNKNOWN')}>
                        Not sure yet
                    </Button>
                </div>
            </Card.Body>
        </Card>
    );
}
