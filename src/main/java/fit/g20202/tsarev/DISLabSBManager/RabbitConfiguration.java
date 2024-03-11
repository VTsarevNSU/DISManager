package fit.g20202.tsarev.DISLabSBManager;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory("rabbitmq");
        cachingConnectionFactory.setUsername("user");
        cachingConnectionFactory.setPassword("password");
        return cachingConnectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public Queue workerQueue() {
        return new Queue("worker_queue");
    }
    @Bean
    public Queue managerQueue() {
        return new Queue("manager_queue");
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange("exchange", true, false);
    }
    @Bean
    public Binding workerBinding(@Qualifier("workerQueue") Queue workerQueue, DirectExchange exchange){
        return BindingBuilder.bind(workerQueue).to(exchange).with("workerKey");
    }
    @Bean
    public Binding managerBinding(@Qualifier("managerQueue") Queue managerQueue, DirectExchange exchange){
        return BindingBuilder.bind(managerQueue).to(exchange).with("managerKey");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}