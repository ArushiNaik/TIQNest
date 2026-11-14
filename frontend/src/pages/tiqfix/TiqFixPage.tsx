import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { Alert, Badge, Button, Card, Col, Container, Form, Row, Spinner } from 'react-bootstrap';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getRecommendations, getTicketForTiqFix } from '../../services/remediation-service';
import type { IssueType, RecommendationRequest, RecommendationResponse, TiqFixTicket } from '../../types/remediation';
import { formatLabel } from '../TicketCard';
import EscalationModal from './EscalationModal';
import RemediationCard from './RemediationCard';
import RemediationFeedback from './RemediationFeedback';
import RemediationHistory from './RemediationHistory';
import { ISSUE_OPTIONS, REMEDY_ICON, issueOption } from './tiqfix-ui';
import './TiqFix.css';

function parseTicketId(raw: string | null): number | null | 'invalid' {
    if (raw == null || raw === '') return null;
    const id = Number(raw);
    return Number.isInteger(id) && id > 0 ? id : 'invalid';
}

export default function TiqFixPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const ticketParam = parseTicketId(searchParams.get('ticketId'));
    const ticketId = typeof ticketParam === 'number' ? ticketParam : undefined;

    const [ticket, setTicket] = useState<TiqFixTicket | null>(null);
    const [ticketLoading, setTicketLoading] = useState(false);
    const [ticketError, setTicketError] = useState<string | null>(null);

    const [selectedIssue, setSelectedIssue] = useState<IssueType | null>(null);
    const [description, setDescription] = useState('');
    const [formError, setFormError] = useState<string | null>(null);

    const [result, setResult] = useState<RecommendationResponse | null>(null);
    const [activeIndex, setActiveIndex] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [actionTaken, setActionTaken] = useState(false);
    const [historyKey, setHistoryKey] = useState(0);
    const [showEscalation, setShowEscalation] = useState(false);
    const resultRef = useRef<HTMLDivElement>(null);
    const requestSeq = useRef(0);

    const requestRecommendation = useCallback(async (request: RecommendationRequest) => {
        const seq = ++requestSeq.current;
        setLoading(true);
        setError(null);
        setActionTaken(false);
        setActiveIndex(0);
        try {
            const response = await getRecommendations(request);
            if (seq !== requestSeq.current) return;
            setResult(response);
            setTimeout(() => resultRef.current?.focus({ preventScroll: false }), 50);
        } catch (err) {
            if (seq !== requestSeq.current) return;
            setResult(null);
            setError((err as Error).message);
        } finally {
            if (seq === requestSeq.current) setLoading(false);
        }
    }, []);

    useEffect(() => {
        setTicket(null);
        setTicketError(null);
        setResult(null);
        if (ticketId == null) return;
        let cancelled = false;
        setTicketLoading(true);
        getTicketForTiqFix(ticketId)
            .then((t) => {
                if (cancelled) return;
                setTicket(t);
                requestRecommendation({ ticketId: t.id });
            })
            .catch((err) => !cancelled && setTicketError((err as Error).message))
            .finally(() => !cancelled && setTicketLoading(false));
        return () => {
            cancelled = true;
        };
    }, [ticketId, requestRecommendation]);

    const handleSelectIssue = (type: IssueType) => {
        setSelectedIssue(type);
        setFormError(null);
        requestRecommendation({ issueType: type, description: description.trim() || undefined });
    };

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        const text = description.trim();
        if (!text && !selectedIssue) {
            setFormError('Choose a problem type or describe what’s happening.');
            return;
        }
        setFormError(null);
        requestRecommendation({ issueType: selectedIssue ?? undefined, description: text || undefined });
    };

    const leaveTicketMode = () => navigate('/fix');

    const remedy = result?.recommendations[activeIndex] ?? null;
    const alternatives = result?.recommendations.filter((_, i) => i !== activeIndex) ?? [];
    const isEscalation = remedy?.actionType === 'ESCALATION';
    const escalationIssue: IssueType = selectedIssue ?? result?.detectedIssue ?? 'OTHER';

    return (
        <Container className="py-4 tiqfix">
            <header className="tiqfix-hero mb-4">
                <div className="d-flex align-items-center gap-3">
                    <div className="tiqfix-hero-icon" aria-hidden="true">🛠️</div>
                    <div>
                        <h1 className="h2 mb-1 fw-bold">TiqFix — Smart IT Repair</h1>
                        <p className="mb-0 opacity-75">Safe, guided solutions for everyday IT problems.</p>
                    </div>
                </div>
                <ul className="tiqfix-promises list-unstyled d-flex flex-wrap gap-2 mt-3 mb-0">
                    <li>✅ IT-approved fixes only</li>
                    <li>🔒 Nothing runs without you</li>
                    <li>📝 Every result is logged</li>
                </ul>
            </header>

            <Row className="g-4">
                <Col lg={5} xl={4} className="d-flex flex-column gap-4">
                    {ticketParam === 'invalid' && (
                        <Alert variant="warning">
                            That ticket link isn’t valid. <Alert.Link onClick={leaveTicketMode}>Choose a problem instead</Alert.Link>
                        </Alert>
                    )}

                    {ticketId != null ? (
                        <Card className="shadow-sm">
                            <Card.Body>
                                <div className="small text-uppercase text-muted fw-semibold mb-2">Fixing ticket #{ticketId}</div>
                                {ticketLoading && <div className="d-flex align-items-center gap-2"><Spinner size="sm" animation="border" /> Loading ticket…</div>}
                                {ticketError && (
                                    <Alert variant="danger" className="mb-0">
                                        {ticketError}
                                        <div className="mt-2"><Alert.Link onClick={leaveTicketMode}>Continue without a ticket</Alert.Link></div>
                                    </Alert>
                                )}
                                {ticket && (
                                    <>
                                        <h2 className="h5">{ticket.title}</h2>
                                        {ticket.description && <p className="text-muted mb-2">{ticket.description}</p>}
                                        <div className="d-flex flex-wrap gap-1 mb-3">
                                            {ticket.category && <Badge bg="secondary">{formatLabel(ticket.category)}</Badge>}
                                            {ticket.affectedSystem && <Badge bg="light" text="dark" className="border">💻 {ticket.affectedSystem}</Badge>}
                                            {ticket.status && <Badge bg="primary">{formatLabel(ticket.status)}</Badge>}
                                        </div>
                                        {result && (
                                            <div className="tiqfix-detected mb-3">
                                                <span aria-hidden="true">{REMEDY_ICON[result.detectedIssue]}</span>{' '}
                                                Detected problem: <strong>{result.detectedIssue === 'OTHER' ? 'Needs a technician' : issueOption(result.detectedIssue).label}</strong>
                                            </div>
                                        )}
                                        <Button variant="link" className="p-0" onClick={leaveTicketMode}>Not right? Choose a different problem</Button>
                                    </>
                                )}
                            </Card.Body>
                        </Card>
                    ) : (
                        <Card className="shadow-sm">
                            <Card.Body>
                                <h2 className="h5 mb-1">What’s going wrong?</h2>
                                <p className="text-muted small">Pick a problem or describe it – TiqFix finds the matching approved fix.</p>
                                <div className="tiqfix-issues" role="radiogroup" aria-label="Problem type">
                                    {ISSUE_OPTIONS.map((o) => (
                                        <button
                                            key={o.type}
                                            type="button"
                                            role="radio"
                                            aria-checked={selectedIssue === o.type}
                                            className={`tiqfix-issue ${selectedIssue === o.type ? 'active' : ''}`}
                                            onClick={() => handleSelectIssue(o.type)}
                                        >
                                            <span className="tiqfix-issue-icon" aria-hidden="true">{o.icon}</span>
                                            <span>
                                                <span className="d-block fw-semibold">{o.label}</span>
                                                <span className="d-block small text-muted">{o.hint}</span>
                                            </span>
                                        </button>
                                    ))}
                                </div>
                                <Form onSubmit={handleSubmit} className="mt-3">
                                    <Form.Group controlId="tiqfix-description">
                                        <Form.Label className="fw-semibold">Describe the problem <span className="text-muted fw-normal">(optional)</span></Form.Label>
                                        <Form.Control
                                            as="textarea"
                                            rows={3}
                                            maxLength={2000}
                                            value={description}
                                            placeholder="e.g. My VPN keeps disconnecting."
                                            onChange={(e) => setDescription(e.target.value)}
                                            onKeyDown={(e) => {
                                                if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) handleSubmit(e);
                                            }}
                                            isInvalid={!!formError}
                                        />
                                        <Form.Control.Feedback type="invalid">{formError}</Form.Control.Feedback>
                                    </Form.Group>
                                    <Button type="submit" className="w-100 mt-3" size="lg" disabled={loading}>
                                        {loading ? <><Spinner size="sm" animation="border" className="me-2" />Finding a fix…</> : '✨ Try TiqFix'}
                                    </Button>
                                </Form>
                            </Card.Body>
                        </Card>
                    )}

                    <RemediationHistory ticketId={ticketId} refreshKey={historyKey} />
                </Col>

                <Col lg={7} xl={8}>
                    <div ref={resultRef} tabIndex={-1} className="tiqfix-result" aria-live="polite" aria-busy={loading}>
                        {loading && (
                            <Card className="shadow-sm text-center py-5">
                                <Card.Body>
                                    <Spinner animation="border" variant="primary" role="status" />
                                    <p className="mt-3 mb-0 text-muted">Matching your problem against IT-approved fixes…</p>
                                </Card.Body>
                            </Card>
                        )}

                        {!loading && error && (
                            <Alert variant="danger">
                                <Alert.Heading className="h6">We couldn’t get a recommendation</Alert.Heading>
                                {error}
                            </Alert>
                        )}

                        {!loading && !error && !result && !ticketLoading && !ticketError && (
                            <Card className="shadow-sm tiqfix-empty text-center">
                                <Card.Body className="py-5">
                                    <div className="display-5 mb-2" aria-hidden="true">🧰</div>
                                    <h2 className="h5">Your fix will appear here</h2>
                                    <p className="text-muted mb-0 mx-auto" style={{ maxWidth: 420 }}>
                                        Choose a problem on the left. TiqFix only suggests fixes reviewed by IT – it never runs anything on your computer by itself.
                                    </p>
                                </Card.Body>
                            </Card>
                        )}

                        {!loading && result && remedy && (
                            <div className="d-flex flex-column gap-4">
                                <div className={`tiqfix-verdict ${result.approvedRemedyAvailable ? 'ok' : 'escalate'}`}>
                                    {result.approvedRemedyAvailable ? '✨ ' : '🎧 '}
                                    {result.message}
                                </div>

                                <RemediationCard
                                    remedy={remedy}
                                    onActionTaken={() => setActionTaken(true)}
                                    onEscalate={() => setShowEscalation(true)}
                                />

                                {alternatives.length > 0 && (
                                    <div>
                                        <div className="small text-uppercase text-muted fw-semibold mb-2">Other approved options</div>
                                        <div className="d-flex flex-wrap gap-2">
                                            {alternatives.map((alt) => (
                                                <Button
                                                    key={alt.remedyId}
                                                    variant="outline-secondary"
                                                    onClick={() => {
                                                        setActiveIndex(result.recommendations.indexOf(alt));
                                                        setActionTaken(false);
                                                    }}
                                                >
                                                    {REMEDY_ICON[alt.issueType]} {alt.title}
                                                </Button>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {!isEscalation && (actionTaken ? (
                                    <RemediationFeedback
                                        remedy={remedy}
                                        ticketId={ticket?.id}
                                        onRecorded={() => setHistoryKey((k) => k + 1)}
                                        onEscalate={() => setShowEscalation(true)}
                                    />
                                ) : (
                                    <div className="text-center text-muted small">
                                        Try the action above, then tell us if it worked.{' '}
                                        <Button variant="link" size="sm" className="p-0 align-baseline" onClick={() => setActionTaken(true)}>
                                            Already tried it?
                                        </Button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </Col>
            </Row>

            <EscalationModal
                show={showEscalation}
                onHide={() => setShowEscalation(false)}
                ticket={ticket}
                issueType={escalationIssue}
                description={ticket ? ticket.description : description}
                remedy={remedy}
            />
        </Container>
    );
}
