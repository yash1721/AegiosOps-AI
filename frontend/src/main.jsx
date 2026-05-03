import React from 'react';
import ReactDOM from 'react-dom/client';
import './styles.css';

function App() {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

  return (
    <main className="min-h-screen bg-zinc-950 text-zinc-100">
      <section className="mx-auto flex min-h-screen max-w-5xl flex-col justify-center px-6 py-12">
        <p className="text-sm font-medium uppercase tracking-wide text-emerald-400">AegisOps</p>
        <h1 className="mt-4 text-4xl font-semibold tracking-normal text-white sm:text-5xl">
          Operations console foundation
        </h1>
        <p className="mt-5 max-w-2xl text-base leading-7 text-zinc-300">
          React, Vite, and Tailwind are ready. The backend API base URL is configured from the environment.
        </p>
        <div className="mt-8 rounded border border-zinc-800 bg-zinc-900 p-4">
          <p className="text-sm text-zinc-400">API base URL</p>
          <p className="mt-2 break-all font-mono text-sm text-emerald-300">{apiBaseUrl}</p>
        </div>
      </section>
    </main>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
