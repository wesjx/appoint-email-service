package com.bappoint.email.application;

import com.bappoint.email.dto.EmailContent;
import com.bappoint.email.dto.EmailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailAppointmentConfirmationStrategy implements EmailStrategy {

    private final TemplateEngine templateEngine;

    @Override
    public String type() {
        return "appointment-confirmed";
    }

    @Override
    public EmailContent buildEmail(EmailDto dto) {
        Context context = new Context();
        context.setVariable("name", dto.getUserName());
        context.setVariable("date", dto.getAppointmentDate());

        String html = templateEngine.process("email/appointment-confirmation", context);
        return new EmailContent("Appointment confirmed", html);
    }

}
