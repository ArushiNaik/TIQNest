export type UserRole = 'EMPLOYEE' | 'ADMIN';

export type TicketCategory = 'NETWORK' | 'EMAIL' | 'VPN' | 'PRINTER' | 'SOFTWARE' | 'HARDWARE' | 'ACCOUNT' | 'OTHER';

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'LINKED_TO_INCIDENT' | 'RESOLVED' | 'CLOSED';

export interface Ticket {
    id: number;
    title: string;
    description: string | null;
    category: TicketCategory;
    affectedSystem: string;
    requesterEmail: string;
    status: TicketStatus;
    createdAt: string;
    incidentId: number | null;
}

export interface CreateTicketRequest {
    title: string;
    description: string;
    category: TicketCategory;
    affectedSystem: string;
    requesterEmail: string;
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
}
