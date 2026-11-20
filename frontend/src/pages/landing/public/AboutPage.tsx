import { Link } from 'react-router-dom';
import PublicLayout, { GITHUB_URL, PageHeader, useAppEntry } from '../PublicLayout';

const STACK = [
    ['Backend', 'Java 21, Spring Boot, Spring Data JPA, MapStruct'],
    ['Frontend', 'React, TypeScript, Vite, React Bootstrap'],
    ['Database', 'PostgreSQL'],
    ['Hosting', 'Netlify for the frontend, a hosted Spring Boot backend'],
];

export default function AboutPage() {
    const { destination, cta } = useAppEntry();
    return (
        <PublicLayout>
            <PageHeader title="ABOUT" intro="TIQNest was built at HackDécouverte Concordia by Ilian Adeleke, Arushi Naik, Malick Konate and Jacky Bolap." />
            <section className="lp-wrap lp-about">
                <div className="lp-demo-logins">
                    <h3>Built with</h3>
                    <dl>{STACK.map(([k, v]) => <div key={k}><dt>{k}</dt><dd>{v}</dd></div>)}</dl>
                    <a className="lp-text-link" href={GITHUB_URL} target="_blank" rel="noreferrer">Source code on GitHub <span aria-hidden="true">↗</span></a>
                </div>
                <div className="lp-demo-logins">
                    <h3>Demo logins</h3>
                    <dl>
                        <div><dt>Admin</dt><dd className="lp-mono">Christine@demo.com</dd></div>
                        <div><dt>Employee</dt><dd className="lp-mono">Amin@demo.com</dd></div>
                        <div><dt>Password</dt><dd className="lp-mono">pwd</dd></div>
                    </dl>
                    <Link className="lp-button" to={destination}>{cta} <span aria-hidden="true">↗</span></Link>
                </div>
            </section>
        </PublicLayout>
    );
}
