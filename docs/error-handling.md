# Error Handling and Reliability

## Acknowledgement and retry settings

The listener container is configured with `acknowledge-mode: auto`, `default-requeue-rejected: false`, and retry enabled with:

| Setting | Value |
|---|---:|
| Maximum attempts | 3 total attempts (initial invocation plus retries) |
| Initial retry interval | 1 second |
| Multiplier | 2 |
| Maximum retry interval | 10 seconds |

The intended retry intervals for three attempts are approximately 1 second between attempts one and two, then 2 seconds between attempts two and three. The configured 10-second cap is not reached with only three attempts. Retry applies to listener processing failures handled by the configured Spring AMQP retry advice; broker connection recovery is separate. Exact classification of conversion and fatal exceptions depends on the Spring AMQP error-handler path.

With auto acknowledgement, normal listener return results in acknowledgement. After retries are exhausted, the configured recoverer rejects without requeueing. Because requeue-on-reject is disabled, a rejected work-queue message is not put back on the same queue.

## Dead-letter behavior

`RabbitMQConfig` declares a durable direct DLX and adds its name plus the configured dead-letter routing key to the durable work queue. The YAML also contains a DLQ name, but no code declares a queue under that name or binds it to the DLX. Consequently:

1. Rejection after retries can cause RabbitMQ to dead-letter the message to the configured exchange/routing key.
2. The message reaches a DLQ only if a queue is provisioned and bound to that DLX/routing key by external infrastructure.
3. With no matching binding, the message can be discarded as unroutable. The application has no DLQ inspection or replay tooling.

Do not assume that `email.dlq` exists or that messages are recoverable until broker topology is verified.

## SES behavior

`SesEmailGateway` catches AWS SDK `SesException`, logs an error and the recipient, and does not rethrow. The `EmailService` and consumer can therefore return normally after an SES service error; RabbitMQ may acknowledge the request as processed. The application does not persist a failed-send record, retry SES specifically, return an error to the producer, or distinguish SES acceptance from final mailbox delivery.

Exceptions not caught as `SesException` may propagate to the listener and enter listener retry handling. This is not a guaranteed retry policy for SES failures because the principal SDK exception type is suppressed.

## Failure scenarios

| Scenario | Actual behavior | Retry? | Final outcome | Recommended action |
|---|---|---:|---|---|
| Listener strategy/rendering throws, e.g. a null template variable | Listener invocation fails | Up to 3 total attempts under configured retry path | Rejected without requeue after exhaustion; dead-lettering depends on a matching external DLX binding | Fix/publish complete fields; inspect broker topology and rejected-message logs |
| Valid enum with no registered strategy | `EmailService` throws `IllegalArgumentException` | Up to 3 total attempts under configured retry path | Rejected without requeue; DLQ conditional on external binding | Implement the strategy or publish only supported key |
| `SesException` from SES | Gateway logs and suppresses exception | No listener retry for this caught exception | Listener may return normally and message may be acknowledged, although send failed | Alert on SES errors; fix gateway to propagate or persist failures before relying on retries |
| Non-`SesException` runtime failure in SES path | Can escape gateway/listener | Usually subject to listener retry advice; exact fatal classification is framework-dependent | Reject after exhaustion; dead-letter conditional on binding | Inspect root exception and effective listener error-handler behavior |
| Malformed JSON / incompatible field conversion | Conversion/error handler fails before or during listener invocation | Framework fatal-exception classification may bypass retry; verify deployed Spring AMQP behavior | Reject/dead-letter behavior depends on error handler and topology | Test malformed payloads against the exact deployed version; validate producer serialization |
| RabbitMQ unavailable or unreachable | Connection/listener startup or recovery fails; no message can be consumed | Not the listener retry policy | Messages remain broker-side if published; service cannot process until connection recovers | Restore network/broker and inspect connection recovery logs |
| SES unavailable | SDK may throw `SesException`, which is caught and suppressed | No listener retry for caught exception | Message can be acknowledged although email was not submitted | Treat logs/metrics as failure; implement explicit retry/outbox/DLQ policy |
| Publisher cannot reach broker or route message | Producer-side publish fails or message is unroutable | No publisher retry is implemented here | No guarantee request reached the work queue | Configure publisher confirms and mandatory returns on the producer |
| Duplicate delivery/replay | No application deduplication | N/A | Duplicate email submission is possible | Add an idempotency key and durable send status before replay/scale-out |

## Delivery guarantees and ordering

The code does not implement exactly-once processing, idempotency, deduplication, message ordering, or an application receipt. At-least-once broker behavior can result in repeated processing in failure/recovery scenarios; duplicates are possible. Multiple consumers can also process messages concurrently and complete out of order.

## Recovery

Recovery requires broker administration because no DLQ is declared/bound by the application. Before replaying a message, verify its JSON fields, destination, SES sender permissions, and whether a previous SES request may already have succeeded. Blind replay can send duplicates. Retain original message metadata and restrict access because payloads contain personal data.
