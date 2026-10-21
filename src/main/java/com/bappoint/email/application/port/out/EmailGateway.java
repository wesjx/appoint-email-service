package com.bappoint.email.application.port.out;

import com.bappoint.email.application.value.EmailContent;

public interface EmailGateway {
    void send(String emailTo, EmailContent content);
}
