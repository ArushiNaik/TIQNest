import { useCallback, useEffect, useState } from 'react';
import { Alert, Badge, Button, Card, Col, Container, Row, Table } from 'react-bootstrap';
import {
    getAllIncidents,
    getQueue,
    getSlaAlerts,
    getSlaSummary,
    notifyOpsChanged,
    OPS_REFRESH_EVENT,
    resolveIncident,
} from '../../services/ops-service';
import type { Incident, SlaSummary, Ticket } from '../../types/models';
import { formatLabel } from '../tiqradar/TicketCard';
import DemoOutageButton from './DemoOutageButton';
import AdminReviewPanel from '../tiqvision/AdminReviewPanel';
import SlaBadge from './SlaBadge';
import TicketDetailsModal from './TicketDetailsModal';
import './dashboard.css';

const POLL_MS = 15000;

const tiles: { key: keyof SlaSummary; label: string; variant: string }[] = [
    { key: 'onTrack', label: 'On track', variant: 'success' },
    { key: 'atRisk', label: 'At risk', variant: 'warning' },
    { key: 'breached', label: 'Breached', variant: 'danger' },
    { key: 'met', label: 'Met', variant: 'secondary' },
];

function TicketTable({ tickets, empty, onOpen }: { tickets: Ticket[]; empty: string; onOpen: (id: number) => void }) {
    if (tickets.length === 0) return <p className="text-muted small mb-0">{empty}</p>;
    return (
        <Table size="sm" className="mb-0 align-middle">
            <thead>
                <tr>
                    <th>#</th>
                    <th>Ticket</th>
                    <th>Priority</th>
                    <th>SLA</th>
                    <th>Assignee</th>
                </tr>
            </thead>
            <tbody>
                {tickets.map((t) => (
                    <tr key={t.id} className="ops-row" onClick={() => onOpen(t.id)}>
                        <td>{t.id}</td>
                        <td>
                            <div>{t.title}</div>
                            <small className="text-muted">{t.affectedSystem}</small>
                        </td>
                        <td><Badge bg="dark">{formatLabel(t.priority)}</Badge></td>
                        <td><SlaBadge ticket={t} /></td>
                        <td>{t.assignee ?? <span className="text-muted">Unassigned</span>}</td>
                    </tr>
                ))}
            </tbody>
        </Table>
    );
}

export default function DashboardPage() {
    const [summary, setSummary] = useState<SlaSummary | null>(null);
    const [incidents, setIncidents] = useState<Incident[]>([]);
    const [alerts, setAlerts] = useState<Ticket[]>([]);
    const [queue, setQueue] = useState<Ticket[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [openTicketId, setOpenTicketId] = useState<number | null>(null);
    const [busyIncident, setBusyIncident] = useState<number | null>(null);
    const [updatedAt, setUpdatedAt] = useState<Date | null>(null);

    const load = useCallback(async () => {
        try {
            const [s, i, a, q] = await Promise.all([getSlaSummary(), getAllIncidents(), getSlaAlerts(), getQueue()]);
            setSummary(s);
            setIncidents([...i].sort((x, y) => (x.status === y.status ? y.id - x.id : x.status === 'ACTIVE' ? -1 : 1)));
            setAlerts(a);
            setQueue(q);
            setError(null);
            setUpdatedAt(new Date());
        } catch (err) {
            setError((err as Error).message);
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
        setBusyIncident(id);
        try {
            await resolveIncident(id);
            notifyOpsChanged();
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setBusyIncident(null);
        }
    };

    return (
        <Container className="mt-4 mb-5">
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="mb-0">Dashboard</h2>
                <div className="d-flex align-items-center gap-2">
                    <DemoOutageButton />
                    {updatedAt && <small className="text-muted">Updated {updatedAt.toLocaleTimeString()}</small>}
                    <Button size="sm" variant="outline-primary" onClick={load}>Refresh</Button>
                </div>
            </div>

            {error && <Alert variant="danger">{error}</Alert>}

            <Row className="g-3 mb-4">
                {tiles.map((tile) => (
                    <Col xs={6} md={3} key={tile.key}>
                        <Card className={`sla-tile border-top border-4 border-${tile.variant}`}>
                            <Card.Body>
                                <div className={`display-6 text-${tile.variant}`}>{summary ? summary[tile.key] : '–'}</div>
                                <div className="text-muted">{tile.label}</div>
                            </Card.Body>
                        </Card>
                    </Col>
                ))}
            </Row>

            <Row className="g-3">
                <Col lg={5}>
                    <div className="ops-panel">
                        <h5>📡 Incidents</h5>
                        {incidents.length === 0 ? (
                            <p className="text-muted small mb-0">No incidents detected.</p>
                        ) : (
                            incidents.map((incident) => (
                                <div key={incident.id} className="d-flex justify-content-between align-items-center border-bottom py-2">
                                    <div>
                                        <div className="fw-semibold">
                                            #{incident.id} {incident.title}{' '}
                                            <Badge bg={incident.status === 'ACTIVE' ? 'danger' : 'success'}>
                                                {formatLabel(incident.status)}
                                            </Badge>
                                        </div>
                                        <small className="text-muted">
                                            {incident.ticketCount} reports · detected {new Date(incident.detectedAt).toLocaleTimeString()}
                                        </small>
                                    </div>
                                    {incident.status === 'ACTIVE' && (
                                        <Button
                                            size="sm"
                                            variant="success"
                                            disabled={busyIncident === incident.id}
                                            onClick={() => handleResolve(incident.id)}
                                        >
                                            Resolve
                                        </Button>
                                    )}
                                </div>
                            ))
                        )}
                    </div>
                </Col>
                <Col lg={7}>
                    <div className="ops-panel">
                        <h5>🚨 Needs attention <small className="text-muted fs-6">(SLA at risk or breached)</small></h5>
                        <TicketTable tickets={alerts} empty="Nothing at risk. 🎉" onOpen={setOpenTicketId} />
                    </div>
                </Col>
                <Col xs={12}>
                    <div className="ops-panel">
                        <h5>📥 Agent queue <small className="text-muted fs-6">(open tickets not part of an outage)</small></h5>
                        <TicketTable tickets={queue} empty="The queue is empty." onOpen={setOpenTicketId} />
                    </div>
                </Col>
            </Row>

            <AdminReviewPanel />

            <TicketDetailsModal ticketId={openTicketId} onClose={() => setOpenTicketId(null)} />
        </Container>
    );
}
