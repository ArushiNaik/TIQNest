// Plain interfaces that mirror the backend DTOs. Keep them simple.

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type UserRole = 'EMPLOYEE' | 'ADMIN';

export interface Ticket {
    ticketId: string;
    title: string;
    description: string;
    location: string;
    status: TicketStatus;
    priority: TicketPriority;
    reporterId: string;
    assigneeId: string;
    incidentId?: string;
    createdAt: string;
    slaDeadline?: string;
}

export interface User {
    userId: string;
    name: string;
    email: string;
    role: UserRole;
    password?: string; // only sent when creating or changing a user, never displayed
}

export interface LoginResponse {
    userId: string;
    name: string;
    role: UserRole;
    message?: string;
}
