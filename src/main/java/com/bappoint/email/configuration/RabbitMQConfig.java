package com.bappoint.email.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange emailExchange(
            @Value("${spring.rabbitmq.exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange(
            @Value("${spring.rabbitmq.dead-letter-exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    public Queue emailQueue(
            @Value("${app.rabbitmq.queue-name}") String name,
            @Value("${app.rabbitmq.dead-letter-exchange}") String deadLetterExchange,
            @Value("${app.rabbitmq.dead-letter-routing-key}") String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }

    @Bean
    public Binding emailBinding(
            @Qualifier("emailQueue") Queue queue,
            @Qualifier("emailExchange") DirectExchange exchange,
            @Value("${app.rabbitmq.routing-key}") String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

}
