import './brand.css';

export const BRAND_NAME = 'TIQNest';

export const FEATURE_ICONS = {
    radar: 'bi-broadcast',
    vision: 'bi-eye',
    fix: 'bi-tools',
    tickets: 'bi-ticket-perforated',
    dashboard: 'bi-speedometer2',
} as const;

export type FeatureKey = keyof typeof FEATURE_ICONS;

export function Icon({ name, className = '' }: { name: string; className?: string }) {
    return <i className={`bi ${name} ${className}`.trim()} aria-hidden="true" />;
}

export function FeatureIcon({ feature, className }: { feature: FeatureKey; className?: string }) {
    return <Icon name={FEATURE_ICONS[feature]} className={className} />;
}

export function Logo({ className = '' }: { className?: string }) {
    return (
        <span className={`brand-logo ${className}`.trim()}>
            {BRAND_NAME}
            <Icon name="bi-asterisk" className="brand-mark" />
        </span>
    );
}
