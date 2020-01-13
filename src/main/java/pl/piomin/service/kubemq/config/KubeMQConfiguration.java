package pl.piomin.service.kubemq.config;

import io.kubemq.sdk.basic.ServerAddressNotSuppliedException;
import io.kubemq.sdk.event.Channel;
import io.kubemq.sdk.event.Subscriber;
import io.kubemq.sdk.queue.Queue;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLException;

@Configuration
@ConfigurationProperties("kubemq")
public class KubeMQConfiguration {

    private String address;

    @Bean
    public Queue queue() throws ServerAddressNotSuppliedException, SSLException {
        return new Queue("transactions", "orders", address);
    }

    @Bean
    public Subscriber subscriber() {
        return new Subscriber(address);
    }

    @Bean
    public Channel channel() {
        return new Channel("transactions", "orders", true, address);
    }

    String getAddress() {
        return address;
    }

    void setAddress(String address) {
        this.address = address;
    }

}
