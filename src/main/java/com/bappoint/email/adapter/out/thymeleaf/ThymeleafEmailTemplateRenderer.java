package com.bappoint.email.adapter.out.thymeleaf;

import com.bappoint.email.application.port.out.EmailTemplateRenderer;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

@Component
public class ThymeleafEmailTemplateRenderer implements EmailTemplateRenderer {
    private final TemplateEngine templateEngine;

    public ThymeleafEmailTemplateRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String render(String templateName, Map<String, Object> variables) {
        Context context = new Context(Locale.getDefault(), variables);
        return templateEngine.process(templateName, context);
    }
}
