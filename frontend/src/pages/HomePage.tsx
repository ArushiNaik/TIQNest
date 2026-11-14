import { Link } from 'react-router-dom';
import { getCurrentUser, isAdmin } from '../services/auth';
import './HomePage.css';

export default function HomePage() {
    const currentUser = getCurrentUser();
    const admin = isAdmin();

    return (
        <div className="home">
            <h1 className="hero-title">TIQNest</h1>
            <p className="hero-subtitle">
                {currentUser ? `Welcome back, ${currentUser.name}. ` : ''}
                IT help without the IT department: report a problem, follow it live,
                and know about an outage before you even ask.
            </p>
            <div className="ctaContainer">
                {admin ? (
                    <Link to="/dashboard" className="mainButton">Open dashboard</Link>
                ) : (
                    <Link to="/tickets" className="mainButton">Report a problem</Link>
                )}
                <Link to={admin ? '/tickets' : '/fix'} className="mainButton secondaryButton">
                    {admin ? 'All tickets' : 'Try TiqFix'}
                </Link>
            </div>
        </div>
    );
}
