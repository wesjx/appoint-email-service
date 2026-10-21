package com.bappoint.email.application.services;

import com.bappoint.email.application.command.SendEmailCommand;
import com.bappoint.email.application.domain.EmailKey;
import com.bappoint.email.application.strategy.EmailStrategy;
import com.bappoint.email.application.value.EmailContent;
import com.bappoint.email.adapter.in.rabbitmq.EmailDto;
import com.bappoint.email.application.port.out.EmailGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private final EmailGateway emailGateway;

    private final Map<EmailKey, EmailStrategy> strategies;

    public EmailService(List<EmailStrategy> strategyList, EmailGateway emailGateway) {
        this.emailGateway = emailGateway;

        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(EmailStrategy::type, Function.identity()));
    }

    public void sendEmail(SendEmailCommand command) {
        EmailKey type = command.type();
        String emailTo = command.emailTo();
        EmailStrategy strategy = strategies.get(type);
        if (strategy != null) {
            EmailContent content = strategies.get(type).buildEmail(command);

            emailGateway.send(emailTo, content);

            System.out.println(content);

        } else {
            throw new IllegalArgumentException("No strategy found for type: " + type);
        }
    }


}
