package com.checkout.bff.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.exchange.checkout:checkout.exchange}")
    private String checkoutExchange;

    @Value("${spring.rabbitmq.exchange.dlx:checkout.dlx}")
    private String dlxExchange;

    @Value("${spring.rabbitmq.queue.payment.request:checkout.payment.request}")
    private String paymentRequestQueue;

    @Value("${spring.rabbitmq.queue.payment.request.dlq:checkout.payment.request.dlq}")
    private String paymentRequestDLQ;

    @Value("${spring.rabbitmq.queue.payment.processed:checkout.payment.processed}")
    private String paymentProcessedQueue;

    @Value("${spring.rabbitmq.queue.payment.processed.dlq:checkout.payment.processed.dlq}")
    private String paymentProcessedDLQ;

    @Value("${spring.rabbitmq.queue.cart.updated:checkout.cart.updated}")
    private String cartUpdatedQueue;

    @Value("${spring.rabbitmq.queue.order.created:checkout.order.created}")
    private String orderCreatedQueue;

    @Bean
    public TopicExchange checkoutExchange() {
        return ExchangeBuilder.topicExchange(checkoutExchange).durable(true).build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(dlxExchange).durable(true).build();
    }

    @Bean
    public Queue paymentRequestQueue() {
        return QueueBuilder.durable(paymentRequestQueue)
                .withArgument("x-dead-letter-exchange", dlxExchange)
                .withArgument("x-dead-letter-routing-key", "payment.request")
                .build();
    }

    @Bean
    public Queue paymentRequestDLQ() {
        return QueueBuilder.durable(paymentRequestDLQ).build();
    }

    @Bean
    public Queue paymentProcessedQueue() {
        return QueueBuilder.durable(paymentProcessedQueue)
                .withArgument("x-dead-letter-exchange", dlxExchange)
                .withArgument("x-dead-letter-routing-key", "payment.processed")
                .build();
    }

    @Bean
    public Queue paymentProcessedDLQ() {
        return QueueBuilder.durable(paymentProcessedDLQ).build();
    }

    @Bean
    public Queue cartUpdatedQueue() {
        return QueueBuilder.durable(cartUpdatedQueue).build();
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(orderCreatedQueue).build();
    }

    @Bean
    public Binding paymentRequestBinding() {
        return BindingBuilder.bind(paymentRequestQueue())
                .to(checkoutExchange())
                .with("payment.request");
    }

    @Bean
    public Binding paymentProcessedBinding() {
        return BindingBuilder.bind(paymentProcessedQueue())
                .to(checkoutExchange())
                .with("payment.processed");
    }

    @Bean
    public Binding cartUpdatedBinding() {
        return BindingBuilder.bind(cartUpdatedQueue())
                .to(checkoutExchange())
                .with("cart.updated");
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(checkoutExchange())
                .with("order.created");
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(paymentRequestDLQ())
                .to(deadLetterExchange())
                .with("payment.request");
    }

    @Bean
    public Binding paymentProcessedDlqBinding() {
        return BindingBuilder.bind(paymentProcessedDLQ())
                .to(deadLetterExchange())
                .with("payment.processed");
    }

    @Bean
    @Primary
    public ObjectMapper messagingObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // DO NOT enable default typing - this causes issues with JSONB storage and cross-service deserialization
        return mapper;
    }

    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(messagingObjectMapper());
        // Disable default typing to avoid sending __TypeId__ headers that cause deserialization issues
        converter.setCreateMessageIds(false);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}