# Architecture Decisions and Limitations

These records describe technical choices visible in the current implementation. The original authors' historical rationale is not recorded; rationale below is an assessment, not a claim about project history.

## ADR-001: Use RabbitMQ for asynchronous email requests

**Context:** Appointment processing and email delivery are separate operations with different latency and availability characteristics.

**Decision (implemented):** Consume requests from a RabbitMQ queue using Spring AMQP.

**Alternatives considered:** Synchronous HTTP call to an email service; direct SDK call from the appointment application; another message broker.

**Advantages:** Decouples the producer from the email worker's immediate execution and provides broker-side buffering.

**Disadvantages and trade-offs:** Requires broker operations and contract coordination. The application has no producer receipt, idempotency, processing status, or end-to-end delivery tracking. Ordering and duplicate prevention are not implemented.

**Consequences:** Producers must publish the exact JSON contract to the direct exchange/routing key. Broker permissions, connectivity, and topology are operational dependencies.

## ADR-002: Use AWS SES through an application gateway

**Context:** The service needs an outbound email provider.

**Decision (implemented):** `SesEmailGateway` uses an AWS SDK `SesClient` and sends HTML content using configured sender identity.

**Alternatives considered:** SMTP/Spring Mail or another email provider.

**Advantages:** Keeps provider-specific request handling behind `EmailGateway` and uses SES APIs.

**Disadvantages and trade-offs:** The client currently uses static credentials; SES exceptions are logged and suppressed, so failures are not propagated to RabbitMQ retry handling. SDK success is not inbox-delivery confirmation.

**Consequences:** Configure SES region, credentials/role, sender verification, sending mode, and limits. Before production, make delivery failures observable and recoverable.

## ADR-003: Use a strategy and Thymeleaf template

**Context:** Different email types may require different content.

**Decision (implemented):** `EmailService` selects an `EmailStrategy` by `EmailKey`; the appointment confirmation strategy renders a named Thymeleaf template through a renderer port.

**Alternatives considered:** A single conditional service/template or an external template platform.

**Advantages:** Separates orchestration, formatting, and provider integration and gives a natural extension point for more message types.

**Disadvantages and trade-offs:** Only one strategy exists. There is no schema validation; null template variables fail at runtime. The current template expects `year`, which is not supplied.

**Consequences:** New enum values require an actual strategy and template. Test template variables and escaping when adding content.

## ADR-004: Retry listener failures and reject without requeue

**Context:** Transient listener failures should not immediately lose a message, while permanently failing messages should not loop forever on the work queue.

**Decision (implemented):** Listener retry is enabled with 3 total attempts, 1-second initial delay, multiplier 2, 10-second cap; rejected messages are not requeued. The work queue has DLX arguments.

**Alternatives considered:** Immediate requeue, unbounded retry, application-managed retry queue, or no retry.

**Advantages:** Bounds listener retries and avoids an immediate poison-message loop.

**Disadvantages and trade-offs:** Application declares no DLQ or DLX binding, and SES `SesException` is swallowed before the listener sees it. Recovery and retention are therefore incomplete.

**Consequences:** Operators must provision and verify the DLQ binding externally. Retry/reject behavior should be integration-tested against the deployed Spring AMQP version and broker.

## ADR-005: Keep database dependencies/configuration

**Context:** The repository includes PostgreSQL and JPA configuration.

**Decision (present in build/configuration):** Include Spring Data JPA, PostgreSQL, and a datasource configuration.

**Assessment:** No entity, repository, or email persistence behavior is present. It is unclear whether the database is a future dependency or unused setup.

**Advantages:** A database foundation exists if persistence is added.

**Disadvantages and trade-offs:** Adds an infrastructure dependency and startup/configuration burden without current email-flow value. Flyway properties are present without the Flyway dependency or migration files.

**Consequences:** Confirm whether PostgreSQL is required. If not, remove unused dependencies/configuration; if it is required, implement persistence and migrations with explicit operational requirements.

## Current limitations

- No HTTP API, producer, callback, delivery status, or audit ledger.
- No formal validation, schema versioning, idempotency, or ordering guarantee.
- No declared/bound DLQ in the application.
- SES API exceptions can be suppressed.
- No health/metrics/tracing endpoints, CI/CD configuration, or end-to-end messaging tests.
- Hard-coded credentials are present in application configuration and must be rotated/replaced.
- Docker port and configured application port differ.
