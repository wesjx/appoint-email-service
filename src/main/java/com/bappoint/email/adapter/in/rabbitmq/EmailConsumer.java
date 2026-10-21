package com.bappoint.email.adapter.in.rabbitmq;

import com.bappoint.email.application.command.SendEmailCommand;
import com.bappoint.email.application.services.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class EmailConsumer {

    private final EmailService emailService;

    public EmailConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue-name}")
    public void listenEmailQueue(@Payload EmailDto email) throws InterruptedException{
        emailService.sendEmail(new SendEmailCommand(
                email.getUserId(),
                email.getUserName(),
                email.getKey(),
                email.getEmailFrom(),
                email.getEmailTo(),
                email.getAppointmentDate(),
                email.getAppointmentTime(),
                email.getCompanyName(),
                email.getServiceName()
        ));

        System.out.println(email);
    }
}
