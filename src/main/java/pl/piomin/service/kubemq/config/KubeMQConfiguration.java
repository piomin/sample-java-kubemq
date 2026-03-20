package pl.piomin.service.kubemq.config;

import io.kubemq.sdk.pubsub.PubSubClient;
import io.kubemq.sdk.queues.QueuesClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("kubemq")
public class KubeMQConfiguration {

    private String address;

    @Bean
    public QueuesClient queuesClient() {
        return QueuesClient.builder()
                .address(address)
                .clientId("orders-service-queues")
                .build();
    }

    @Bean
    public PubSubClient pubSubClient() {
        return PubSubClient.builder()
                .address(address)
                .clientId("orders-service-pubsub")
                .build();
    }

    String getAddress() {
        return address;
    }

    void setAddress(String address) {
        this.address = address;
    }

}
