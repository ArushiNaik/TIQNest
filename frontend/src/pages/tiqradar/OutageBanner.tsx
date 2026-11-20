import { useCallback, useEffect, useState } from 'react';
import { Alert, Button, Container } from 'react-bootstrap';
import { getCurrentUser, isAdmin } from '../../services/auth';
import { getActiveIncidents, notifyOpsChanged, OPS_REFRESH_EVENT, resolveIncident } from '../../services/ops-service';
import type { Incident } from '../../types/models';

const POLL_MS = 10000;

export default function OutageBanner() {
    const [incidents, setIncidents] = useState<Incident[]>([]);
    const [busyId, setBusyId] = useState<number | null>(null);
    const admin = isAdmin();

    const load = useCallback(async () => {
        if (!getCurrentUser()) return;
        try {
            setIncidents(await getActiveIncidents());
        } catch (err) {
            console.warn('Could not load active incidents:', err);
        }
    }, []);

    useEffect(() => {
        load();
        const timer = setInterval(load, POLL_MS);
        window.addEventListener(OPS_REFRESH_EVENT, load);
        return () => {
            clearInterval(timer);
            window.removeEventListener(OPS_REFRESH_EVENT, load);
        };
    }, [load]);

    const handleResolve = async (id: number) => {
        setBusyId(id);
        try {
            await resolveIncident(id);
            notifyOpsChanged();
        } catch (err) {
            console.error('Could not resolve incident:', err);
        } finally {
            setBusyId(null);
            load();
        }
    };

    if (incidents.length === 0) return null;

    return (
        <Container className="mt-3">
            {incidents.map((incident) => (
                <Alert key={incident.id} variant="warning" className="d-flex justify-content-between align-items-center mb-2 shadow-sm">
                    <div>
                        <div className="fw-bold">{incident.bannerMessage}</div>
                        <small className="text-muted">
                            Incident #{incident.id} · detected {new Date(incident.detectedAt).toLocaleTimeString()}
                            {incident.lastReportAt && <> · last report {new Date(incident.lastReportAt).toLocaleTimeString()}</>}
                        </small>
                    </div>
                    {admin && (
                        <Button
                            variant="success"
                            size="sm"
                            disabled={busyId === incident.id}
                            onClick={() => handleResolve(incident.id)}
                        >
                            {busyId === incident.id ? 'Resolving...' : 'Mark resolved'}
                        </Button>
                    )}
                </Alert>
            ))}
        </Container>
    );
}
