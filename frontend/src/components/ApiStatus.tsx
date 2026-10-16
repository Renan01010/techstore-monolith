import { useEffect, useState } from 'react';
import { getApiReadiness } from '../api/health';

type ApiState = 'checking' | 'available' | 'unavailable';

export function ApiStatus() {
  const [state, setState] = useState<ApiState>('checking');

  useEffect(() => {
    const controller = new AbortController();

    getApiReadiness(controller.signal)
      .then(() => setState('available'))
      .catch(() => {
        if (!controller.signal.aborted) setState('unavailable');
      });

    return () => controller.abort();
  }, []);

  const label = {
    checking: 'Verificando conexão com a API',
    available: 'API disponível',
    unavailable: 'API indisponível',
  }[state];

  return (
    <p className={`api-status api-status--${state}`} role="status" aria-live="polite">
      <span aria-hidden="true" />
      {label}
    </p>
  );
}
