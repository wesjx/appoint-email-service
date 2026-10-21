# Deployment

## Supported packaging

The Maven Wrapper builds the application JAR:

```sh
./mvnw clean package
```

The provided Dockerfile uses `eclipse-temurin:25-jdk`, copies `target/*.jar`, and runs it with `java -jar`. Build the JAR before building the image:

```sh
docker build -t bappoint-email-service .
```

The Dockerfile declares `EXPOSE 3030`, while `application.yaml` configures `server.port: 8081`. The effective Spring port remains 8081 unless overridden. Align the Dockerfile/port mapping before deployment; no health check or container entrypoint wait-for-dependencies is provided.

`compose.yaml` contains only PostgreSQL 16 with a local persistent volume. It is a development database helper, not a complete service deployment. No CI/CD workflow, Kubernetes manifests, Helm chart, or production compose stack is present.

## Required infrastructure

Production deployment needs:

1. RabbitMQ with a reachable AMQP/TLS endpoint, application credentials, and a virtual host.
2. A durable work exchange/queue/binding compatible with `app.rabbitmq.*`. The application declares its work topology, but broker permissions must permit it.
3. A dead-letter exchange plus an explicitly provisioned durable DLQ and binding to the configured dead-letter routing key if dead-letter retention is required.
4. AWS SES in the region used by `aws.region`, a verified sender matching `aws.ses.from`, credentials/role permissions to submit email, and recipient authorization appropriate to the SES account's sending mode.
5. PostgreSQL if retaining the configured JPA datasource dependency in the application runtime. No email data persistence is implemented.

Use TLS for broker connections and the deployment platform's secret manager. Do not use the hard-coded credentials in the checked-in YAML. The custom SES client currently uses static basic credentials; prefer workload IAM roles/short-lived credentials after changing the application configuration to support them.

## Container execution

The following is a deployment-shaped example; it assumes an image has been built, the port mismatch has been resolved, and required environment values are supplied by a secure runtime:

```sh
docker run --rm \
  -p 8081:8081 \
  -e RABBITMQ_ADDRESSES='<amqps broker URI>' \
  -e RABBITMQ_USERNAME='<injected username>' \
  -e RABBITMQ_PASSWORD='<injected password>' \
  -e RABBITMQ_VHOST='<virtual host>' \
  -e AWS_ACCESS_KEY_ID='<injected key>' \
  -e AWS_SECRET_ACCESS_KEY='<injected secret>' \
  bappoint-email-service
```

This command is illustrative, not directly sufficient with the current YAML: AWS variables are configured under `cloud.aws.*`, while `AmazonSesConfig` reads `aws.accessKeyId` and `aws.secretKey`, currently hard-coded in YAML. Replace those properties with environment/role-based configuration before production. The application does not currently expose environment-variable placeholders for its custom SES credentials.

## SES and network setup

- Permit outbound network access to RabbitMQ's configured AMQP/TLS port and AWS SES API endpoints for the configured region.
- Verify the sender identity in SES and use an IAM identity with least-privilege send permissions (for example, scoped `ses:SendEmail` as appropriate to the account).
- Check SES sandbox/production status, recipient verification requirements, quotas, and sending rate in the target account.
- Set the sender and AWS region consistently; `cloud.aws.region.static` and `aws.region` differ in the current YAML, and the custom SES client uses `aws.region`.

## Readiness, scaling, and shutdown

Spring Boot starts a web server on port 8081, but no HTTP routes, Actuator health endpoint, readiness probe, or metrics endpoint are implemented. A listening TCP port is not proof of RabbitMQ or SES readiness. Monitor process logs and broker consumer state externally.

Multiple replicas can consume from the same queue, but there is no ordering guarantee or idempotency. A deployment restart can interrupt in-flight work; RabbitMQ acknowledgement and redelivery behavior can lead to duplicates. Graceful shutdown/readiness behavior has not been customized or verified in this repository.

## Production improvements (not currently implemented)

- Align the Docker port and application port; add a non-root runtime image and health/readiness checks.
- Provision and verify DLQ topology, retention, alerting, and a controlled replay process.
- Replace static AWS credentials with an IAM role and make SES failures visible to the retry/recovery system.
- Add message validation, idempotency, send status persistence, and integration tests.
- Add CI/CD, resource limits, operational dashboards, and a documented secrets rotation procedure.
