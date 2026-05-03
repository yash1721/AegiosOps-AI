import React, { useEffect, useMemo, useState } from 'react';
import ReactDOM from 'react-dom/client';
import {
  analyzeIncident,
  approveIncident,
  createRunbook,
  fetchAuditLogs,
  fetchIncident,
  fetchIncidents,
  fetchRunbooks,
  resolveIncident,
} from './services/api';
import './styles.css';

const navItems = [
  { href: '/incidents', label: 'Incidents' },
  { href: '/runbooks', label: 'Runbooks' },
  { href: '/audit-logs', label: 'Audit Logs' },
];

function navigate(path) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

function useRoute() {
  const [path, setPath] = useState(window.location.pathname);

  useEffect(() => {
    const onPopState = () => setPath(window.location.pathname);
    window.addEventListener('popstate', onPopState);
    if (window.location.pathname === '/') {
      navigate('/incidents');
    }
    return () => window.removeEventListener('popstate', onPopState);
  }, []);

  return path;
}

function App() {
  const path = useRoute();
  const route = useMemo(() => {
    if (path.startsWith('/incidents/')) {
      return { page: 'incident-detail', id: path.split('/')[2] };
    }
    if (path === '/runbooks') return { page: 'runbooks' };
    if (path === '/audit-logs') return { page: 'audit-logs' };
    return { page: 'incidents' };
  }, [path]);

  return (
    <AppLayout currentPath={path}>
      {route.page === 'incident-detail' && <IncidentDetailPage incidentId={route.id} />}
      {route.page === 'runbooks' && <RunbooksPage />}
      {route.page === 'audit-logs' && <AuditLogsPage />}
      {route.page === 'incidents' && <IncidentsPage />}
    </AppLayout>
  );
}

function AppLayout({ children, currentPath }) {
  return (
    <div className="min-h-screen bg-slate-100 text-slate-950">
      <Sidebar currentPath={currentPath} />
      <main className="min-h-screen pl-0 lg:pl-64">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">{children}</div>
      </main>
    </div>
  );
}

function Sidebar({ currentPath }) {
  return (
    <aside className="border-slate-200 bg-white lg:fixed lg:inset-y-0 lg:left-0 lg:w-64 lg:border-r">
      <div className="flex h-full flex-col">
        <div className="border-b border-slate-200 px-5 py-5">
          <div className="text-lg font-semibold tracking-normal text-slate-950">AegisOps</div>
          <div className="mt-1 text-sm text-slate-500">Operations Console</div>
        </div>
        <nav className="flex gap-2 overflow-x-auto px-4 py-3 lg:flex-col lg:gap-1 lg:overflow-visible">
          {navItems.map((item) => {
            const active = currentPath === item.href || currentPath.startsWith(`${item.href}/`);
            return (
              <a
                key={item.href}
                href={item.href}
                onClick={(event) => {
                  event.preventDefault();
                  navigate(item.href);
                }}
                className={[
                  'whitespace-nowrap rounded-md px-3 py-2 text-sm font-medium transition',
                  active ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950',
                ].join(' ')}
              >
                {item.label}
              </a>
            );
          })}
        </nav>
      </div>
    </aside>
  );
}

function PageHeader({ title, description, action }) {
  return (
    <div className="mb-6 flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="text-2xl font-semibold tracking-normal text-slate-950">{title}</h1>
        <p className="mt-1 max-w-3xl text-sm leading-6 text-slate-600">{description}</p>
      </div>
      {action}
    </div>
  );
}

function IncidentsPage() {
  const [state, setState] = useAsyncData(fetchIncidents);

  return (
    <>
      <PageHeader
        title="Incidents"
        description="Deduplicated operational issues created from incoming alerts."
        action={<RefreshButton onClick={() => setState.reload()} />}
      />
      <Panel>
        {state.loading && <LoadingState label="Loading incidents" />}
        {state.error && <ErrorState message={state.error} onRetry={() => setState.reload()} />}
        {!state.loading && !state.error && <IncidentTable incidents={state.data ?? []} />}
      </Panel>
    </>
  );
}

