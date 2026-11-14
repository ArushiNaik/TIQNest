import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { postData } from '../services/api-services';
import { setCurrentUser } from '../services/auth';
import { API_URL_LOGIN } from '../services/config';
import type { LoginResponse } from '../types/models';
import './LoginPage.css';

export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();

        if (!email || !password) {
            setError('Please fill in both fields');
            return;
        }

        try {
            const data = await postData<LoginResponse>(API_URL_LOGIN, { email, password });
            if (data && data.userId) {
                setCurrentUser({ userId: data.userId, name: data.name, role: data.role });
                navigate('/home');
            } else {
                setError(data?.message || 'Invalid email or password');
            }
        } catch (err) {
            console.error('Login error:', err);
            setError('Could not sign in. Check your email and password and try again.');
        }
    };

    return (
        <div className="login-page">
            <div className="login-form">
                <h2>🛠️ TIQNest</h2>
                <p className="login-subtitle">Sign in to report a problem or check your tickets</p>
                {error && <div className="error-message">{error}</div>}
                <form onSubmit={handleSubmit}>
                    <div>
                        <label htmlFor="email">Work email</label>
                        <input
                            type="email"
                            id="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>
                    <div>
                        <label htmlFor="password">Password</label>
                        <input
                            type="password"
                            id="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>
                    <button type="submit" disabled={!email || !password}>Sign In</button>
                </form>
            </div>
        </div>
    );
}
