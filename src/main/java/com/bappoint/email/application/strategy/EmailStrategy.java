package com.bappoint.email.application.strategy;

import com.bappoint.email.application.command.SendEmailCommand;
import com.bappoint.email.application.domain.EmailKey;
import com.bappoint.email.application.value.EmailContent;
import com.bappoint.email.adapter.in.rabbitmq.EmailDto;

public interface EmailStrategy {
    EmailKey type();

    EmailContent buildEmail(SendEmailCommand command);
}
