import { Link } from 'react-router-dom';
import PublicLayout, { PageHeader, useAppEntry } from '../PublicLayout';
import { AUDIENCES, STEPS } from '../content';

export default function HowItWorksPage() {
    const { destination, cta } = useAppEntry();
    return (
        <PublicLayout>
            <PageHeader title="HOW IT WORKS" intro="An example: the office Wi-Fi starts dropping on a busy morning." />
            <section className="lp-wrap lp-detail-list">
                {STEPS.map((step, i) => (
                    <article className="lp-detail" key={step.title}>
                        <div className="lp-detail-head">
                            <span className="lp-step-number">0{i + 1}</span>
                            <h2>{step.title}</h2>
                        </div>
                        <div>
                            <p className="lp-detail-summary">{step.long}</p>
                            <p className="lp-feature-tag">Behind the scenes</p>
                            <ul>{step.details.map((d) => <li key={d}>{d}</li>)}</ul>
                        </div>
                    </article>
                ))}
            </section>
        </PublicLayout>
    );
}