function IncidentDetailPage({ incidentId }) {
  const [incidentState, setIncidentState] = useAsyncData(() => fetchIncident(incidentId), [incidentId]);
  const [analysis, setAnalysis] = useState(null);
  const [actionState, setActionState] = useState({ loading: false, error: '' });

  async function runAction(action) {
    setActionState({ loading: true, error: '' });
    try {
      const result = await action();
      if (result?.summary) {
        setAnalysis(result);
      }
      await setIncidentState.reload();
      setActionState({ loading: false, error: '' });
    } catch (error) {
      setActionState({ loading: false, error: error.message });
    }
  }

  return (
    <>
      <PageHeader
        title="Incident Detail"
        description="Review alert context, run analysis, approve remediation, and resolve the incident."
        action={<BackButton />}
      />
      {incidentState.loading && <LoadingState label="Loading incident" />}
      {incidentState.error && <ErrorState message={incidentState.error} onRetry={() => setIncidentState.reload()} />}
      {!incidentState.loading && !incidentState.error && incidentState.data && (
        <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_380px]">
          <div className="space-y-5">
            <IncidentDetailCard incident={incidentState.data} />
            <AlertList alerts={incidentState.data.alerts ?? []} />
          </div>
          <div className="space-y-5">
            <Panel>
              <div className="space-y-3">
                <button
                  className="button-primary w-full"
                  disabled={actionState.loading}
                  onClick={() => runAction(() => analyzeIncident(incidentId))}
                >
                  Analyze Incident
                </button>
                <button
                  className="button-secondary w-full"
                  disabled={actionState.loading}
                  onClick={() =>
                    runAction(() =>
                      approveIncident(incidentId, {
                        approvedBy: 'ops-console',
                        actionType: 'REMEDIATION',
                      }),
                    )
                  }
                >
                  Approve Remediation
                </button>
                <button
                  className="button-danger w-full"
                  disabled={actionState.loading}
                  onClick={() => runAction(() => resolveIncident(incidentId))}
                >
                  Resolve Incident
                </button>
                {actionState.error && <ErrorState message={actionState.error} compact />}
              </div>
            </Panel>
            <AIRecommendationCard analysis={analysis} />
          </div>
        </div>
      )}
    </>
  );
}

function RunbooksPage() {
  const [state, setState] = useAsyncData(fetchRunbooks);
  const [submitState, setSubmitState] = useState({ loading: false, error: '', message: '' });

  async function handleSubmit(payload) {
    setSubmitState({ loading: true, error: '', message: '' });
    try {
      const response = await createRunbook(payload);
      await setState.reload();
      setSubmitState({
        loading: false,
        error: '',
        message: response.indexed ? 'Runbook uploaded and indexed.' : 'Runbook uploaded. Vector indexing degraded.',
      });
    } catch (error) {
      setSubmitState({ loading: false, error: error.message, message: '' });
    }
  }

  return (
    <>
      <PageHeader title="Runbooks" description="Upload operator guidance and inspect indexed runbook metadata." />
      <div className="grid gap-5 xl:grid-cols-[420px_minmax(0,1fr)]">
        <RunbookForm onSubmit={handleSubmit} state={submitState} />
        <Panel>
          {state.loading && <LoadingState label="Loading runbooks" />}
          {state.error && <ErrorState message={state.error} onRetry={() => setState.reload()} />}
          {!state.loading && !state.error && <RunbookList runbooks={state.data ?? []} />}
        </Panel>
      </div>
    </>
  );
}

function AuditLogsPage() {
  const [state, setState] = useAsyncData(fetchAuditLogs);

  return (
    <>
      <PageHeader
        title="Audit Logs"
        description="Operational record of alert ingestion, deduplication, AI analysis, approvals, and resolution."
        action={<RefreshButton onClick={() => setState.reload()} />}
      />
      <Panel>
        {state.loading && <LoadingState label="Loading audit logs" />}
        {state.error && <ErrorState message={state.error} onRetry={() => setState.reload()} />}
        {!state.loading && !state.error && <AuditLogTable logs={state.data ?? []} />}
      </Panel>
    </>
  );
}

