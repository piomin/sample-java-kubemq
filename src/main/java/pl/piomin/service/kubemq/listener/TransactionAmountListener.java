package pl.piomin.service.kubemq.listener;

import io.kubemq.sdk.pubsub.PubSubClient;
import io.kubemq.sdk.pubsub.EventsStoreSubscription;
import io.kubemq.sdk.pubsub.EventsStoreType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.piomin.service.kubemq.exception.InsufficientFundsException;
import pl.piomin.service.kubemq.model.Order;
import pl.piomin.service.kubemq.repository.AccountRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TransactionAmountListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionAmountListener.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private PubSubClient pubSubClient;
    private AccountRepository accountRepository;

    public TransactionAmountListener(PubSubClient pubSubClient, AccountRepository accountRepository) {
        this.pubSubClient = pubSubClient;
        this.accountRepository = accountRepository;
    }

    @PostConstruct
    public void init() {
        try {
            EventsStoreSubscription subscription = EventsStoreSubscription.builder()
                    .channel("transactions")
                    .group("")
                    .eventsStoreType(EventsStoreType.StartNewOnly)
                    .onReceiveEventCallback(event -> {
                        try {
                            Order order = objectMapper.readValue(event.getBody(), Order.class);
                            LOGGER.info("Amount event: {}", order);
                            accountRepository.updateBalance(order.getAccountIdTo(), (int) (order.getAmount() * 0.1));
                        } catch (InsufficientFundsException e) {
                            LOGGER.error("Error", e);
                        } catch (Exception e) {
                            LOGGER.error("Error processing event", e);
                        }
                    })
                    .onErrorCallback(error -> {
                        LOGGER.error("Subscription error", error);
                    })
                    .build();

            pubSubClient.subscribeToEventsStore(subscription);
        } catch (Exception e) {
            LOGGER.error("Error initializing subscription", e);
        }
    }
}
