import { useEffect, useState } from 'react';
import { Button } from 'react-bootstrap';
import { copyToClipboard } from './tiqfix-ui';

interface CopyButtonProps {
    text: string;
    label: string;
    variant?: string;
    size?: 'sm' | 'lg';
    disabled?: boolean;
    onCopied?: () => void;
}

/** Copies an approved value and announces the result to screen readers. */
export default function CopyButton({ text, label, variant = 'outline-primary', size, disabled, onCopied }: CopyButtonProps) {
    const [state, setState] = useState<'idle' | 'copied' | 'failed'>('idle');

    useEffect(() => {
        if (state === 'idle') return;
        const timer = setTimeout(() => setState('idle'), 2500);
        return () => clearTimeout(timer);
    }, [state]);

    const handleClick = async () => {
        const ok = await copyToClipboard(text);
        setState(ok ? 'copied' : 'failed');
        if (ok) onCopied?.();
    };

    return (
        <>
            <Button variant={state === 'copied' ? 'success' : variant} size={size} disabled={disabled} onClick={handleClick}>
                {state === 'copied' ? '✓ Copied' : state === 'failed' ? 'Copy failed – select the text manually' : label}
            </Button>
            <span className="visually-hidden" role="status" aria-live="polite">
                {state === 'copied' ? 'Copied to clipboard' : state === 'failed' ? 'Copy failed' : ''}
            </span>
        </>
    );
}
