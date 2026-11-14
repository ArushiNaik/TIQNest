export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

export const API_URL_TICKETS = `${API_BASE_URL}/tickets`;
export const API_URL_USERS = `${API_BASE_URL}/users`;
export const API_URL_LOGIN = `${API_BASE_URL}/auth/login`;
export const API_URL_LOGOUT = `${API_BASE_URL}/auth/logout`;
