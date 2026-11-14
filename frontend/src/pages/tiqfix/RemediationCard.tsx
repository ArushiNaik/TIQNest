import { useEffect, useRef, useState } from 'react';
import { Badge, Card, Collapse, OverlayTrigger, Tooltip } from 'react-bootstrap';
import type { Remedy, RemedyAction } from '../../types/remediation';
import RemediationDetails from './RemediationDetails';
import { ACTION_TYPE_LABEL, REMEDY_ICON, SAFETY, confidenceLabel } from './tiqfix-ui';

interface RemediationCardProps {
    remedy: Remedy;
    onActionTaken: (action: RemedyAction) => void;
    onEscalate: () => void;
}

export default function RemediationCard({ remedy, onActionTaken, onEscalate }: RemediationCardProps) {
    const escalation = remedy.actionType === 'ESCALATION';
    const guided = remedy.actionType === 'GUIDED_TROUBLESHOOTING';
    const [showSteps, setShowSteps] = useState(!guided);
    const stepsRef = useRef<HTMLDivElement>(null);
    const safety = SAFETY[remedy.safetyLevel];
    const match = confidenceLabel(remedy.confidence);

    useEffect(() => setShowSteps(!guided), [remedy.remedyId, guided]);

    const viewInstructions = () => {
        setShowSteps(true);
        setTimeout(() => stepsRef.current?.focus(), 350);
    };

    return (
        <Card className={`tiqfix-remedy shadow-sm tiqfix-safety-${remedy.safetyLevel.toLowerCase()}`}>
            <Card.Body className="p-4">
                <div className="d-flex gap-3 align-items-start">
                    <div className="tiqfix-remedy-icon" aria-hidden="true">{REMEDY_ICON[remedy.issueType]}</div>
                    <div className="flex-grow-1">
                        <div className="text-uppercase small fw-semibold text-primary tiqfix-eyebrow">
                            {escalation ? 'Next step' : 'Recommended fix'}
                        </div>
                        <h3 className="h4 mb-2">{remedy.title}</h3>
                        <div className="d-flex flex-wrap gap-2">
                            <OverlayTrigger placement="top" overlay={<Tooltip id={`safety-${remedy.remedyId}`}>{safety.detail}</Tooltip>}>
                                <Badge bg={safety.variant} text={safety.variant === 'warning' ? 'dark' : undefined} tabIndex={0}>
                                    🛡️ {safety.label}
                                </Badge>
                            </OverlayTrigger>
                            <Badge bg="primary" className="bg-opacity-75">{ACTION_TYPE_LABEL[remedy.actionType]}</Badge>
                            <Badge bg="light" text="dark" className="border">💻 {remedy.supportedPlatform}</Badge>
                            {match && !escalation && <Badge bg="info" text="dark">🎯 {match}</Badge>}
                        </div>
                    </div>
                </div>

                <p className="lead fs-6 mt-3 mb-3">{remedy.explanation}</p>

                {remedy.matchReason && (
                    <div className="tiqfix-why mb-4">
                        <strong>Why this {escalation ? 'step' : 'fix'}?</strong> {remedy.matchReason}
                    </div>
                )}

                <RemediationDetails
                    actions={remedy.actions}
                    onActionTaken={onActionTaken}
                    onViewInstructions={viewInstructions}
                    onEscalate={onEscalate}
                />

                <Collapse in={showSteps}>
                    <div>
                        <div ref={stepsRef} tabIndex={-1} className="mt-4 tiqfix-steps" aria-label="Step-by-step instructions">
                            <h4 className="h6 text-uppercase text-muted">{escalation ? 'Before you contact IT' : 'Step-by-step'}</h4>
                            <ol className="mb-0">
                                {remedy.steps.map((step, i) => <li key={i}>{step}</li>)}
                            </ol>
                        </div>
                    </div>
                </Collapse>

                <div className="row g-3 mt-2">
                    <div className="col-md-6">
                        <div className="tiqfix-info h-100">
                            <div className="small text-uppercase text-muted fw-semibold">Expected result</div>
                            <div>{remedy.expectedResult}</div>
                        </div>
                    </div>
                    <div className="col-md-6">
                        <div className="tiqfix-info h-100">
                            <div className="small text-uppercase text-muted fw-semibold">Safety</div>
                            <div>{safety.detail}</div>
                            {remedy.cautions.map((c, i) => <div key={i} className="small text-muted mt-1">⚠️ {c}</div>)}
                        </div>
                    </div>
                </div>
            </Card.Body>
        </Card>
    );
}
