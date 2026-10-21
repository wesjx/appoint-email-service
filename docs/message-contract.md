# Message Contract and Publisher Integration

## Destination and encoding

Publish to the configured **direct exchange** (`app.rabbitmq.exchange`, default `emailExchange`) using the configured **routing key** (`app.rabbitmq.routing-key`, default `email.send`). The service binds the configured work queue (`app.rabbitmq.queue-name`, default `emailQueue`) to that exchange/key.

Use a JSON object with `content_type` / AMQP content type `application/json`. The consumer uses `JacksonJsonMessageConverter` and binds the body to `EmailDto`. The broker topology is declared by the service, but producers should not rely on the service being available to create the exchange before first publish; provision topology separately or start the service first.

## Schema

There are no Bean Validation annotations or explicit null checks on `EmailDto`. “Operationally required” below means the current code needs the field for the supported email path; it is not enforced by a formal validator.

| Field | JSON type | Operational requirement | Handling |
|---|---|---|---|
| `userId` | string UUID or `null` | No | Deserialized to `UUID`; unused in email rendering/sending |
| `userName` | string | Yes for template rendering | Template variable `name`; null causes `Map.of` to throw |
| `key` | string enum | Yes | Must deserialize to `EmailKey`; supported strategy is `APPOINTMENT_CONFIRMATION` |
| `emailFrom` | string | No for current flow | Deserialized but unused; sender comes from `aws.ses.from` |
| `emailTo` | string | Yes for SES request | Recipient passed to SES |
| `appointmentDate` | string | Yes for template rendering | Template variable `date`; no format validation |
| `appointmentTime` | string | Yes for template rendering | Template variable `time`; no format validation |
| `companyName` | string | Yes for template rendering | Template variable `companyName` |
| `serviceName` | string | Yes for template rendering | Template variable `serviceName` |

`Map.of` rejects null values. All five template values (`userName`, `appointmentDate`, `appointmentTime`, `companyName`, and `serviceName`) must therefore be non-null for rendering. Date/time formats are not parsed; they are rendered as supplied. Unknown/unsupported enum values do not select a strategy; valid enum values other than `APPOINTMENT_CONFIRMATION` currently have no strategy.

No correlation ID, message ID, idempotency key, deduplication, schema version, or application-level response is read or created by this code. RabbitMQ protocol properties may exist, but the application does not use them for processing guarantees.

## Supported example

```json
{
  "userId": "c8bcb8e5-7419-4e17-84b6-bf4b1ced3034",
  "userName": "Alex Example",
  "key": "APPOINTMENT_CONFIRMATION",
  "emailFrom": "appointments@example.invalid",
  "emailTo": "alex@example.invalid",
  "appointmentDate": "2026-10-25",
  "appointmentTime": "14:30",
  "companyName": "Example Studio",
  "serviceName": "Consultation"
}
```

The `.invalid` addresses are placeholders and cannot receive mail. For a real test, use an authorized recipient and a verified SES sender under the account's sending-mode restrictions.

The publisher's appointment object is not the consumer contract. For example, nested `company.name` or `services[].name` values are not automatically mapped to `companyName` or `serviceName`. The publisher must flatten/rename these fields before publishing, or the consumer contract/code must be intentionally changed.

## Spring AMQP publisher example

This is an illustrative producer-side example. It is not included in this repository. Configure the producer's `RabbitTemplate` with a Jackson JSON message converter and valid broker connection credentials:

```java
Map<String, Object> payload = Map.of(
    "userId", "c8bcb8e5-7419-4e17-84b6-bf4b1ced3034",
    "userName", "Alex Example",
    "key", "APPOINTMENT_CONFIRMATION",
    "emailFrom", "appointments@example.invalid",
    "emailTo", "alex@example.invalid",
    "appointmentDate", "2026-10-25",
    "appointmentTime", "14:30",
    "companyName", "Example Studio",
    "serviceName", "Consultation"
);
rabbitTemplate.convertAndSend("emailExchange", "email.send", payload);
```

If exchange and routing-key names are overridden, use the same values as the running service. Do not include secrets in source code. Producer-side publisher confirms/mandatory-return handling are not configured by this repository; configure them in the producer if broker-acceptance feedback is required.

## Generic producer example

Illustrative Python example using `pika` (a producer-side dependency, not used by this project):

```python
import json
import pika

connection = pika.BlockingConnection(
    pika.URLParameters("amqps://<username>:<password>@<broker-host>/<virtual-host>")
)
channel = connection.channel()
channel.basic_publish(
    exchange="emailExchange",
    routing_key="email.send",
    body=json.dumps(payload),
    properties=pika.BasicProperties(content_type="application/json"),
)
connection.close()
```

Use a secret manager or environment-based connection setup rather than embedding credentials in the URL or source file. The example does not enable publisher confirms.

## Invalid and failed messages

- A malformed JSON body or incompatible field type can fail conversion before the listener method executes.
- An invalid `key` enum value can fail JSON deserialization; a valid but unsupported enum value reaches strategy lookup and fails with `IllegalArgumentException`.
- Missing/null template fields fail rendering because of `Map.of`.
- Missing/invalid recipient data can cause the SES request to fail.
- The application does not provide a validation response to the publisher. Consult [error handling](error-handling.md) for retries and final broker behavior.

These examples describe failure cases; they are not supported successful requests.

## Delivery semantics and confirmation

The publisher receives no application-level response, email status, or callback from this service. A successful publish operation only reports what the producer's AMQP client/broker confirms under its own configuration; this application does not enable publisher confirms or return handling. SES acceptance and final mailbox delivery are distinct events. There is no exactly-once guarantee, ordering guarantee, deduplication, or status API.