function IncidentTable({ incidents }) {
  if (incidents.length === 0) return <EmptyState label="No incidents found" />;

  return (
    <div className="overflow-x-auto">
      <table className="data-table">
        <thead>
          <tr>
            <th>Incident</th>
            <th>Service</th>
            <th>Severity</th>
            <th>Status</th>
            <th>Started</th>
          </tr>
        </thead>
        <tbody>
          {incidents.map((incident) => (
            <tr
              key={incident.id}
              className="cursor-pointer"
              onClick={() => navigate(`/incidents/${incident.id}`)}
            >
              <td className="font-mono text-xs text-slate-700">{shortId(incident.id)}</td>
              <td className="font-medium text-slate-950">{incident.serviceName}</td>
              <td><SeverityBadge severity={incident.severity} /></td>
              <td><StatusBadge status={incident.status} /></td>
              <td>{formatDate(incident.startedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function IncidentDetailCard({ incident }) {
  return (
    <Panel>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="text-sm text-slate-500">{incident.id}</div>
          <h2 className="mt-2 text-xl font-semibold tracking-normal text-slate-950">{incident.title}</h2>
          <div className="mt-3 flex flex-wrap gap-2">
            <SeverityBadge severity={incident.severity} />
            <StatusBadge status={incident.status} />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3 text-sm sm:min-w-72">
          <InfoTile label="Service" value={incident.serviceName} />
          <InfoTile label="Started" value={formatDate(incident.startedAt)} />
          <InfoTile label="Resolved" value={incident.resolvedAt ? formatDate(incident.resolvedAt) : 'Open'} />
          <InfoTile label="Dedup Key" value={incident.dedupKey} />
        </div>
      </div>
    </Panel>
  );
}

function AlertList({ alerts }) {
  return (
    <Panel title="Related Alerts">
      {alerts.length === 0 ? (
        <EmptyState label="No alerts returned for this incident" />
      ) : (
        <div className="space-y-3">
          {alerts.map((alert) => (
            <div key={alert.id} className="rounded-md border border-slate-200 bg-slate-50 p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="font-medium text-slate-950">{alert.metric}</div>
                <SeverityBadge severity={alert.severity} />
              </div>
              <div className="mt-3 grid gap-3 text-sm sm:grid-cols-4">
                <InfoTile label="Value" value={alert.value} />
                <InfoTile label="Threshold" value={alert.threshold} />
                <InfoTile label="Region" value={alert.region} />
                <InfoTile label="Created" value={formatDate(alert.createdAt)} />
              </div>
            </div>
          ))}
        </div>
      )}
    </Panel>
  );
}

function AIRecommendationCard({ analysis }) {
  return (
    <Panel title="AI Recommendation">
      {!analysis ? (
        <EmptyState label="Run analysis to generate RCA and remediation guidance" />
      ) : (
        <div className="space-y-4">
          <InfoTile label="Summary" value={analysis.summary} />
          <InfoTile label="Probable Root Cause" value={analysis.probableRootCause} />
          <div className="grid grid-cols-2 gap-3">
            <InfoTile label="Confidence" value={`${Math.round((analysis.confidence ?? 0) * 100)}%`} />
            <InfoTile label="Model" value={analysis.modelUsed} />
          </div>
          <RemediationSteps steps={analysis.remediationSteps ?? []} />
        </div>
      )}
    </Panel>
  );
}

function RemediationSteps({ steps }) {
  return (
    <div>
      <div className="text-xs font-semibold uppercase text-slate-500">Remediation Steps</div>
      {steps.length === 0 ? (
        <div className="mt-2 text-sm text-slate-500">No remediation steps returned.</div>
      ) : (
        <ol className="mt-2 space-y-2">
          {steps.map((step, index) => (
            <li key={`${step.description}-${index}`} className="rounded-md border border-slate-200 bg-white p-3 text-sm">
              <span className="mr-2 font-semibold text-slate-950">{step.order ?? index + 1}.</span>
              {step.description ?? step}
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

function RunbookForm({ onSubmit, state }) {
  const [form, setForm] = useState({ serviceName: '', title: '', content: '' });

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  return (
    <Panel title="Upload Runbook">
      <form
        className="space-y-4"
        onSubmit={(event) => {
          event.preventDefault();
          onSubmit(form);
        }}
      >
        <Field label="Service Name">
          <input className="field" value={form.serviceName} onChange={(event) => update('serviceName', event.target.value)} required />
        </Field>
        <Field label="Title">
          <input className="field" value={form.title} onChange={(event) => update('title', event.target.value)} required />
        </Field>
        <Field label="Content">
          <textarea
            className="field min-h-48 resize-y"
            value={form.content}
            onChange={(event) => update('content', event.target.value)}
            required
          />
        </Field>
        <button className="button-primary w-full" disabled={state.loading}>
          {state.loading ? 'Uploading...' : 'Upload Runbook'}
        </button>
        {state.error && <ErrorState message={state.error} compact />}
        {state.message && <div className="rounded-md border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">{state.message}</div>}
      </form>
    </Panel>
  );
}

function RunbookList({ runbooks }) {
  if (runbooks.length === 0) return <EmptyState label="No runbooks uploaded" />;

  return (
    <div className="space-y-3">
      {runbooks.map((runbook) => (
        <div key={runbook.id} className="rounded-md border border-slate-200 bg-white p-4">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <div className="font-medium text-slate-950">{runbook.title}</div>
              <div className="mt-1 text-sm text-slate-500">{runbook.serviceName}</div>
            </div>
            <div className="text-sm text-slate-500">{formatDate(runbook.createdAt)}</div>
          </div>
        </div>
      ))}
    </div>
  );
}

function AuditLogTable({ logs }) {
  if (logs.length === 0) return <EmptyState label="No audit logs found" />;

  return (
    <div className="overflow-x-auto">
      <table className="data-table">
        <thead>
          <tr>
            <th>Action</th>
            <th>Entity Type</th>
            <th>Entity ID</th>
            <th>Metadata</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          {logs.map((log) => (
            <tr key={log.id}>
              <td className="font-medium text-slate-950">{log.action}</td>
              <td>{log.entityType}</td>
              <td className="font-mono text-xs">{shortId(log.entityId)}</td>
              <td className="max-w-md truncate font-mono text-xs">{log.metadata || '-'}</td>
              <td>{formatDate(log.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function StatusBadge({ status }) {
  const classes = {
    OPEN: 'bg-blue-50 text-blue-700 ring-blue-200',
    ANALYZING: 'bg-amber-50 text-amber-700 ring-amber-200',
    ANALYZED: 'bg-cyan-50 text-cyan-700 ring-cyan-200',
    APPROVED: 'bg-violet-50 text-violet-700 ring-violet-200',
    RESOLVED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  };
  return <Badge className={classes[status] ?? 'bg-slate-50 text-slate-700 ring-slate-200'}>{status}</Badge>;
}

function SeverityBadge({ severity }) {
  const classes = {
    SEV1: 'bg-rose-50 text-rose-700 ring-rose-200',
    SEV2: 'bg-orange-50 text-orange-700 ring-orange-200',
    SEV3: 'bg-slate-50 text-slate-700 ring-slate-200',
  };
  return <Badge className={classes[severity] ?? 'bg-slate-50 text-slate-700 ring-slate-200'}>{severity}</Badge>;
}

function Badge({ className, children }) {
  return <span className={`inline-flex rounded-full px-2 py-1 text-xs font-semibold ring-1 ${className}`}>{children}</span>;
}

function LoadingState({ label }) {
  return <div className="rounded-md border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">{label}...</div>;
}

function ErrorState({ message, onRetry, compact = false }) {
  return (
    <div className={`rounded-md border border-rose-200 bg-rose-50 text-sm text-rose-800 ${compact ? 'p-3' : 'p-4'}`}>
      <div>{message}</div>
      {onRetry && (
        <button className="mt-3 text-sm font-semibold text-rose-900 underline" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
}

function EmptyState({ label }) {
  return <div className="rounded-md border border-dashed border-slate-300 bg-slate-50 p-6 text-center text-sm text-slate-500">{label}</div>;
}

function Panel({ title, children }) {
  return (
    <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
      {title && <h2 className="mb-4 text-base font-semibold tracking-normal text-slate-950">{title}</h2>}
      {children}
    </section>
  );
}

function InfoTile({ label, value }) {
  return (
    <div className="min-w-0">
      <div className="text-xs font-semibold uppercase text-slate-500">{label}</div>
      <div className="mt-1 break-words text-sm text-slate-900">{value ?? '-'}</div>
    </div>
  );
}

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-slate-700">{label}</span>
      <div className="mt-1">{children}</div>
    </label>
  );
}

function RefreshButton({ onClick }) {
  return <button className="button-secondary" onClick={onClick}>Refresh</button>;
}

function BackButton() {
  return <button className="button-secondary" onClick={() => navigate('/incidents')}>Back to Incidents</button>;
}

function useAsyncData(loader, deps = []) {
  const [state, setState] = useState({ loading: true, error: '', data: null });

  async function load() {
    setState((current) => ({ ...current, loading: true, error: '' }));
    try {
      const data = await loader();
      setState({ loading: false, error: '', data });
    } catch (error) {
      setState({ loading: false, error: error.message, data: null });
    }
  }

  useEffect(() => {
    load();
  }, deps);

  return [state, { reload: load }];
}

function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function shortId(value) {
  if (!value) return '-';
  return value.length > 12 ? `${value.slice(0, 8)}...` : value;
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
