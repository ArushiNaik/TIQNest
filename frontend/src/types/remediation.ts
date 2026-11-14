// TiqFix types. These mirror the backend records in com.konate.ithelpdesk.remediation.PresentationLayer.
import type { TicketCategory } from './models';

export type IssueType = 'VPN' | 'PRINTER' | 'DNS' | 'BROWSER_CACHE' | 'OTHER';

export type RemedyActionType =
    | 'SETTINGS_SHORTCUT'
    | 'GUIDED_TROUBLESHOOTING'
    | 'DIAGNOSTIC_COMMAND'
    | 'REPAIR_COMMAND'
    | 'ESCALATION';

export type SafetyLevel = 'SAFE' | 'LOW_RISK' | 'IT_ONLY';

export type RemediationOutcome = 'SUCCESS' | 'FAILED' | 'UNKNOWN';

export interface RemedyAction {
    id: string;
    type: RemedyActionType;
    label: string;
    /** ms-settings deep link or command text from the approved server-side catalog. */
    value?: string;
    shell?: string;
    explanation: string;
    effects?: string;
}

export interface Remedy {
    remedyId: string;
    title: string;
    explanation: string;
    issueType: IssueType;
    supportedPlatform: string;
    safetyLevel: SafetyLevel;
    actionType: RemedyActionType;
    deepLink?: string;
    command?: string;
    actions: RemedyAction[];
    steps: string[];
    expectedResult: string;
    cautions: string[];
    userConfirmationRequired: boolean;
    matchReason?: string;
    confidence?: number;
}

export interface RecommendationRequest {
    ticketId?: number;
    issueType?: IssueType;
    category?: TicketCategory;
    affectedSystem?: string;
    description?: string;
}

export interface RecommendationResponse {
    ticketId?: number;
    ticketTitle?: string;
    detectedIssue: IssueType;
    approvedRemedyAvailable: boolean;
    message: string;
    recommendations: Remedy[];
}

export interface RemediationAttemptRequest {
    remedyId: string;
    ticketId?: number;
    outcome: RemediationOutcome;
    feedback?: string;
}

export interface RemediationAttempt {
    id: number;
    remedyId: string;
    remedyTitle: string;
    ticketId: number | null;
    outcome: RemediationOutcome;
    feedback: string | null;
    reportedBy: string | null;
    createdAt: string;
}

/**
 * The ticket details TiqFix needs. Built through toTiqFixTicket() so TiqFix does not depend on
 * which ticket shape a given branch of the frontend uses.
 */
export interface TiqFixTicket {
    id: number;
    title: string;
    description: string;
    category: TicketCategory | null;
    affectedSystem: string;
    status: string | null;
}

const TICKET_CATEGORIES: readonly string[] = [
    'NETWORK', 'EMAIL', 'VPN', 'PRINTER', 'SOFTWARE', 'HARDWARE', 'ACCOUNT', 'OTHER',
];

function str(value: unknown): string {
    return typeof value === 'string' ? value : '';
}

/**
 * Integration boundary between ticket JSON and TiqFix.
 * Accepts the backend TicketResponse ({ id, category, affectedSystem, ... }) as well as the older
 * frontend shape ({ ticketId, location, priority, ... }) and returns null if there is no usable ID.
 */
export function toTiqFixTicket(raw: unknown): TiqFixTicket | null {
    if (!raw || typeof raw !== 'object') return null;
    const t = raw as Record<string, unknown>;
    const id = Number(t.id ?? t.ticketId);
    if (!Number.isInteger(id) || id <= 0) return null;
    const category = str(t.category).toUpperCase();
    return {
        id,
        title: str(t.title) || `Ticket #${id}`,
        description: str(t.description),
        category: TICKET_CATEGORIES.includes(category) ? (category as TicketCategory) : null,
        affectedSystem: str(t.affectedSystem) || str(t.location),
        status: str(t.status) || null,
    };
}
