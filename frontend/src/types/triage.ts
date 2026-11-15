// TiqVision / IT-scope types. Mirror com.konate.ithelpdesk.triage.PresentationLayer.
import type { TicketCategory, TicketPriority } from './models';
import type { IssueType } from './remediation';

export type ScopeClassification =
    | 'IT_SUPPORTED'
    | 'IT_UNSUPPORTED'
    | 'NON_IT'
    | 'UNCERTAIN'
    | 'SECURITY_RISK'
    | 'INSUFFICIENT_INFO';

export type NextStep = 'TRY_FIX' | 'CREATE_TICKET' | 'ESCALATE_SECURITY' | 'CLARIFY';

export interface TriageAnalysis {
    classification: ScopeClassification;
    isITRelated: boolean;
    /** The model's own estimate, not a calibrated probability. */
    confidence: number;
    confidenceLabel: 'HIGH' | 'MEDIUM' | 'LOW';
    reason: string | null;
    title: string;
    description: string | null;
    category: TicketCategory;
    affectedSystem: string;
    visibleErrorCode: string | null;
    possibleCause: string | null;
    evidence: string[];
    suggestedRemedyIds: string[];
    recommendedNextStep: NextStep;
    requiresUserReview: boolean;
    suggestedPriority: TicketPriority;
    clarifyingQuestion: string | null;
    promptInjectionDetected: boolean;
    source: 'CLAUDE' | 'DEMO_FIXTURE';
    demo: boolean;
    imageCount: number;
}

export interface ClassifyResult {
    classification: ScopeClassification;
    isITRelated: boolean;
    confidence: number;
    reason?: string;
    clarifyingQuestion?: string;
    issueType?: IssueType;
    category?: TicketCategory;
    source: 'AI' | 'RULES' | 'RULES_AI_UNAVAILABLE';
    recommendedNextStep: NextStep;
    suggestedPriority: TicketPriority;
    suggestedRemedyIds: string[];
}

export interface TriageStatus {
    aiConfigured: boolean;
    demoFixturesEnabled: boolean;
}

export interface AdminReview {
    id: number;
    requesterEmail: string;
    submittedText: string;
    classification: string | null;
    status: 'OPEN' | 'CLOSED';
    createdAt: string;
    closedAt: string | null;
    closedBy: string | null;
}
