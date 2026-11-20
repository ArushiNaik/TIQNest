import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { FeatureIcon } from '../../brand';
import PublicLayout, { useAppEntry } from './PublicLayout';
import { AUDIENCES, FEATURES, STEPS } from './content';

const previewStages = [
    { pill: 'Open', meta: '1 report', progress: 25, radar: false, fix: true },
    { pill: 'Grouped', meta: '3 reports in 4 minutes', progress: 60, radar: true, fix: false },
    { pill: 'Resolved', meta: '3 tickets resolved together', progress: 100, radar: true, fix: false },
];

function usePreviewStage() {
    const [stage, setStage] = useState(0);
    useEffect(() => {
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            setStage(1);
            return;
        }
        const timer = window.setInterval(() => setStage((s) => (s + 1) % previewStages.length), 2800);
        return () => window.clearInterval(timer);
    }, []);
    return previewStages[stage];
}

function MoreLink({ to, children }: { to: string; children: string }) {
    return <Link className="lp-text-link lp-more" to={to}>{children} <span aria-hidden="true">→</span></Link>;
}

export default function LandingPage() {
    const { destination, cta } = useAppEntry();
    const preview = usePreviewStage();
    const products = FEATURES.filter((f) => !f.secondary);

    return (
        <PublicLayout>
            <section className="lp-hero lp-wrap" aria-labelledby="hero-heading">
                <div className="lp-hero-copy">
                    <h1 id="hero-heading">TWELVE REPORTS.<br /><span>ONE INCIDENT.</span></h1>
                    <p className="lp-intro">TIQNest is a help desk built to make IT staff's work easier. When twelve people report the same outage, IT gets one incident instead of twelve tickets, whether IT is one person at a small business or a full team at a large company.</p>
                    <div className="lp-actions">
                        <Link className="lp-button" to={destination}>{cta} <span aria-hidden="true">↗</span></Link>
                        <Link className="lp-text-link" to="/how-it-works">How it works <span aria-hidden="true">→</span></Link>
                    </div>
                </div>
                <figure className="lp-preview">
                    <div className="lp-orbit" aria-hidden="true" />
                    <div className="lp-preview-window">
                        <div className="lp-window-top"><span>EXAMPLE · OFFICE WI-FI</span><span aria-hidden="true">•••</span></div>
                        <div className="lp-preview-body" aria-live="polite">
                            <div className="lp-demo-ticket">
                                <div className="lp-ticket-meta"><span>TICKET · 042</span><span className={`lp-pill lp-pill-${preview.pill.toLowerCase()}`}>{preview.pill}</span></div>
                                <h3>Wi-Fi keeps dropping</h3>
                                <p>{preview.meta}</p>
                                <div className="lp-progress" aria-hidden="true"><span style={{ width: `${preview.progress}%` }} /></div>
                                <div className="lp-ticket-meta"><span>Reported</span><span>Grouped</span><span>Resolved</span></div>
                            </div>
                            <div className={`lp-demo-fix${preview.fix ? '' : ' is-hidden'}`}>
                                <span className="lp-icon"><FeatureIcon feature="fix" /></span>
                                <div><strong>TiqFix suggests a reviewed fix</strong><p>Clear the DNS cache, step by step.</p></div>
                            </div>
                        </div>
                    </div>
                    <div className={`lp-demo-radar${preview.radar ? '' : ' is-hidden'}`}>
                        <span className="lp-radar-mark"><FeatureIcon feature="radar" /></span>
                        <div><strong>TiqRadar opened an incident</strong><p>Everyone sees the outage banner.</p></div>
                    </div>
                    <figcaption>ILLUSTRATIVE PREVIEW · NO LIVE USER DATA</figcaption>
                </figure>
            </section>

            <section className="lp-feature-section" aria-labelledby="features-heading">
                <div className="lp-wrap">
                    <div className="lp-section-heading">
                        <h2 id="features-heading">FEATURES</h2>
                        <MoreLink to="/features">All features</MoreLink>
                    </div>
                    <div className="lp-feature-grid">
                        {products.map((f) => (
                            <article className={['lp-feature', f.tone && `lp-feature-${f.tone}`].filter(Boolean).join(' ')} key={f.key}>
                                <span className="lp-icon"><FeatureIcon feature={f.key} /></span>
                                <p className="lp-feature-tag">{f.tag}</p>
                                <h3>{f.name}</h3>
                                <p>{f.summary}</p>
                            </article>
                        ))}
                    </div>
                </div>
            </section>

            <section className="lp-wrap lp-how" aria-labelledby="how-heading">
                <div className="lp-section-heading">
                    <h2 id="how-heading">HOW IT WORKS</h2>
                    <MoreLink to="/how-it-works">How it works, step by step</MoreLink>
                </div>
                <ol className="lp-flow">
                    {STEPS.map((step, i) => (
                        <li key={step.title}><span className="lp-flow-number">0{i + 1}</span><strong>{step.title}</strong><span>{step.short}</span></li>
                    ))}
                </ol>
            </section>

            <section className="lp-wrap lp-audiences" aria-labelledby="audience-heading">
                <h2 id="audience-heading">WHO IT'S FOR</h2>
                <div className="lp-audience-grid">
                    {AUDIENCES.map((group) => (
                        <article key={group.title}>
                            <h3>{group.title}</h3>
                            <p>{group.text}</p>
                            <ul>{group.items.map((item) => <li key={item}><span aria-hidden="true">✓</span>{item}</li>)}</ul>
                        </article>
                    ))}
                </div>
            </section>
        </PublicLayout>
    );
}
