<!--
Sync Impact Report
Version change: unversioned template -> 1.0.0
Modified principles:
- Principle 1 name placeholder -> I. Modular Monolith
- Principle 2 name placeholder -> II. Hexagonal Modules and Explicit Boundaries
- Principle 3 name placeholder -> III. Data Ownership and Versioned Persistence
- Principle 4 name placeholder -> IV. Approved Product Scope
- Principle 5 name placeholder -> V. Specification-Driven Delivery
Added sections:
- Technology and Security Requirements
- Development Workflow and Quality Gates
Removed sections: None; the previous document contained only template placeholders.
Follow-up TODOs: None.
-->

# TechStore Monolith Constitution

## Core Principles

### I. Modular Monolith

The backend MUST be one Spring Boot application and MUST remain a modular monolith,
independent of the microservices repository. The project MUST NOT introduce an API
Gateway or independently deployed microservices. This keeps deployment cohesive while
preserving explicit business boundaries.

### II. Hexagonal Modules and Explicit Boundaries

Backend code MUST be organized into business modules with explicit boundaries. Each
module MUST separate domain, application, and adapter concerns. Internal modules MUST
communicate through Java interfaces and use cases, never through internal HTTP calls.
A module MUST NOT access another module's tables outside the defined persistence
rules, and module dependencies MUST NOT be circular.

### III. Data Ownership and Versioned Persistence

The application MUST use PostgreSQL and Flyway migrations. Every schema change MUST
be versioned through Flyway, and an applied migration MUST NOT be modified. Data
access MUST respect module ownership and the persistence boundaries established for
each module.

### IV. Approved Product Scope

Implementation MUST stay within the approved TechStore scope: home and visual
identity; catalog, categories, search, filters, and product details; registration,
login, email verification, and password recovery; profile and account security;
user-associated cart; and referral of interested customers to commercial contact.
Transactional checkout, payments, orders, and order tracking are out of scope and
MUST NOT be implemented unless separately approved through a specification.

### V. Specification-Driven Delivery

Before implementing each feature, the team MUST produce and review its specification,
plan, tasks, and consistency analysis. Implementation MUST NOT begin until those
artifacts are reviewed, and MUST NOT exceed their approved scope. Each implementation
MUST include relevant automated tests and updated execution instructions.

## Technology and Security Requirements

The required stack is Java 21, Spring Boot, Maven, React, TypeScript, Vite,
PostgreSQL, Flyway, Spring Security, JWT authentication, and Docker Compose for the
local environment. Automated tests MUST use JUnit and other tools compatible with
the project.

HTTP APIs MUST validate input and use DTOs for their contracts. Passwords MUST be
stored using an appropriate password-hashing algorithm. Authenticated operations and
user data MUST be protected so each user can access only their own data. Passwords,
JWTs, and password-recovery tokens MUST NOT be written to logs. Environment
variables MUST be documented without publishing secrets.

## Development Workflow and Quality Gates

Changes MUST include relevant unit, integration, and security tests. Database
changes MUST comply with the Flyway migration rules in this constitution. Execution
and setup instructions MUST be kept current, including instructions for the local
Docker Compose environment where applicable.

Before implementation, reviewers MUST confirm that the specification, plan, tasks,
and consistency analysis agree with one another and respect the approved product
scope. Before completion, reviewers MUST verify module boundaries, security
requirements, relevant test coverage, migration safety, and updated execution
instructions.

## Governance

This constitution governs feature specifications, plans, implementation, and review.
Amendments MUST be reviewed and approved before they take effect. Every amendment
MUST update the version and last-amended date, and MUST include a Sync Impact Report
for human review.

Constitution versions MUST follow semantic versioning: MAJOR for backward-incompatible
governance or principle changes, MINOR for new principles or materially expanded
requirements, and PATCH for clarifications and non-semantic refinements. Feature
reviews MUST check compliance with this constitution; any exception MUST be
explicitly approved and documented in the relevant specification.

**Version**: 1.0.0 | **Ratified**: 2026-10-16 | **Last Amended**: 2026-10-16
