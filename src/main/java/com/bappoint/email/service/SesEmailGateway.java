package com.bappoint.email.service;

import com.bappoint.email.dto.EmailContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

@Slf4j
@Service
public class SesEmailService implements EmailGateway {

    private final SesClient sesClient;

    @Value("${aws.ses.from}")
    private String from;

    public SesEmailService(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    @Override
    public void send(String emailTo, EmailContent content) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(from)
                .destination(d -> d.toAddresses(emailTo))
                .message(m -> m
                        .subject(s -> s.data(content.subject()).charset("UTF-8"))
                        .body(b -> b.html(h -> h.data(content.body()).charset("UTF-8"))))
                .build();
        try {
            sesClient.sendEmail(request);
        } catch (SesException e) {
            System.err.println("Error sending email: " + e.getMessage());
            log.error("Error sending email to {}", emailTo, e);
//            throw new EmailSendingException("Failed to send email", e);
        }

    }

}