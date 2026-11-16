import { useEffect, useState, type ReactNode } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { Logo } from '../../brand';
import { getCurrentUser, isAdmin } from '../../services/auth';
import './LandingPage.css';

export const GITHUB_URL = 'https://github.com/ArushiNaik/TIQNest';

const NAV = [
    { to: '/features', label: 'Features' },
    { to: '/how-it-works', label: 'How it works' },
    { to: '/about', label: 'About' },
];

export function useAppEntry() {
    const signedIn = !!getCurrentUser();
    return {
        signedIn,
        destination: signedIn ? (isAdmin() ? '/dashboard' : '/home') : '/login',
        cta: signedIn ? 'Open the app' : 'Try the demo',
    };
}

const REVEAL_SELECTOR = '.lp-section-heading, .lp-page-head, .lp-feature, .lp-flow, .lp-audience-grid article, .lp-detail, .lp-demo-logins';

function useScrollReveal() {
    const { pathname } = useLocation();
    useEffect(() => {
        const items = Array.from(document.querySelectorAll<HTMLElement>(`.landing :is(${REVEAL_SELECTOR})`));
        items.forEach((el) => {
            const siblings = el.parentElement ? Array.from(el.parentElement.children) : [];
            el.style.setProperty('--reveal-delay', `${Math.min(siblings.indexOf(el), 4) * 90}ms`);
            el.classList.add('lp-reveal');
        });
        if (!('IntersectionObserver' in window)) {
            items.forEach((el) => el.classList.add('is-visible'));
            return;
        }
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('is-visible');
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' });
        items.forEach((el) => observer.observe(el));
        return () => observer.disconnect();
    }, [pathname]);
}

export default function PublicLayout({ children }: { children: ReactNode }) {
    const [menuOpen, setMenuOpen] = useState(false);
    const { signedIn, destination, cta } = useAppEntry();
    const close = () => setMenuOpen(false);
    useScrollReveal();

    return (
        <div className="landing" id="top">
            <a className="lp-skip" href="#main-content">Skip to content</a>
            <header className="lp-header">
                <Link to="/" className="lp-brand" aria-label="TIQNest home" onClick={close}><Logo /></Link>
                <button className="lp-menu" type="button" aria-expanded={menuOpen} aria-controls="public-nav" onClick={() => setMenuOpen(!menuOpen)}>
                    {menuOpen ? 'Close' : 'Menu'}<span className="visually-hidden"> navigation</span>
                </button>
                <nav id="public-nav" aria-label="Main" className={menuOpen ? 'lp-nav is-open' : 'lp-nav'} onKeyDown={(e) => { if (e.key === 'Escape') close(); }}>
                    {NAV.map((item) => <NavLink key={item.to} to={item.to} onClick={close}>{item.label}</NavLink>)}
                    {!signedIn && <Link to="/login" onClick={close}>Sign in</Link>}
                    <Link className="lp-button lp-small" to={destination} onClick={close}>{cta} <span aria-hidden="true">↗</span></Link>
                </nav>
            </header>
            <main id="main-content">{children}</main>
            <footer className="lp-wrap lp-footer">
                <div>
                    <Link to="/" className="lp-brand lp-brand-small"><Logo /></Link>
                    <p>A help desk that turns duplicate reports into one incident.</p>
                </div>
                <nav aria-label="Footer">
                    {NAV.map((item) => <Link key={item.to} to={item.to}>{item.label}</Link>)}
                    <a href={GITHUB_URL} target="_blank" rel="noreferrer">GitHub</a>
                </nav>
            </footer>
        </div>
    );
}

export function PageHeader({ title, intro }: { title: string; intro: string }) {
    return (
        <section className="lp-wrap lp-page-head">
            <h1>{title}</h1>
            <p className="lp-intro">{intro}</p>
        </section>
    );
}
