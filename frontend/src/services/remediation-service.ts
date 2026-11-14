import { getData, postData } from './api-services';
import { API_BASE_URL, API_URL_TICKETS } from './config';
import type { CreateTicketRequest, Ticket } from '../types/models';
import {
    toTiqFixTicket,
    type RecommendationRequest,
    type RecommendationResponse,
    type Remedy,
    type RemediationAttempt,
    type RemediationAttemptRequest,
    type TiqFixTicket,
} from '../types/remediation';

const API_URL_REMEDIATIONS = `${API_BASE_URL}/remediations`;

export function getRecommendations(request: RecommendationRequest): Promise<RecommendationResponse> {
    return postData<RecommendationResponse>(`${API_URL_REMEDIATIONS}/recommend`, request);
}

export function getCatalog(): Promise<Remedy[]> {
    return getData<Remedy[]>(`${API_URL_REMEDIATIONS}/catalog`);
}

export function recordAttempt(request: RemediationAttemptRequest): Promise<RemediationAttempt> {
    return postData<RemediationAttempt>(`${API_URL_REMEDIATIONS}/attempts`, request);
}

export function getAttempts(ticketId?: number): Promise<RemediationAttempt[]> {
    const query = ticketId ? `?ticketId=${encodeURIComponent(ticketId)}` : '';
    return getData<RemediationAttempt[]>(`${API_URL_REMEDIATIONS}/attempts${query}`);
}

export async function getTicketForTiqFix(ticketId: number): Promise<TiqFixTicket> {
    const raw = await getData<unknown>(`${API_URL_TICKETS}/${encodeURIComponent(ticketId)}`);
    const ticket = toTiqFixTicket(raw);
    if (!ticket) throw new Error(`Ticket #${ticketId} could not be read.`);
    return ticket;
}

export function createEscalationTicket(request: CreateTicketRequest): Promise<Ticket> {
    return postData<Ticket>(API_URL_TICKETS, request);
}
