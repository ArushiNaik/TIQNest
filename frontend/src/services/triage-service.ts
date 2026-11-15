// TiqVision API calls. Images go to our backend only; the Claude key never reaches the browser.
import { getData, patchData, postData, postForm } from './api-services';
import { API_BASE_URL } from './config';
import type { AdminReview, ClassifyResult, TriageAnalysis, TriageStatus } from '../types/triage';

const API_URL_TRIAGE = `${API_BASE_URL}/triage`;

export function getTriageStatus(): Promise<TriageStatus> {
    return getData<TriageStatus>(`${API_URL_TRIAGE}/status`);
}

export function classifyText(description: string): Promise<ClassifyResult> {
    return postData<ClassifyResult>(`${API_URL_TRIAGE}/classify`, { description });
}

export function analyzeImages(files: Blob[], description: string, demo?: string): Promise<TriageAnalysis> {
    const form = new FormData();
    files.forEach((file, i) => form.append('files', file, file instanceof File ? file.name : `frame-${i + 1}.jpg`));
    if (description.trim()) form.append('description', description.trim());
    if (demo) form.append('demo', demo);
    return postForm<TriageAnalysis>(`${API_URL_TRIAGE}/analyze`, form);
}

export function requestAdminReview(text: string, classification?: string): Promise<AdminReview> {
    return postData<AdminReview>(`${API_URL_TRIAGE}/reviews`, { text, classification, confirmed: true });
}

export function getAdminReviews(): Promise<AdminReview[]> {
    return getData<AdminReview[]>(`${API_URL_TRIAGE}/reviews`);
}

export function closeAdminReview(id: number): Promise<AdminReview> {
    return patchData<AdminReview>(`${API_URL_TRIAGE}/reviews/${id}/close`);
}
