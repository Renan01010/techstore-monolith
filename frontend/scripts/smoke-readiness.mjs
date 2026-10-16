const readinessUrl = process.env.SMOKE_READINESS_URL
  ?? 'http://localhost:5173/actuator/health/readiness';
const timeoutMs = Number(process.env.SMOKE_TIMEOUT_MS ?? 60_000);
const deadline = Date.now() + timeoutMs;
let lastError = 'No response received';

while (Date.now() < deadline) {
  try {
    const response = await fetch(readinessUrl, {
      headers: { Accept: 'application/json' },
      signal: AbortSignal.timeout(5_000),
    });
    const payload = await response.json();

    if (response.ok && payload?.status === 'UP') {
      console.log(`Readiness smoke test passed: ${readinessUrl}`);
      process.exit(0);
    }

    lastError = `HTTP ${response.status}, status=${payload?.status ?? 'missing'}`;
  } catch (error) {
    lastError = error instanceof Error ? error.message : String(error);
  }

  await new Promise((resolve) => setTimeout(resolve, 1_000));
}

console.error(`Readiness smoke test failed after ${timeoutMs}ms: ${lastError}`);
process.exit(1);
