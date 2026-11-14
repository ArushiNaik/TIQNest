import type { User } from '../types/models';

export interface Session {
    token: string;
    user: User;
}

const STORAGE_KEY = 'session';

export function getSession(): Session | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
        return JSON.parse(raw) as Session;
    } catch {
        return null;
    }
}

export function getCurrentUser(): User | null {
    return getSession()?.user ?? null;
}

export function isAdmin(): boolean {
    return getCurrentUser()?.role === 'ADMIN';
}

export function saveSession(session: Session): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function clearSession(): void {
    localStorage.removeItem(STORAGE_KEY);
}
