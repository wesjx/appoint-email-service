# Operations and Troubleshooting

## Current operational signals

The application uses Spring Boot's default console logging plus explicit `System.out`/`System.err` and SLF4J calls. It prints the consumed `EmailDto` and rendered `EmailContent`; these objects can include email addresses, user names, appointment details, and HTML. `SesEmailGateway` logs SES exceptions with the recipient address.

No Actuator dependency, health endpoint, metrics, tracing, structured logging configuration, alert rules, or application-specific dashboards are present. Use broker and AWS service consoles plus process logs. Avoid collecting message bodies or PII in centralized logs.

## Troubleshooting

| Symptom | Likely area | Checks |
|---|---|---|
| `NoRouteToHostException` / Rabbit connection failure | Network or broker address | Check resolved `spring.rabbitmq.addresses`, port, DNS, firewall/VPN, broker status, TLS URI, and network route |
| Authentication/vhost refusal | RabbitMQ credentials/permissions | Verify username, password, virtual host, configure/consume/write permissions; do not paste credentials into logs |
| `PRECONDITION_FAILED` when declaring an existing queue | Broker topology drift | RabbitMQ requires an existing queue's durability and arguments to match a redeclaration; compare its current arguments with `RabbitMQConfig`, then plan a migration without losing queued messages |
| Placeholder cannot be resolved | Property names/config source | Confirm topology properties use `app.rabbitmq.*`; connection properties use `spring.rabbitmq.*`; environment variables are actually passed to the process |
| Consumer sees no messages | Routing/topology | Confirm publisher uses the configured direct exchange and routing key, JSON content type, queue binding, and active consumer |
| `NullPointerException` in `Map.of` | Missing template input | Check non-null `userName`, `appointmentDate`, `appointmentTime`, `companyName`, and `serviceName` in the actual JSON, not only the publisher's object `toString()` |
| No strategy found | Enum/strategy mismatch | Use `key: "APPOINTMENT_CONFIRMATION"`; other enum constants have no strategy |
| SES error appears but broker message disappears | SES exception suppression | `SesEmailGateway` catches `SesException`; it may log and return, allowing normal listener completion/acknowledgement |
| Dead-letter message is not visible | DLX binding | Application does not declare the configured DLQ or bind it; provision a queue bound to the DLX and routing key, then verify broker topology |
| Application fails to start due to database | PostgreSQL config/connectivity | Confirm PostgreSQL is reachable at configured URL; the Compose helper maps host port 5433 |
| Docker container not reachable as expected | Port mismatch | Dockerfile exposes 3030; application defaults to 8081 |

Springdoc exposes generated `/v3/api-docs` and `/swagger-ui.html` endpoints by default. These are API-description endpoints, not email submission endpoints. The context-load test does not verify that the consumer successfully declared its queue or is receiving messages.

## Safe message inspection and recovery

Use broker management tooling with access controls to inspect message headers and bodies. Payloads can contain personally identifiable information (PII). Redact before sharing diagnostic output.

Before replay:

1. Confirm the message matches the current JSON contract and has all non-null template inputs.
2. Confirm the SES sender, region, account mode, and recipient permissions.
3. Check whether an earlier SES call may have accepted the message; the application has no idempotency key or send ledger.
4. Replay only through an approved, audited process. Duplicate email submission is possible.

This repository includes no DLQ consumer, replay command, or automated recovery tool.

## Operational checklist

- Verify RabbitMQ connection, work queue, binding, active consumers, and broker alarms.
- Verify that the DLX has a bound DLQ and that retention/alerting policies exist; neither is fully provisioned in the repository.
- Monitor SES send/reject metrics in AWS; application-level SES failure reporting is incomplete.
- Restrict access to logs and redact email addresses and request payloads.
- Check database, RabbitMQ, and AWS availability separately; this service has no unified readiness endpoint.
- Rotate credentials exposed in the checked-in configuration and deploy only secret-managed replacements.
