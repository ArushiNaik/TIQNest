import type { ApiError } from '../types/models';
import { clearSession, getSession } from './auth';

export class ApiRequestError extends Error {
    status: number;
    fieldErrors?: Record<string, string>;

    constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

async function request<T>(method: string, apiUrl: string, body?: unknown): Promise<T> {
    const headers: Record<string, string> = {};
    const token = getSession()?.token;
    if (token) headers['Authorization'] = `Bearer ${token}`;
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    let response: Response;
    try {
        response = await fetch(apiUrl, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
        });
    } catch {
        throw new ApiRequestError(0, 'Cannot reach the server. Is the backend running on port 8080?');
    }

    const text = await response.text();
    if (!response.ok) {
        if (response.status === 401 && token) {
            clearSession();
            window.location.assign('/login');
        }
        let error: Partial<ApiError> = {};
        try {
            error = JSON.parse(text) as ApiError;
        } catch {
        }
        throw new ApiRequestError(response.status, error.message || `Request failed (${response.status})`, error.fieldErrors);
    }
    return (text ? JSON.parse(text) : undefined) as T;
}

export function getData<T>(apiUrl: string): Promise<T> {
    return request<T>('GET', apiUrl);
}

export function postData<T>(apiUrl: string, newData?: unknown): Promise<T> {
    return request<T>('POST', apiUrl, newData);
}

export function patchData<T>(apiUrl: string, changes?: unknown): Promise<T> {
    return request<T>('PATCH', apiUrl, changes);
}
