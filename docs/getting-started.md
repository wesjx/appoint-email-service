# Getting Started

## Prerequisites

- Java 25 (the project sets `java.version` to 25).
- A shell capable of running the checked-in Maven Wrapper (`./mvnw`).
- A reachable RabbitMQ broker and an Amazon SES account for end-to-end email processing.

The application does not use a database. The repository does not include a local RabbitMQ container or a mock SES service.

## Prepare the repository

```sh
git clone <repository-url>
cd bappoint-email-service
```

Before starting the service, review [configuration](configuration.md) and [security](security.md). Provide credentials through environment variables, IDE run configuration, or a secret manager; do not commit them. Spring Boot does not automatically load `src/main/resources/.env` as a dotenv file.

## Configure external services

Set RabbitMQ and SES configuration as described in [configuration](configuration.md). Ensure:

- The RabbitMQ broker is reachable from the process.
- The service identity has the permissions needed to declare the configured exchanges/queue/binding and consume messages.
- An external publisher can publish to the configured work exchange and routing key.
- AWS credentials used by the custom `SesClient` are valid for the configured AWS region.
- The SES sender identity configured by `aws.ses.from` is verified and allowed to send to the chosen recipient under the SES account's mode and limits.

Do not put credentials in documentation or source control. The code's custom SES client reads `aws.accessKeyId` and `aws.secretKey`; configure these properties securely for your environment.

## Build, test, and run

```sh
./mvnw clean test
./mvnw spring-boot:run
```

To create a package:

```sh
./mvnw clean package
```

There is one `@SpringBootTest` context-load test. It is not isolated from external configuration; missing credentials or required services may prevent the Spring context from loading. The test can also complete successfully even if RabbitMQ topology declaration logs an error, because it does not assert that the listener is consuming messages. During validation, the configured broker reported that the existing work queue had arguments different from the application's declaration. Resolve that broker-side conflict deliberately before relying on consumption; do not delete/recreate a queue without assessing its messages and bindings. No dedicated integration test, test broker, or SES mock is provided.

No database is needed for these commands.

## Verify startup and publish a test message

The application listens on port `8081` according to `server.port`. It has no business HTTP endpoint or Actuator health endpoint; generated Springdoc endpoints are enabled by default. Confirm successful startup from application logs and verify the RabbitMQ consumer is active in broker management tooling. A successful publish does not prove that an email was sent; see [message contract](message-contract.md) and [operations](operations.md).

Publish the JSON in [message contract](message-contract.md) to the configured direct exchange/routing key. Use a test address authorized by SES and never paste production credentials or personal information into shared logs.

## Docker image

The Dockerfile copies an already-built `target/*.jar`; build the JAR first:

```sh
./mvnw package
docker build -t bappoint-email-service .
```

The image declares port `3030`, while the application is configured for `8081`. No application container or RabbitMQ service is defined in `compose.yaml`. See [deployment](deployment.md) before container use.
