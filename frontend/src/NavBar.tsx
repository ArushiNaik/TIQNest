import { Link, NavLink, useNavigate } from 'react-router-dom';
import { postData } from './services/api-services';
import { clearSession, getCurrentUser, isAdmin } from './services/auth';
import { API_URL_LOGOUT } from './services/config';
import './NavBar.css';

interface NavLinkItem {
    to: string;
    title: string;
    adminOnly: boolean;
}

const links: NavLinkItem[] = [
    { to: 'home', title: 'Home', adminOnly: false },
    { to: 'dashboard', title: 'Dashboard', adminOnly: true },
    { to: 'tickets', title: 'Tickets', adminOnly: false },
    { to: 'vision', title: 'TiqVision', adminOnly: false },
    { to: 'fix', title: 'TiqFix', adminOnly: false },
    { to: 'users', title: 'Users', adminOnly: true },
];

export default function HelpDeskNavBar() {
    const navigate = useNavigate();
    const currentUser = getCurrentUser();
    const visibleLinks = links.filter((link) => !link.adminOnly || isAdmin());

    const handleSignOut = async () => {
        try {
            await postData(API_URL_LOGOUT);
        } catch {
        }
        clearSession();
        navigate('/login');
    };

    return (
        <nav className="navbar">
            <Link to="/home" className="navBrand">TIQNest</Link>
            <div className="navLinks">
                {visibleLinks.map((link) => (
                    <NavLink key={link.to} to={`/${link.to}`}>
                        {link.title}
                    </NavLink>
                ))}
            </div>
            {currentUser && (
                <div className="navUser">
                    <span>{currentUser.name} · {isAdmin() ? 'Admin' : 'Employee'}</span>
                    <button type="button" className="navSignOut" onClick={handleSignOut}>
                        Sign out
                    </button>
                </div>
            )}
        </nav>
    );
}
