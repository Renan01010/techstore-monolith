# HTTP Contracts: Foundation Health and Errors

This feature exposes no catalog, identity, authentication, cart, or other business
HTTP operations.

## Readiness

### `GET /actuator/health/readiness`

Public status-only readiness check for the local frontend and Compose health checks.
It reports `UP` only if the backend is ready and its required PostgreSQL connection
is healthy. Component names, connection details, credentials, and diagnostic details
must not be exposed.

Example success:

```json
{
  "status": "UP"
}
```

When readiness is down, the response uses a non-success HTTP status and a non-UP
status. Compose must wait for PostgreSQL health before starting the backend and for
backend readiness before treating frontend/API smoke validation as ready.

Liveness is a separate operational signal and does not fail solely because
PostgreSQL is unavailable.

## Request Correlation

- Header: `X-Correlation-Id`
- Accept a non-empty incoming value of at most 128 ASCII characters matching
  `[A-Za-z0-9._-]+`.
- If missing or invalid, generate a UUID for the request.
- Return the selected value as `X-Correlation-Id` in the response.
- Include it in request-scoped logs; remove it from the logging context after request
  processing.
- Do not treat the identifier as a secret or authentication credential.

## Error Response

All application errors use an RFC 9457-compatible problem document through Spring
`ProblemDetail`, with a stable correlation extension. The response content type is
`application/problem+json`.

```json
{
  "type": "about:blank",
  "title": "Request validation failed",
  "status": 400,
  "detail": "One or more request fields are invalid.",
  "instance": "/example",
  "correlationId": "generated-or-validated-id",
  "errors": [
    {
      "field": "example",
      "message": "Invalid value."
    }
  ]
}
```

`errors` is present only when field validation errors exist. Do not include rejected
secret values, stack traces, SQL, database connection details, passwords, JWTs, or
password-recovery tokens. Unexpected server errors use a generic title/detail and
the correlation ID.
