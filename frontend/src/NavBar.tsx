import { Button, Nav, Navbar } from 'react-bootstrap';
import Container from 'react-bootstrap/Container';
import { Link, useNavigate } from 'react-router-dom';
import { postData } from './services/api-services';
import { clearSession, getCurrentUser, isAdmin } from './services/auth';
import { API_URL_LOGOUT } from './services/config';

interface NavLinkItem {
    to: string;
    title: string;
    adminOnly: boolean;
}

const links: NavLinkItem[] = [
    { to: 'tickets', title: 'Tickets', adminOnly: false },
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
        <Navbar bg="primary" data-bs-theme="dark">
            <Container>
                <Navbar.Brand as={Link} to="/tickets" className="fs-3 fw-bold">
                    🛠️ TIQNest
                </Navbar.Brand>
                <Nav className="me-auto fs-5">
                    {visibleLinks.map((link) => (
                        <Nav.Link key={link.to} as={Link} to={`/${link.to}`}>
                            {link.title}
                        </Nav.Link>
                    ))}
                </Nav>
                {currentUser && (
                    <>
                        <Navbar.Text className="me-3">
                            {currentUser.name} ({isAdmin() ? 'Admin' : 'Employee'})
                        </Navbar.Text>
                        <Button variant="outline-light" size="sm" onClick={handleSignOut}>
                            Sign Out
                        </Button>
                    </>
                )}
            </Container>
        </Navbar>
    );
}
