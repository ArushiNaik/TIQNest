import { getData, patchData } from './api-services';
import { API_URL_INCIDENTS, API_URL_SLA, API_URL_TICKETS } from './config';
import type { AuditEvent, Incident, SlaSummary, Ticket, UpdateTicketRequest } from '../types/models';

export function getActiveIncidents(): Promise<Incident[]> {
    return getData<Incident[]>(`${API_URL_INCIDENTS}/active`);
}

export function getAllIncidents(): Promise<Incident[]> {
    return getData<Incident[]>(API_URL_INCIDENTS);
}

export function resolveIncident(id: number): Promise<Incident> {
    return patchData<Incident>(`${API_URL_INCIDENTS}/${id}/resolve`);
}

export function getSlaSummary(): Promise<SlaSummary> {
    return getData<SlaSummary>(`${API_URL_SLA}/summary`);
}

export function getSlaAlerts(): Promise<Ticket[]> {
    return getData<Ticket[]>(`${API_URL_SLA}/alerts`);
}

export function getQueue(): Promise<Ticket[]> {
    return getData<Ticket[]>(`${API_URL_TICKETS}/queue`);
}

export function getTicket(id: number): Promise<Ticket> {
    return getData<Ticket>(`${API_URL_TICKETS}/${id}`);
}

export function updateTicket(id: number, changes: UpdateTicketRequest): Promise<Ticket> {
    return patchData<Ticket>(`${API_URL_TICKETS}/${id}`, changes);
}

export function getTicketHistory(id: number): Promise<AuditEvent[]> {
    return getData<AuditEvent[]>(`${API_URL_TICKETS}/${id}/history`);
}

export const OPS_REFRESH_EVENT = 'ops:refresh';

export function notifyOpsChanged(): void {
    window.dispatchEvent(new Event(OPS_REFRESH_EVENT));
}
