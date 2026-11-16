import { FeatureIcon } from '../../../brand';
import PublicLayout, { PageHeader } from '../PublicLayout';
import { FEATURES } from '../content';

export default function FeaturesPage() {
    return (
        <PublicLayout>
            <PageHeader title="FEATURES" intro="Three tools for the people who report problems and the people who fix them, plus the tickets and dashboard underneath." />
            <section className="lp-wrap lp-detail-list">
                {FEATURES.map((f) => (
                    <article className="lp-detail" key={f.key} id={f.key}>
                        <div className="lp-detail-head">
                            <span className="lp-icon"><FeatureIcon feature={f.key} /></span>
                            <div>
                                <p className="lp-feature-tag">{f.tag}</p>
                                <h2>{f.name}</h2>
                            </div>
                        </div>
                        <div>
                            <p className="lp-detail-summary">{f.summary}</p>
                            <ul>{f.details.map((d) => <li key={d}>{d}</li>)}</ul>
                        </div>
                    </article>
                ))}
            </section>
        </PublicLayout>
    );
}
