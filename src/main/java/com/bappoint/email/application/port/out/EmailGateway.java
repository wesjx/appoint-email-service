package com.bappoint.email.service;

import com.bappoint.email.dto.EmailContent;

public interface EmailGateway {
    void send(String emailTo, EmailContent content);
}
