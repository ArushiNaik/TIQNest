import type { FeatureKey } from '../../brand';

export interface FeatureInfo {
    key: FeatureKey;
    name: string;
    tag: string;
    summary: string;
    details: string[];
    tone?: 'pink' | 'dark';
    secondary?: boolean;
}

export const FEATURES: FeatureInfo[] = [
    {
        key: 'radar',
        name: 'TiqRadar',
        tag: 'Outage detection',
        summary: 'Duplicate reports about the same system become one incident, and everyone sees that it is being handled.',
        details: [
            'Three reports with the same category and affected system within five minutes open one incident.',
            'Later reports join the open incident instead of becoming new tickets.',
            'An outage banner tells every employee the problem is known.',
            'The same person reporting the same problem twice within five minutes is told it is already logged.',
            'Resolving the incident resolves every ticket linked to it.',
        ],
    },
    {
        key: 'vision',
        name: 'TiqVision',
        tag: 'Screenshot triage · prototype',
        summary: 'Show the error instead of describing it. TiqVision drafts the ticket for you to review.',
        details: [
            'Upload a screenshot or a 5-second screen recording of the problem.',
            'AI analysis suggests a category, likely fixes and a drafted ticket. You review everything before it is sent.',
            'Requests that are not IT problems are flagged instead of becoming tickets, and you can ask a person to review them.',
            'AI analysis is turned off on the public demo.',
        ],
        tone: 'pink',
    },
    {
        key: 'fix',
        name: 'TiqFix',
        tag: 'IT-reviewed fixes',
        summary: 'Step-by-step fixes for common problems, limited to ones IT has reviewed.',
        details: [
            'Covers VPN, printer, DNS and browser cache problems.',
            'Uses Windows Settings shortcuts and copy-and-paste commands. Nothing runs on your computer without you.',
            'Anything that sounds like a security problem, such as phishing or a virus, goes straight to IT.',
            'Problems it does not recognise go to IT instead of getting a guessed fix.',
            'Records whether each fix worked, so IT can see which ones help.',
        ],
        tone: 'dark',
    },
    {
        key: 'tickets',
        name: 'Tickets',
        tag: 'Deadlines and history',
        summary: 'Every ticket has a deadline based on its priority and a full history.',
        details: [
            'Deadlines: 1 hour for critical, 4 for high, 24 for medium and 72 for low.',
            'A countdown shows when a ticket is at risk of missing its deadline.',
            'Every status change is recorded with who made it.',
        ],
        secondary: true,
    },
    {
        key: 'dashboard',
        name: 'Admin dashboard',
        tag: 'For the IT team',
        summary: 'Active incidents, tickets close to their deadline and deadline totals on one screen.',
        details: [
            'Lists active incidents with how many reports each has.',
            'Highlights tickets that are at risk of missing, or have missed, their deadline.',
            'Shows how many tickets are on track, at risk, late or done on time.',
        ],
        secondary: true,
    },
];

export const STEPS = [
    {
        title: 'Report',
        short: 'Employees report a problem',
        long: 'Amin reports that the office Wi-Fi keeps dropping. TiqFix suggests one reviewed fix to try first. If it does not help, his ticket is already with IT.',
        details: [
            'Employees describe the problem with a category and the affected system, or upload a screenshot with TiqVision.',
            'If a reviewed fix matches, TiqFix offers it. Security problems and anything it does not recognise go to IT.',
            'Every ticket gets a deadline from its priority, from 1 hour for critical to 72 hours for low.',
        ],
    },
    {
        title: 'Group',
        short: 'Matching reports become one incident',
        long: 'Two coworkers report the same problem within five minutes. TiqRadar opens one incident and shows a banner, so nobody else has to ask whether IT knows.',
        details: [
            'TiqRadar compares new tickets by category and affected system.',
            'Three matching reports within five minutes open an incident. Later reports join it instead of piling up.',
            'An outage banner tells everyone the problem is known, and a repeat report from the same person is caught.',
        ],
    },
    {
        title: 'Prioritize',
        short: 'IT sees one incident, not a pile of tickets',
        long: 'Christine, the admin, sees one incident instead of three tickets. Its deadline comes from the priority, and the dashboard shows how much time is left.',
        details: [
            'The dashboard lists active incidents with how many reports each has.',
            'Tickets at risk of missing their deadline, or already late, are highlighted.',
        ],
    },
    {
        title: 'Resolve',
        short: 'Closing the incident closes every linked ticket',
        long: 'She resolves the incident and all three tickets are resolved with it. Each change is in the ticket history.',
        details: [
            'Resolving the incident resolves every ticket linked to it.',
            'Each status change is recorded with who made it.',
            'Whether a TiqFix fix worked is recorded too, so IT can see which fixes help.',
        ],
    },
];

export const AUDIENCES = [
    {
        title: 'For employees',
        text: 'Report a problem once and see what happens to it.',
        items: ['Report a problem or upload a screenshot', 'Try a fix IT has reviewed', 'See an outage banner instead of wondering', 'Follow each ticket\'s status'],
    },
    {
        title: 'For IT staff',
        text: 'Whether you are the only IT person or one of fifty, spend your time fixing problems, not sorting duplicate tickets.',
        items: ['Duplicate reports grouped into one incident', 'Deadlines by priority, with a countdown', 'One dashboard for incidents and late tickets', 'A full history on every ticket'],
    },
];
