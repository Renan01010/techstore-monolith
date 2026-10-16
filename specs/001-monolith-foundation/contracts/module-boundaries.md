# Internal Contract: Business Module Boundaries

## Module packages

The single backend application contains four top-level packages under one base
package:

```text
com.techstore.identity
com.techstore.catalog
com.techstore.cart
com.techstore.shared
```

Each module includes `domain`, `application`, and `adapter` package boundaries.
Adapters may distinguish `in` and `out` responsibilities. No business use case is
required by this foundation; package markers and architecture tests are sufficient
to establish the empty boundaries.

## Visibility and dependencies

- A module root package or explicitly named API/interface package is its only public
  cross-module entry point.
- Module internals remain package-private where Java allows.
- A caller invokes another module's application use case through an explicit Java
  interface; calls run in-process.
- Internal HTTP, direct calls into another module's implementation packages, and
  direct access to another module's persistence/schema are forbidden.
- Module dependencies are acyclic. A module may depend on a public interface of
  another module only when a specified use case requires it.
- `shared` may contain technical cross-cutting contracts, but no module-specific
  business rules, repositories, or entities.
- The application is verified by an automated module-boundary test. The test must
  fail on an undeclared dependency or dependency cycle.

## Database ownership

`identity`, `catalog`, and `cart` own their respective PostgreSQL schemas. A module
adds migrations only for its schema and accesses only its own schema. Flyway remains
application-wide with one ordered migration history. The `shared` package owns no
business data.

## Events

No event contract is introduced. Future events require an approved feature
specification that defines their business meaning and delivery guarantees; they are
not a substitute for synchronous use-case calls.
