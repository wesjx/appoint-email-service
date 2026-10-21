package com.bappoint.email.application.domain;

import com.bappoint.email.application.strategy.EmailStrategy;
import com.bappoint.email.application.records.EmailContent;
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

    private final Map<String, EmailStrategy> strategies;

    public EmailService(List<EmailStrategy> strategyList, EmailGateway emailGateway) {
        this.emailGateway = emailGateway;

        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(EmailStrategy::type, Function.identity()));
    }

    public void sendEmail(EmailDto dto) {
        String type = dto.getKey();
        String emailTo = dto.getEmailTo();
        EmailStrategy strategy = strategies.get(type);
        if (strategy != null) {
            EmailContent content = strategies.get(type).buildEmail(dto);

            emailGateway.send(emailTo, content);

            System.out.println(content);

        } else {
            throw new IllegalArgumentException("No strategy found for type: " + type);
        }
    }


}
