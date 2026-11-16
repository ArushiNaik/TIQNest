import { useState, type FormEvent } from 'react';
import { Alert, Button, Form, Spinner } from 'react-bootstrap';
import { ApiRequestError } from '../../services/api-services';
import { requestAdminReview } from '../../services/triage-service';

interface NonItNoticeProps {
    /** What the employee submitted, sent only if they explicitly ask for a human review. */
    submittedText: string;
    onTryAgain: () => void;
}

/** Friendly out-of-scope state. No diagnosis, no fix, no ticket. Admin review is opt-in. */
export function NonItNotice({ submittedText, onTryAgain }: NonItNoticeProps) {
    const [confirming, setConfirming] = useState(false);
    const [sending, setSending] = useState(false);
    const [sent, setSent] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const send = async () => {
        setSending(true);
        setError(null);
        try {
            await requestAdminReview(submittedText.slice(0, 1000) || '(screenshot only)', 'NON_IT');
            setSent(true);
            setConfirming(false);
        } catch (err) {
            setError(err instanceof ApiRequestError ? err.message : 'Could not send the review request.');
        } finally {
            setSending(false);
        }
    };

    return (
        <Alert variant="secondary" className="tiqvision-notice shadow-sm" role="status">
            <Alert.Heading className="h5">🧭 This doesn't look like an IT support issue.</Alert.Heading>
            <p className="mb-2">
                TIQNest is designed to help with computers, software, network connections, accounts and technical
                problems. Please describe an IT-related issue.
            </p>
            <p className="small text-muted mb-3">No diagnosis was made, no ticket was created and no fix was suggested.</p>

            {sent ? (
                <Alert variant="success" className="py-2 mb-2">
                    Review requested. An IT administrator will take a look. This did not create a support ticket.
                </Alert>
            ) : confirming ? (
                <div className="tiqvision-confirm mb-2">
                    <p className="mb-2">
                        Send this request to an IT administrator for manual review? It will be attributed to you and will
                        <strong> not</strong> create a support ticket.
                    </p>
                    <div className="d-flex gap-2">
                        <Button variant="primary" size="sm" disabled={sending} onClick={send}>
                            {sending && <Spinner size="sm" animation="border" className="me-1" />}Yes, request review
                        </Button>
                        <Button variant="outline-light" size="sm" disabled={sending} onClick={() => setConfirming(false)}>
                            Cancel
                        </Button>
                    </div>
                </div>
            ) : null}
            {error && <Alert variant="warning" className="py-2 mb-2">{error}</Alert>}

            <div className="d-flex flex-wrap gap-2">
                <Button variant="primary" onClick={onTryAgain}>✏️ Try another prompt</Button>
                {!sent && !confirming && (
                    <Button variant="outline-light" onClick={() => setConfirming(true)}>🙋 Request IT admin review</Button>
                )}
            </div>
        </Alert>
    );
}

/** Security incidents: no self-service actions, direct to IT security with a high-priority ticket. */
export function SecurityNotice({ onCreateTicket, ticketLabel = 'Create high-priority security ticket' }:
                                   { onCreateTicket?: () => void; ticketLabel?: string }) {
    return (
        <Alert variant="danger" className="tiqvision-notice shadow-sm" role="alert">
            <Alert.Heading className="h5">🚨 This looks like a possible security incident.</Alert.Heading>
            <ul className="mb-3">
                <li>Do not click any more links or open attachments from the suspicious message.</li>
                <li>Do not enter your password anywhere until IT security has checked your account.</li>
                <li>Keep the device as it is and contact IT security now. Do not try cleanup tools or scripts.</li>
            </ul>
            <p className="small mb-3">TiqFix does not offer self-service actions for security incidents.</p>
            {onCreateTicket && <Button variant="light" onClick={onCreateTicket}>🛡️ {ticketLabel}</Button>}
        </Alert>
    );
}

/** Ambiguous or too-short input: ask one question and let the employee add detail. */
export function ClarifyNotice({ question, onAnswer, busy }: { question: string; onAnswer: (detail: string) => void; busy?: boolean }) {
    const [detail, setDetail] = useState('');
    const submit = (e: FormEvent) => {
        e.preventDefault();
        if (detail.trim()) onAnswer(detail.trim());
    };
    return (
        <Alert variant="info" className="tiqvision-notice shadow-sm">
            <Alert.Heading className="h6">🤔 A quick question</Alert.Heading>
            <p className="mb-2">{question}</p>
            <Form onSubmit={submit} className="d-flex gap-2">
                <Form.Control
                    aria-label="Add more detail"
                    placeholder="e.g. my laptop monitor shows flickering lines"
                    value={detail}
                    maxLength={500}
                    onChange={(e) => setDetail(e.target.value)}
                />
                <Button type="submit" disabled={busy || !detail.trim()}>
                    {busy ? <Spinner size="sm" animation="border" /> : 'Add detail'}
                </Button>
            </Form>
        </Alert>
    );
}
