# Security

## Credentials and secrets

The checked-in `application.yaml` currently includes hard-coded RabbitMQ and AWS credentials. They are deliberately not repeated in documentation. Treat them as compromised: revoke/rotate them, remove hard-coded values, review repository history and access, and use a secret manager or protected deployment variables. Merely adding a variable in `src/main/resources/.env` does not make Spring Boot load it automatically.

The custom SES client reads static credentials from `aws.accessKeyId` and `aws.secretKey`. The `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` placeholders under `cloud.aws.credentials` do not configure these custom `@Value` properties. Update the application configuration (or use a supported AWS credential provider chain/IAM role) before deployment.

RabbitMQ username/password have fallback literals in the YAML. Override them securely and rotate exposed credentials. Use separate least-privilege identities for publishing and consuming/topology declaration. Avoid putting credentials in shell history, command-line arguments, Docker image layers, message URLs, logs, or source files.

A `.env` file is present under `src/main/resources`. Spring Boot does not automatically load it as a dotenv file, and the repository's `.gitignore` does not exclude `.env` files. Do not store credentials there; remove/rotate any secrets in it and ensure secret files are excluded from version control and build artifacts.

## Transport and authorization

Use TLS-protected AMQPS in production and validate broker certificates. Restrict network access to the broker and SES endpoints. Limit RabbitMQ virtual-host permissions to necessary exchange/queue configure, publish, and consume operations. IAM permissions should be scoped to the minimum SES send actions and identities needed.

The repository configures a broker address, but certificate validation policy and client TLS customization are not explicitly configured in application code. Verify the effective broker TLS setup in the deployment environment.

## Personal data and logging

Messages contain user name, recipient email, appointment date/time, company, and service details. The consumer prints the entire DTO; the service prints rendered `EmailContent`; and SES exception logging includes the recipient. These are potential PII disclosures. Restrict log access, shorten retention, redact identifiers, and avoid logging body/payload data.

Do not place sensitive data in subjects or template values unless required. Protect RabbitMQ queues and backups, and apply retention limits to dead-lettered messages.

## Untrusted message content

RabbitMQ messages are external inputs. The DTO has no Bean Validation constraints and the consumer performs no explicit field validation. The Thymeleaf template uses `th:text` for dynamic values, which escapes text in normal Thymeleaf HTML mode; do not change to unescaped output for user-provided content without security review. Limit message size and restrict publishers at the broker.

## Replay and abuse risks

The service has no deduplication or idempotency checks. Replaying a message can submit another email. Validate destinations, authorize publishers, audit manual replay, and avoid exposing the DLX/DLQ management interface publicly.

## Security capabilities not present

The repository does not implement a business HTTP API, payload signature verification, field validation, PII redaction, idempotency, security audit events, or documented key rotation automation. Springdoc's generated documentation endpoints are enabled by default; disable them in production if not needed.
