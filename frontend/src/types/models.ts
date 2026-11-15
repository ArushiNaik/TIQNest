export type UserRole = 'EMPLOYEE' | 'ADMIN';

export type TicketCategory = 'NETWORK' | 'EMAIL' | 'VPN' | 'PRINTER' | 'SOFTWARE' | 'HARDWARE' | 'ACCOUNT' | 'OTHER';

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'LINKED_TO_INCIDENT' | 'RESOLVED' | 'CLOSED';

export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type SlaStatus = 'ON_TRACK' | 'AT_RISK' | 'BREACHED' | 'MET';

export interface Ticket {
    id: number;
    title: string;
    description: string | null;
    category: TicketCategory;
    affectedSystem: string;
    requesterEmail: string;
    status: TicketStatus;
    priority: TicketPriority;
    assignee: string | null;
    createdAt: string;
    slaDeadline: string | null;
    slaStatus: SlaStatus | null;
    resolvedAt: string | null;
    incidentId: number | null;
}

export interface CreateTicketRequest {
    title: string;
    description: string;
    category: TicketCategory;
    affectedSystem: string;
    requesterEmail: string;
    priority?: TicketPriority;
}

export interface UpdateTicketRequest {
    status?: TicketStatus;
    priority?: TicketPriority;
    assignee?: string;
    actor?: string;
}

export type IncidentStatus = 'ACTIVE' | 'RESOLVED';

export interface Incident {
    id: number;
    title: string;
    category: TicketCategory;
    affectedSystem: string;
    status: IncidentStatus;
    ticketCount: number;
    detectedAt: string;
    lastReportAt: string | null;
    resolvedAt: string | null;
    bannerMessage: string;
}

export interface SlaSummary {
    onTrack: number;
    atRisk: number;
    breached: number;
    met: number;
    total: number;
}

export type AuditAction = 'CREATED' | 'STATUS_CHANGED' | 'PRIORITY_CHANGED' | 'ASSIGNED' | 'LINKED_TO_INCIDENT';

export interface AuditEvent {
    id: number;
    ticketId: number;
    action: AuditAction;
    oldValue: string | null;
    newValue: string | null;
    actor: string;
    message: string;
    createdAt: string;
}

export interface User {
    userId: string;
    name: string;
    email: string;
    role: UserRole;
}

export interface UserRequest {
    name: string;
    email: string;
    password: string;
    role: UserRole;
}

export interface LoginResponse {
    token: string;
    user: User;
}

export interface ApiError {
    status: number;
    message?: string;
    fieldErrors?: Record<string, string>;
    code?: string;
    suggestions?: string[];
    canRequestAdminReview?: boolean;
}
