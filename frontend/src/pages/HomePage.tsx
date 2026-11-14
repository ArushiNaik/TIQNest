import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCurrentUser } from '../services/auth';

export default function HomePage() {
    const navigate = useNavigate();
    const currentUser = getCurrentUser();

    useEffect(() => {
        if (!currentUser) {
            navigate('/login');
            return;
        }
        const timer = setTimeout(() => navigate('/tickets'), 2000);
        return () => clearTimeout(timer);
    }, [currentUser, navigate]);

    return (
        <div className="center">
            {currentUser ? (
                <div>
                    <h1>Welcome back, {currentUser.name}!</h1>
                    <p>Taking you to your tickets in 2 seconds...</p>
                </div>
            ) : (
                <p>Loading...</p>
            )}
        </div>
    );
}
