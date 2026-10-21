package com.bappoint.email.adapter.in.rabbitmq;

import com.bappoint.email.application.domain.EmailKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EmailDto {
    private UUID userId;
    private String userName;
    private EmailKey key;
    private String emailFrom;
    private String emailTo;
    private String appointmentDate;
    private String appointmentTime;
    private String companyName;
    private String serviceName;
}