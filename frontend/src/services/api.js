const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
    ...options,
  });

  const text = await response.text();
  const body = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const message = body?.message || `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  return body;
}

export function fetchIncidents() {
  return request('/incidents');
}

export function fetchIncident(incidentId) {
  return request(`/incidents/${incidentId}`);
}

export function analyzeIncident(incidentId) {
  return request(`/incidents/${incidentId}/analyze`, { method: 'POST' });
}

export function approveIncident(incidentId, payload) {
  return request(`/incidents/${incidentId}/approve`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function resolveIncident(incidentId) {
  return request(`/incidents/${incidentId}/resolve`, { method: 'POST' });
}

export function fetchRunbooks() {
  return request('/runbooks');
}

export function createRunbook(payload) {
  return request('/runbooks', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function fetchAuditLogs() {
  return request('/audit-logs');
}
