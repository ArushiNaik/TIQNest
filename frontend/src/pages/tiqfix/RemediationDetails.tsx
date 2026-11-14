import { useState, type ReactNode } from 'react';
import { Alert, Badge, Button, Form } from 'react-bootstrap';
import type { RemedyAction } from '../../types/remediation';
import CopyButton from './CopyButton';
import { ACTION_TYPE_LABEL, isAllowedSettingsUri, isWindows } from './tiqfix-ui';

interface RemediationDetailsProps {
    actions: RemedyAction[];
    /** Called whenever the user actually performs an action (opens settings, copies, views steps). */
    onActionTaken: (action: RemedyAction) => void;
    onViewInstructions: () => void;
    onEscalate: () => void;
}

/** Renders the approved actions of a remedy. Every button here does exactly what its label says. */
export default function RemediationDetails({ actions, onActionTaken, onViewInstructions, onEscalate }: RemediationDetailsProps) {
    return (
        <div className="d-flex flex-column gap-3">
            {actions.map((action, index) => {
                const primary = index === 0;
                switch (action.type) {
                    case 'SETTINGS_SHORTCUT':
                        return <SettingsAction key={action.id} action={action} primary={primary} onActionTaken={onActionTaken} />;
                    case 'DIAGNOSTIC_COMMAND':
                    case 'REPAIR_COMMAND':
                        return <CommandAction key={action.id} action={action} primary={primary} onActionTaken={onActionTaken} />;
                    case 'GUIDED_TROUBLESHOOTING':
                        return (
                            <ActionBlock key={action.id} action={action}>
                                <Button
                                    variant={primary ? 'primary' : 'outline-primary'}
                                    size={primary ? 'lg' : undefined}
                                    onClick={() => {
                                        onViewInstructions();
                                        onActionTaken(action);
                                    }}
                                >
                                    📋 {action.label}
                                </Button>
                            </ActionBlock>
                        );
                    case 'ESCALATION':
                        return (
                            <ActionBlock key={action.id} action={action}>
                                <Button variant="danger" size="lg" onClick={onEscalate}>
                                    🎧 {action.label}
                                </Button>
                            </ActionBlock>
                        );
                    default:
                        return null;
                }
            })}
        </div>
    );
}

function ActionBlock({ action, children }: { action: RemedyAction; children: ReactNode }) {
    return (
        <div className="tiqfix-action">
            <div className="d-flex align-items-center gap-2 mb-1">
                <Badge bg="light" text="dark" className="border">{ACTION_TYPE_LABEL[action.type]}</Badge>
            </div>
            <p className="text-muted small mb-2">{action.explanation}</p>
            {children}
        </div>
    );
}

function SettingsAction({ action, primary, onActionTaken }: { action: RemedyAction; primary: boolean; onActionTaken: (a: RemedyAction) => void }) {
    const [opened, setOpened] = useState(false);
    const uri = action.value;
    if (!isAllowedSettingsUri(uri)) return null; // never render a link that is not a Windows Settings page

    return (
        <ActionBlock action={action}>
            {!isWindows() && (
                <Alert variant="info" className="py-2 small">
                    This shortcut opens Windows Settings. You don’t seem to be on Windows, so it may not open here –
                    use it on your Windows work computer.
                </Alert>
            )}
            <div className="d-flex flex-wrap gap-2 align-items-center">
                <Button
                    href={uri}
                    variant={primary ? 'primary' : 'outline-primary'}
                    size={primary ? 'lg' : undefined}
                    onClick={() => {
                        setOpened(true);
                        onActionTaken(action);
                    }}
                >
                    ⚙️ {action.label}
                </Button>
                <CopyButton text={uri} label="Copy shortcut" variant="outline-secondary" size={primary ? 'lg' : undefined} />
            </div>
            {opened && (
                <Alert variant="light" className="border mt-3 mb-0 small" role="status">
                    <strong>Settings should be opening now.</strong> If your browser asks, choose <em>Open Settings</em>.
                    <br />
                    Nothing happened? Press <kbd>Win</kbd> + <kbd>R</kbd>, paste <code>{uri}</code> and press <kbd>Enter</kbd>.
                    <div className="text-muted mt-1">Opening Settings doesn’t fix anything by itself – follow the steps below.</div>
                </Alert>
            )}
        </ActionBlock>
    );
}

function CommandAction({ action, primary, onActionTaken }: { action: RemedyAction; primary: boolean; onActionTaken: (a: RemedyAction) => void }) {
    const isRepair = action.type === 'REPAIR_COMMAND';
    const [acknowledged, setAcknowledged] = useState(!isRepair);
    const [copied, setCopied] = useState(false);
    if (!action.value) return null;
    const checkboxId = `ack-${action.id}`;

    return (
        <ActionBlock action={action}>
            <div className="tiqfix-command" aria-label={`${action.shell ?? 'Command'} command`}>
                <div className="tiqfix-command-shell">{action.shell}</div>
                <code>{action.value}</code>
            </div>
            {action.effects && (
                <p className="small mt-2 mb-2">
                    <strong>{isRepair ? 'Potential effects:' : 'Effect:'}</strong> {action.effects}
                </p>
            )}
            {isRepair && (
                <Form.Check
                    id={checkboxId}
                    className="small mb-2"
                    checked={acknowledged}
                    onChange={(e) => setAcknowledged(e.target.checked)}
                    label="I understand TiqFix only copies this command. I’ll check it and run it myself in a terminal on my computer."
                />
            )}
            <CopyButton
                text={action.value}
                label={`📋 ${action.label}`}
                variant={primary ? 'primary' : 'outline-primary'}
                size={primary ? 'lg' : undefined}
                disabled={!acknowledged}
                onCopied={() => {
                    setCopied(true);
                    onActionTaken(action);
                }}
            />
            {copied && (
                <Alert variant="light" className="border mt-3 mb-0 small" role="status">
                    Copied. Now open <strong>{action.shell}</strong>, paste with <kbd>Ctrl</kbd> + <kbd>V</kbd>, check it
                    matches exactly, and press <kbd>Enter</kbd>. <strong>TiqFix has not run anything on your computer.</strong>
                </Alert>
            )}
        </ActionBlock>
    );
}
