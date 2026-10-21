package com.bappoint.email.application.port.out;

import java.util.Map;

public interface EmailTemplateRenderer {
    String render(String templateName, Map<String, Object> variables);
}
