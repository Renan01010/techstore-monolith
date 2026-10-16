import { execFileSync } from 'node:child_process';

const timeoutMs = Number(process.env.STACK_SMOKE_TIMEOUT_MS ?? 120_000);
const deadline = Date.now() + timeoutMs;

function dockerCompose(args) {
  return execFileSync('docker', ['compose', ...args], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  }).trim();
}

async function waitFor(description, check) {
  let lastError;
  while (Date.now() < deadline) {
    try {
      if (await check()) {
        console.log(`${description}: ready`);
        return;
      }
    } catch (error) {
      lastError = error;
    }
    await new Promise((resolve) => setTimeout(resolve, 1_000));
  }
  const cause = lastError instanceof Error ? ` Last probe error: ${lastError.message}` : '';
  throw new Error(`Timed out waiting for ${description}.${cause}`);
}

try {
  const configuredServices = dockerCompose(['config', '--services'])
    .split(/\r?\n/)
    .filter(Boolean)
    .sort();
  const expectedServices = ['backend', 'frontend', 'postgres'];
  if (JSON.stringify(configuredServices) !== JSON.stringify(expectedServices)) {
    throw new Error(`Expected only ${expectedServices.join(', ')} services; found ${configuredServices.join(', ')}.`);
  }

  await waitFor('PostgreSQL', () => dockerCompose(['exec', '-T', 'postgres', 'pg_isready']));
  await waitFor('Backend readiness', async () => {
    const response = fetch('http://localhost:8080/actuator/health/readiness', {
      signal: AbortSignal.timeout(2_000),
    });
    const result = await response;
    if (!result.ok) return false;
    const payload = await result.json();
    return payload.status === 'UP';
  });

  const databaseUser = dockerCompose(['exec', '-T', 'postgres', 'printenv', 'POSTGRES_USER']);
  const databaseName = dockerCompose(['exec', '-T', 'postgres', 'printenv', 'POSTGRES_DB']);
  const migrationCount = dockerCompose([
    'exec', '-T', 'postgres', 'psql', '-U', databaseUser, '-d', databaseName,
    '-tAc', "SELECT count(*) FROM public.flyway_schema_history WHERE version = '1'",
  ]);
  if (migrationCount !== '1') {
    throw new Error(`Expected one Flyway V1 record; received ${migrationCount || 'no output'}.`);
  }
  console.log('Flyway V1 migration: applied');

  const runningServices = dockerCompose(['ps', '--status', 'running', '--services'])
    .split(/\r?\n/)
    .filter(Boolean);
  if (!runningServices.includes('frontend')) {
    throw new Error('The frontend Compose service is not running.');
  }
  console.log('Frontend Compose service: running');
} catch (error) {
  console.error(`Local stack verification failed: ${error instanceof Error ? error.message : String(error)}`);
  process.exitCode = 1;
}
