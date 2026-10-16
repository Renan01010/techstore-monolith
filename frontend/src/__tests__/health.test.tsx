import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { getApiReadiness } from '../api/health';
import { ApiStatus } from '../components/ApiStatus';

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('getApiReadiness', () => {
  it('returns a healthy status when the monolith reports UP', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ status: 'UP' }), { status: 200 }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(getApiReadiness()).resolves.toEqual({ status: 'UP' });
    expect(fetchMock).toHaveBeenCalledWith('/actuator/health/readiness', {
      headers: { Accept: 'application/json' },
      signal: undefined,
    });
  });

  it('rejects non-success responses and malformed health payloads', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(new Response('', { status: 503 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ status: 'DOWN' }), { status: 200 })));

    await expect(getApiReadiness()).rejects.toThrow('HTTP 503');
    await expect(getApiReadiness()).rejects.toThrow('invalid readiness response');
  });
});

describe('ApiStatus', () => {
  it('shows available after a successful API check', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ status: 'UP' }), { status: 200 }),
    ));

    render(<ApiStatus />);

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('API disponível'));
  });

  it('shows unavailable when the backend cannot be reached', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')));

    render(<ApiStatus />);

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('API indisponível'));
  });
});
