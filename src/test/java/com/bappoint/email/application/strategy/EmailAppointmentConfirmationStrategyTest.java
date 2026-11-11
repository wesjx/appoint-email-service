package com.bappoint.email.application.strategy;

import com.bappoint.email.application.command.SendEmailCommand;
import com.bappoint.email.application.domain.EmailKey;
import com.bappoint.email.application.port.out.EmailTemplateRenderer;
import com.bappoint.email.application.value.EmailContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailAppointmentConfirmationStrategyTest {

    private static final String TEMPLATE = "email/appointment-confirmation";

    @Mock
    private EmailTemplateRenderer templateRenderer;

    @InjectMocks
    private EmailAppointmentConfirmationStrategy strategy;

    @Captor
    private ArgumentCaptor<Map<String, Object>> variablesCaptor;

    private static SendEmailCommand command(String userName, String date, String time,
                                            String companyName, String serviceName) {
        return new SendEmailCommand(
                UUID.randomUUID(),
                userName,
                EmailKey.APPOINTMENT_CONFIRMATION,
                "sender@email.com",
                "recipient@email.com",
                date,
                time,
                companyName,
                serviceName);
    }

    private static SendEmailCommand validCommand() {
        return command("Wesley", "2026-10-30", "14:30", "Barbearia X", "Haircut");
    }

    @Test
    void shouldHandleAppointmentConfirmationType() {
        assertThat(strategy.type()).isEqualTo(EmailKey.APPOINTMENT_CONFIRMATION);
    }

    @Test
    void shouldBuildEmailWithTheExpectedSubject() {
        when(templateRenderer.render(anyString(), anyMap())).thenReturn("<html></html>");

        EmailContent content = strategy.buildEmail(validCommand());

        assertThat(content.subject()).isEqualTo("Appointment confirmed");
    }

    @Test
    void shouldRenderTheConfirmationTemplate() {
        when(templateRenderer.render(anyString(), anyMap())).thenReturn("<html></html>");

        strategy.buildEmail(validCommand());

        verify(templateRenderer).render(eq(TEMPLATE), anyMap());
    }

    @Test
    void shouldPassAllVariablesToTheTemplate() {
        when(templateRenderer.render(anyString(), anyMap())).thenReturn("<html></html>");

        strategy.buildEmail(validCommand());

        verify(templateRenderer).render(eq(TEMPLATE), variablesCaptor.capture());
        assertThat(variablesCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "name", "Wesley",
                "date", "2026-10-30",
                "time", "14:30",
                "companyName", "Barbearia X",
                "serviceName", "Haircut"));
    }

    @Test
    void shouldUseTheRenderedHtmlAsBody() {
        when(templateRenderer.render(anyString(), anyMap())).thenReturn("<html>rendered</html>");

        EmailContent content = strategy.buildEmail(validCommand());

        assertThat(content.body()).isEqualTo("<html>rendered</html>");
    }

    @ParameterizedTest
    @MethodSource("commandsWithOneNullField")
    void shouldFailWhenAnyTemplateVariableIsNull(SendEmailCommand invalid) {
        assertThatThrownBy(() -> strategy.buildEmail(invalid))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(templateRenderer);
    }

    static Stream<SendEmailCommand> commandsWithOneNullField() {
        return Stream.of(
                command(null, "2026-10-30", "14:30", "Barbearia X", "Haircut"),
                command("Wesley", null, "14:30", "Barbearia X", "Haircut"),
                command("Wesley", "2026-10-30", null, "Barbearia X", "Haircut"),
                command("Wesley", "2026-10-30", "14:30", null, "Haircut"),
                command("Wesley", "2026-10-30", "14:30", "Barbearia X", null));
    }
}