import type { UserRole } from '../types/models';

export interface CurrentUser {
    userId: string;
    name: string;
    role: UserRole;
}

const STORAGE_KEY = 'currentUser';

export function getCurrentUser(): CurrentUser | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
        return JSON.parse(raw) as CurrentUser;
    } catch {
        return null;
    }
}

export function setCurrentUser(user: CurrentUser): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
}

export function signOut(): void {
    localStorage.removeItem(STORAGE_KEY);
}
