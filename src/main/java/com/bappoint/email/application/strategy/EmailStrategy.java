package com.bappoint.email.application;

import com.bappoint.email.dto.EmailContent;
import com.bappoint.email.dto.EmailDto;

public interface EmailStrategy {
    String type();

    EmailContent buildEmail(EmailDto dto);
}
