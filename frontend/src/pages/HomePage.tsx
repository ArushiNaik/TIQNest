import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCurrentUser } from '../services/auth';

export default function HomePage() {
    const navigate = useNavigate();
    const currentUser = getCurrentUser();

    useEffect(() => {
        if (!currentUser) {
            navigate('/login');
        }
    }, [currentUser, navigate]);

    return (
        <div className="center">
            {currentUser ? (
                <div>
                    <h1>Welcome back, {currentUser.name}!</h1>
                </div>
            ) : (
                <p>Loading...</p>
            )}
        </div>
    );
}
