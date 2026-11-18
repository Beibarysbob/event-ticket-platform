package kz.kimep.ticketing.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;

@Configuration
public class RabbitConfiguration {
    public static final String EXCHANGE = "ticketing.events";
    public static final String DEAD_EXCHANGE = "ticketing.dlx";
    public static final String QUEUE = "ticketing.notifications";

    @Bean
    DirectExchange eventExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    FanoutExchange deadExchange() {
        return new FanoutExchange(DEAD_EXCHANGE, true, false);
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(DEAD_EXCHANGE).build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable("ticketing.deadletters").build();
    }

    @Bean
    Binding confirmed(Queue notificationQueue, DirectExchange eventExchange) {
        return BindingBuilder.bind(notificationQueue).to(eventExchange).with("booking.confirmed");
    }

    @Bean
    Binding cancelled(Queue notificationQueue, DirectExchange eventExchange) {
        return BindingBuilder.bind(notificationQueue).to(eventExchange).with("booking.cancelled");
    }

    @Bean
    Binding failed(Queue notificationQueue, DirectExchange eventExchange) {
        return BindingBuilder.bind(notificationQueue).to(eventExchange).with("payment.failed");
    }

    @Bean
    Binding deadLetterBinding(Queue deadLetterQueue, FanoutExchange deadExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadExchange);
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory factory) {
        SimpleRabbitListenerContainerFactory listener = new SimpleRabbitListenerContainerFactory();
        listener.setConnectionFactory(factory);
        listener.setDefaultRequeueRejected(false);
        listener.setAdviceChain(RetryInterceptorBuilder.stateless().maxAttempts(3)
            .backOffOptions(1000, 2.0, 4000)
            .recoverer(new RejectAndDontRequeueRecoverer()).build());
        return listener;
    }
}
