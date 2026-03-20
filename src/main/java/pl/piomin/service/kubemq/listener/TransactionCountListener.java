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
import java.util.HashMap;
import java.util.Map;

@Component
public class TransactionCountListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionCountListener.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private Map<Integer, Integer> transactionsCount = new HashMap<>();

    private PubSubClient pubSubClient;
    private AccountRepository accountRepository;

    public TransactionCountListener(PubSubClient pubSubClient, AccountRepository accountRepository) {
        this.pubSubClient = pubSubClient;
        this.accountRepository = accountRepository;
    }

    @PostConstruct
    public void init() {
        try {
            EventsStoreSubscription subscription = EventsStoreSubscription.builder()
                    .channel("transactions")
                    .group("")
                    .eventsStoreType(EventsStoreType.StartFromFirst)
                    .onReceiveEventCallback(event -> {
                        try {
                            Order order = objectMapper.readValue(event.getBody(), Order.class);
                            LOGGER.info("Count event: {}", order);
                            Integer accountIdTo = order.getAccountIdTo();
                            Integer noOfTransactions = transactionsCount.get(accountIdTo);
                            if (noOfTransactions == null)
                                transactionsCount.put(accountIdTo, 1);
                            else {
                                transactionsCount.put(accountIdTo, ++noOfTransactions);
                                if (noOfTransactions > 5) {
                                    accountRepository.updateBalance(order.getAccountIdTo(), (int) (order.getAmount() * 0.1));
                                    LOGGER.info("Adding extra to: id={}", order.getAccountIdTo());
                                }
                            }
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
