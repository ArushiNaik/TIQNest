import type { TicketCategory } from '../../types/models';
import type { IssueType, RemediationOutcome, RemedyActionType, SafetyLevel } from '../../types/remediation';

export interface IssueOption {
    type: IssueType;
    label: string;
    icon: string;
    hint: string;
    category: TicketCategory;
    system: string;
}

export const ISSUE_OPTIONS: IssueOption[] = [
    { type: 'VPN', label: 'VPN Problems', icon: '🔐', hint: "Can't connect or keeps dropping", category: 'VPN', system: 'VPN' },
    { type: 'PRINTER', label: 'Printer Problems', icon: '🖨️', hint: 'Offline, stuck queue, won’t print', category: 'PRINTER', system: 'Printer' },
    { type: 'DNS', label: 'DNS / Network Problems', icon: '🌐', hint: 'Websites won’t load or resolve', category: 'NETWORK', system: 'Network' },
    { type: 'BROWSER_CACHE', label: 'Browser Cache Problems', icon: '🧭', hint: 'Old content, page looks broken', category: 'SOFTWARE', system: 'Web browser' },
    { type: 'OTHER', label: 'Other', icon: '💬', hint: 'Something else', category: 'OTHER', system: '' },
];

export function issueOption(type: IssueType): IssueOption {
    return ISSUE_OPTIONS.find((o) => o.type === type) ?? ISSUE_OPTIONS[ISSUE_OPTIONS.length - 1];
}

export const REMEDY_ICON: Record<IssueType, string> = {
    VPN: '🔐',
    PRINTER: '🖨️',
    DNS: '🌐',
    BROWSER_CACHE: '🧭',
    OTHER: '🧑‍💻',
};

export const ACTION_TYPE_LABEL: Record<RemedyActionType, string> = {
    SETTINGS_SHORTCUT: 'Settings shortcut',
    GUIDED_TROUBLESHOOTING: 'Guided troubleshooting',
    DIAGNOSTIC_COMMAND: 'Diagnostic command',
    REPAIR_COMMAND: 'Repair command',
    ESCALATION: 'IT escalation',
};

export const SAFETY: Record<SafetyLevel, { label: string; variant: string; detail: string }> = {
    SAFE: {
        label: 'Safe',
        variant: 'success',
        detail: 'Read-only or navigation only. Nothing on your computer changes unless you change it.',
    },
    LOW_RISK: {
        label: 'Low risk',
        variant: 'warning',
        detail: 'A small, reversible change that you perform yourself after reading what it does.',
    },
    IT_ONLY: {
        label: 'IT only',
        variant: 'secondary',
        detail: 'No self-service action is offered. A technician will handle this.',
    },
};

export const OUTCOME: Record<RemediationOutcome, { label: string; variant: string; icon: string }> = {
    SUCCESS: { label: 'Reported solved', variant: 'success', icon: '✅' },
    FAILED: { label: 'Still having issues', variant: 'danger', icon: '⚠️' },
    UNKNOWN: { label: 'Not sure yet', variant: 'secondary', icon: '❔' },
};

export function confidenceLabel(confidence?: number): string | null {
    if (confidence == null) return null;
    if (confidence >= 60) return 'Strong match';
    if (confidence >= 30) return 'Good match';
    return 'Possible match';
}

const SETTINGS_URI = /^ms-settings:[a-z0-9-]+$/;

export function isAllowedSettingsUri(uri: string | undefined): uri is string {
    return !!uri && SETTINGS_URI.test(uri);
}

export function isWindows(): boolean {
    return /Windows/i.test(navigator.userAgent);
}

export async function copyToClipboard(text: string): Promise<boolean> {
    try {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
            return true;
        }
    } catch {
    }
    try {
        const area = document.createElement('textarea');
        area.value = text;
        area.setAttribute('readonly', '');
        area.style.position = 'fixed';
        area.style.opacity = '0';
        document.body.appendChild(area);
        area.select();
        const ok = document.execCommand('copy');
        document.body.removeChild(area);
        return ok;
    } catch {
        return false;
    }
}
