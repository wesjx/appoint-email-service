# Development and Contribution

## Package map

| Package/path | Responsibility |
|---|---|
| `BappointEmailServiceApplication` | Spring Boot entry point |
| `adapter/in/rabbitmq` | `EmailConsumer` listener and `EmailDto` wire model |
| `adapter/out/ses` | `SesEmailGateway`, outbound SES adapter |
| `adapter/out/thymeleaf` | `ThymeleafEmailTemplateRenderer` |
| `application/command` | Immutable `SendEmailCommand` |
| `application/domain` | `EmailKey` enum |
| `application/port/out` | `EmailGateway` and `EmailTemplateRenderer` interfaces |
| `application/services` | `EmailService`, strategy selection and orchestration |
| `application/strategy` | `EmailStrategy` contract and confirmation implementation |
| `application/value` | `EmailContent` subject/body value |
| `configuration` | RabbitMQ topology and custom Amazon SES client |
| `resources/templates/email` | Thymeleaf HTML template |

## Build and test

Use Java 25 and the Maven Wrapper:

```sh
./mvnw clean test
./mvnw package
```

There is one context-load test and no unit tests for the consumer, strategy, rendering, retry behavior, RabbitMQ topology, or SES gateway. The context test may need external property values and infrastructure; do not interpret it as an end-to-end verification. No coverage percentage has been measured.

## Adding an email type

1. Add an enum constant to `EmailKey`.
2. Implement an `EmailStrategy` returning that key.
3. Add a matching Thymeleaf template under `src/main/resources/templates/email/`.
4. Ensure the strategy only passes non-null values to `Map.of` or introduce explicit validation/error handling.
5. Add tests for message-to-command mapping, strategy selection, template rendering, and SES gateway behavior.
6. Update [message contract](message-contract.md) and examples if the wire schema changes.

Only `APPOINTMENT_CONFIRMATION` has a strategy currently, despite other enum values being defined.

## Changing the message contract or topology

`EmailDto` is the consumer wire schema. Coordinate field changes with every producer, update the mapper in `EmailConsumer`, and preserve compatibility or explicitly version/migrate messages. The current code has no schema version and no formal validation.

Exchange/queue/routing-key settings appear under `app.rabbitmq.*`; connection and listener settings appear under `spring.rabbitmq.*`. Keep `RabbitMQConfig` property references, YAML names, publisher configuration, and any broker-provisioned DLQ binding aligned. The `app.rabbitmq.dead-letter-queue` value is currently unused. Test topology changes against a real disposable broker before rollout.

## Contribution workflow

- Keep changes focused and follow existing Java/Spring naming and package conventions.
- Never commit credentials or production payloads.
- Add tests for changed behavior and update the relevant docs.
- Verify the Maven build and inspect the final diff before submitting a pull request.
- No CI workflow, formatter/linter configuration, or established contribution guide is present; agree on these with maintainers before introducing project-wide tooling.

## Known technical debt

- Hard-coded broker/AWS credentials and inconsistent AWS property sources.
- SES exceptions are logged and suppressed.
- No schema validation, idempotency, status persistence, DLQ binding, metrics, health endpoint, integration tests, or CI workflow.
- Stale/unused dependencies and configuration: database/JPA/Flyway settings without email persistence, and Spring Mail starter though email sends through SES.
- Template references `year`, but the strategy does not pass it.
- Dockerfile port differs from application port.
