# Configuration Reference

Configuration is read from `src/main/resources/application.yaml` and standard Spring Boot property sources. Environment placeholders use `${VARIABLE:default}` where a default is shown. In Spring Boot, external configuration can override packaged configuration; prefer deployment environment variables or a secret manager for production.

> Never put credential values in this document, source control, or shared logs. Provide secrets through a secret manager or protected environment configuration.

## Environment variables referenced in the YAML

| Variable | Purpose | Required | Example or default | Sensitive |
|---|---|---:|---|---:|
| `RABBITMQ_ADDRESSES` | RabbitMQ broker address(es) | Depends on the active configuration | Set the environment-specific broker URI | Yes; may reveal infrastructure |
| `RABBITMQ_USERNAME` | RabbitMQ username | Depends on the active configuration | Provide through secret management | Yes |
| `RABBITMQ_PASSWORD` | RabbitMQ password | Depends on the active configuration | Provide through secret management | Yes |
| `RABBITMQ_VHOST` | RabbitMQ virtual host | Depends on the active configuration | Set the provisioned vhost | Treat as environment detail |
| `RABBITMQ_QUEUE_NAME` | Work queue name (`app.rabbitmq.queue-name`) | No | `emailQueue` | No |
| `RABBITMQ_EXCHANGE_NAME` | Work direct-exchange name | No | `emailExchange` | No |
| `RABBITMQ_ROUTING_KEY` | Work binding/publish routing key | No | `email.send` | No |
| `RABBITMQ_DLX_NAME` | Dead-letter exchange name | No | `email.dlx` | No |
| `RABBITMQ_DLQ_NAME` | Configured dead-letter queue name | No | `email.dlq`; currently unused by topology code | No |
| `RABBITMQ_DLQ_ROUTING_KEY` | Queue dead-letter routing key | No | `email.failed` | No |
| `AWS_ACCESS_KEY_ID` | Placeholder under `cloud.aws.credentials.access-key` | Depends on the active configuration | Supply securely if required by the deployment | Yes |
| `AWS_SECRET_ACCESS_KEY` | Placeholder under `cloud.aws.credentials.secret-key` | Depends on the active configuration | Supply securely if required by the deployment | Yes |

**AWS configuration note:** `AmazonSesConfig` creates a custom SES client from `aws.accessKeyId`, `aws.secretKey`, and `aws.region`. Configure the SES credentials securely for the target environment; the `cloud.aws.credentials.*` properties are separate and do not automatically configure these custom properties.

The application does not use a database. No datasource, JPA, or database service is required for build, tests, or runtime.

## Application properties

| Property | Current setting | Notes |
|---|---|---|
| `server.port` | `8081` | No business HTTP controller or health endpoint is implemented |
| `springdoc.api-docs.enabled` / `springdoc.swagger-ui.enabled` | Enabled by Springdoc defaults | Generated `/v3/api-docs` and `/swagger-ui.html` endpoints are available despite there being no business controller; disable for production if not required |
| `spring.rabbitmq.listener.simple.acknowledge-mode` | `auto` | Successful listener return is acknowledged by the container |
| `spring.rabbitmq.listener.simple.default-requeue-rejected` | `false` | Rejected messages are not requeued by default |
| `spring.rabbitmq.listener.simple.retry.enabled` | `true` | Listener retry is enabled |
| `spring.rabbitmq.listener.simple.retry.initial-interval` | `1000` ms | First retry interval |
| `spring.rabbitmq.listener.simple.retry.max-attempts` | `3` | Maximum total listener attempts, including the initial attempt |
| `spring.rabbitmq.listener.simple.retry.multiplier` | `2` | Exponential interval multiplier |
| `spring.rabbitmq.listener.simple.retry.max-interval` | `10000` ms | Retry interval cap |
| `aws.region` | `eu-north-1` | Used by the custom SES client |
| `aws.ses.from` | Configured sender identity | Must be verified/authorized in SES |
| `cloud.aws.region.static` | `us-east-1` | Separate cloud configuration; custom SES client uses `aws.region` |
Although `spring-boot-starter-mail` is present, no SMTP credentials are configured and the application sends through SES, not Spring Mail. The `src/main/resources/.env` file is not a standard Spring Boot dotenv source; the application will not automatically load it merely because it exists.

## RabbitMQ topology properties

All topology properties are under `app.rabbitmq.*`, while connection/listener properties are under `spring.rabbitmq.*`. The custom topology uses the former:

```yaml
app:
  rabbitmq:
    queue-name: ${RABBITMQ_QUEUE_NAME:emailQueue}
    exchange: ${RABBITMQ_EXCHANGE_NAME:emailExchange}
    routing-key: ${RABBITMQ_ROUTING_KEY:email.send}
    dead-letter-exchange: ${RABBITMQ_DLX_NAME:email.dlx}
    dead-letter-queue: ${RABBITMQ_DLQ_NAME:email.dlq}
    dead-letter-routing-key: ${RABBITMQ_DLQ_ROUTING_KEY:email.failed}
```

This illustrates property names and defaults, not a complete production configuration. The `dead-letter-queue` property is currently not consumed by `RabbitMQConfig`.

## Safe external configuration

For local development, configure values in the IDE or environment without committing credentials. For production, inject secrets from the platform's secret manager. Do not copy secret values into documentation, source files, images, or logs.
