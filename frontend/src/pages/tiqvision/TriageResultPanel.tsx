import { Badge, Card, Col, Form, Row } from 'react-bootstrap';
import type { TicketCategory, TicketPriority } from '../../types/models';
import type { ScopeClassification, TriageAnalysis } from '../../types/triage';
import { formatLabel } from '../tiqradar/TicketCard';

export const CATEGORIES: TicketCategory[] = ['NETWORK', 'EMAIL', 'VPN', 'PRINTER', 'SOFTWARE', 'HARDWARE', 'ACCOUNT', 'OTHER'];
const PRIORITIES: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export interface TicketDraft {
    title: string;
    description: string;
    category: TicketCategory;
    affectedSystem: string;
    priority: TicketPriority;
}

const CLASSIFICATION_LABEL: Record<ScopeClassification, { label: string; variant: string }> = {
    IT_SUPPORTED: { label: 'IT issue · self-service may help', variant: 'success' },
    IT_UNSUPPORTED: { label: 'IT issue · needs a technician', variant: 'warning' },
    SECURITY_RISK: { label: 'Security risk · escalate', variant: 'danger' },
    UNCERTAIN: { label: 'Unclear · more detail needed', variant: 'info' },
    INSUFFICIENT_INFO: { label: 'Not enough information', variant: 'info' },
    NON_IT: { label: 'Not an IT issue', variant: 'secondary' },
};

const CONFIDENCE_TEXT = { HIGH: 'Clear evidence', MEDIUM: 'Some evidence', LOW: 'Weak evidence' } as const;

export function draftFrom(result: TriageAnalysis, fallbackText: string): TicketDraft {
    const parts = [result.description ?? fallbackText];
    if (result.visibleErrorCode && !parts[0]?.includes(result.visibleErrorCode)) {
        parts.push(`Visible error code: ${result.visibleErrorCode}`);
    }
    return {
        title: (result.title || fallbackText || 'IT issue').slice(0, 200),
        description: parts.filter(Boolean).join('\n').slice(0, 2000),
        category: result.category ?? 'OTHER',
        affectedSystem: (result.affectedSystem || '').slice(0, 100),
        priority: result.suggestedPriority ?? 'MEDIUM',
    };
}

interface TriageResultPanelProps {
    result: TriageAnalysis;
    draft: TicketDraft;
    onDraftChange: (draft: TicketDraft) => void;
    disabled?: boolean;
}

export default function TriageResultPanel({ result, draft, onDraftChange, disabled }: TriageResultPanelProps) {
    const cls = CLASSIFICATION_LABEL[result.classification];
    const set = <K extends keyof TicketDraft>(key: K, value: TicketDraft[K]) => onDraftChange({ ...draft, [key]: value });

    return (
        <Card className="tiqfix-remedy shadow-sm">
            <Card.Body className="p-4">
                {result.demo && (
                    <div className="tiqvision-demo-flag mb-3" role="note">
                        🎬 DEMO FIXTURE – simulated result. The uploaded image was not analyzed.
                    </div>
                )}
                <div className="d-flex flex-wrap gap-2 align-items-center mb-2">
                    <Badge bg={cls.variant} text={cls.variant === 'warning' || cls.variant === 'info' ? 'dark' : undefined}>
                        {cls.label}
                    </Badge>
                    <Badge bg="light" text="dark" className="border" title="The AI's own estimate, not a measured probability">
                        🔎 {CONFIDENCE_TEXT[result.confidenceLabel]}
                    </Badge>
                    {result.imageCount > 0 && <Badge bg="dark">{result.imageCount} image{result.imageCount > 1 ? 's' : ''} analyzed</Badge>}
                </div>
                {result.reason && <p className="mb-3">{result.reason}</p>}

                {result.promptInjectionDetected && (
                    <div className="tiqvision-warning mb-3" role="note">
                        ⚠️ The screenshot contained instructions aimed at an AI. They were ignored.
                    </div>
                )}

                <Row className="g-3 mb-3">
                    {result.visibleErrorCode && (
                        <Col sm={6}>
                            <div className="tiqfix-info h-100">
                                <div className="small text-uppercase text-muted fw-semibold">Visible error code</div>
                                <code className="fs-5">{result.visibleErrorCode}</code>
                            </div>
                        </Col>
                    )}
                    {result.possibleCause && (
                        <Col sm={result.visibleErrorCode ? 6 : 12}>
                            <div className="tiqfix-info h-100">
                                <div className="small text-uppercase text-muted fw-semibold">Possible cause (not confirmed)</div>
                                <div>{result.possibleCause}</div>
                            </div>
                        </Col>
                    )}
                </Row>

                {result.evidence.length > 0 && (
                    <details className="mb-3">
                        <summary className="small fw-semibold">What the AI saw ({result.evidence.length})</summary>
                        <ul className="small mt-2 mb-0">
                            {result.evidence.map((e, i) => <li key={i}>{e}</li>)}
                        </ul>
                    </details>
                )}

                <h3 className="h6 text-uppercase text-muted mt-2">Review your ticket</h3>
                <p className="small text-muted">Check and edit everything below. Nothing is sent to IT until you click Submit.</p>
                <Form.Group className="mb-2" controlId="tv-title">
                    <Form.Label className="small">Title</Form.Label>
                    <Form.Control value={draft.title} maxLength={200} disabled={disabled}
                                  onChange={(e) => set('title', e.target.value)} />
                </Form.Group>
                <Row className="g-2">
                    <Col sm={4}>
                        <Form.Group className="mb-2" controlId="tv-category">
                            <Form.Label className="small">Category</Form.Label>
                            <Form.Select value={draft.category} disabled={disabled}
                                         onChange={(e) => set('category', e.target.value as TicketCategory)}>
                                {CATEGORIES.map((c) => <option key={c} value={c}>{formatLabel(c)}</option>)}
                            </Form.Select>
                        </Form.Group>
                    </Col>
                    <Col sm={5}>
                        <Form.Group className="mb-2" controlId="tv-system">
                            <Form.Label className="small">Affected system</Form.Label>
                            <Form.Control value={draft.affectedSystem} maxLength={100} disabled={disabled}
                                          onChange={(e) => set('affectedSystem', e.target.value)} />
                        </Form.Group>
                    </Col>
                    <Col sm={3}>
                        <Form.Group className="mb-2" controlId="tv-priority">
                            <Form.Label className="small">Priority</Form.Label>
                            <Form.Select value={draft.priority} disabled={disabled}
                                         onChange={(e) => set('priority', e.target.value as TicketPriority)}>
                                {PRIORITIES.map((p) => <option key={p} value={p}>{formatLabel(p)}</option>)}
                            </Form.Select>
                        </Form.Group>
                    </Col>
                </Row>
                <Form.Group controlId="tv-description">
                    <Form.Label className="small">Description</Form.Label>
                    <Form.Control as="textarea" rows={4} maxLength={2000} value={draft.description} disabled={disabled}
                                  onChange={(e) => set('description', e.target.value)} />
                </Form.Group>
            </Card.Body>
        </Card>
    );
}
