package com.bappoint.email.application.command;

import com.bappoint.email.application.domain.EmailKey;

import java.util.UUID;

public record SendEmailCommand(
        UUID userId,
        String userName,
        EmailKey type,
        String emailFrom,
        String emailTo,
        String appointmentDate,
        String appointmentTime,
        String companyName,
        String serviceName
) {}
