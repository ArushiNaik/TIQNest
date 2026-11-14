import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import type { ReactElement } from 'react';
import HelpDeskNavBar from './NavBar';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import TicketPage from './pages/TicketPage';
import UserPage from './pages/UserPage';
import TiqFixPage from './pages/tiqfix/TiqFixPage';
import { getCurrentUser, isAdmin } from './services/auth';

function RequireLogin({ children }: { children: ReactElement }) {
    return getCurrentUser() ? children : <Navigate to="/login" replace />;
}

function RequireAdmin({ children }: { children: ReactElement }) {
    if (!getCurrentUser()) return <Navigate to="/login" replace />;
    return isAdmin() ? children : <Navigate to="/tickets" replace />;
}

function AppContent() {
    const location = useLocation();
    const isLoginPage = location.pathname === '/login';

    return (
        <div className="App">
            {!isLoginPage && <HelpDeskNavBar />}

            <Routes>
                <Route path="/" element={<Navigate to="/home" />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/home" element={<RequireLogin><HomePage /></RequireLogin>} />
                <Route path="/tickets" element={<RequireLogin><TicketPage /></RequireLogin>} />
                <Route path="/fix" element={<RequireLogin><TiqFixPage /></RequireLogin>} />
                <Route path="/users" element={<RequireAdmin><UserPage /></RequireAdmin>} />
            </Routes>
        </div>
    );
}

export default function App() {
    return (
        <Router>
            <AppContent />
        </Router>
    );
}
