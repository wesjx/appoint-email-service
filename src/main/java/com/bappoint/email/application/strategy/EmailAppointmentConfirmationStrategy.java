package com.bappoint.email.application.strategy;

import com.bappoint.email.application.command.SendEmailCommand;
import com.bappoint.email.application.domain.EmailKey;
import com.bappoint.email.application.port.out.EmailTemplateRenderer;
import com.bappoint.email.application.value.EmailContent;
import com.bappoint.email.adapter.in.rabbitmq.EmailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailAppointmentConfirmationStrategy implements EmailStrategy {

    private final EmailTemplateRenderer templateRenderer;

    @Override
    public EmailKey type() {
        return EmailKey.APPOINTMENT_CONFIRMATION;
    }

    @Override
    public EmailContent buildEmail(SendEmailCommand command) {
        String html = templateRenderer.render(
                "email/appointment-confirmation",
                Map.of(
                        "name", command.userName(),
                        "date", command.appointmentDate(),
                        "time", command.appointmentTime(),
                        "companyName", command.companyName(),
                        "serviceName", command.serviceName()
                )
        );
        return new EmailContent("Appointment confirmed", html);
    }

}
