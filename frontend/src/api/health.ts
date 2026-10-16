export type ApiReadiness = {
  status: 'UP';
};

export async function getApiReadiness(signal?: AbortSignal): Promise<ApiReadiness> {
  const response = await fetch('/actuator/health/readiness', {
    headers: { Accept: 'application/json' },
    signal,
  });

  if (!response.ok) {
    throw new Error(`Readiness request failed with HTTP ${response.status}`);
  }

  const payload: unknown = await response.json();
  if (
    typeof payload !== 'object'
    || payload === null
    || !('status' in payload)
    || payload.status !== 'UP'
  ) {
    throw new Error('The backend returned an invalid readiness response');
  }

  return { status: 'UP' };
}
