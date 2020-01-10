package pl.piomin.service.kubemq.config;

import io.kubemq.sdk.basic.ServerAddressNotSuppliedException;
import io.kubemq.sdk.event.Subscriber;
import io.kubemq.sdk.queue.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLException;

@Configuration
public class KubeMQConfiguration {

    @Bean
    public Queue queue() throws ServerAddressNotSuppliedException, SSLException {
        return new Queue("transactions", "orders", "kubemq:50000");
    }

    @Bean
    public Subscriber subscriber() {
        return new Subscriber("kubemq:50000");
    }

}
