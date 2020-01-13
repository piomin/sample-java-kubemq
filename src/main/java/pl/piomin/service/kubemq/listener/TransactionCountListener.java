package pl.piomin.service.kubemq.listener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLException;

import io.grpc.stub.StreamObserver;
import io.kubemq.sdk.basic.ServerAddressNotSuppliedException;
import io.kubemq.sdk.event.EventReceive;
import io.kubemq.sdk.event.Subscriber;
import io.kubemq.sdk.subscription.EventsStoreType;
import io.kubemq.sdk.subscription.SubscribeRequest;
import io.kubemq.sdk.subscription.SubscribeType;
import io.kubemq.sdk.tools.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.piomin.service.kubemq.exception.InsufficientFundsException;
import pl.piomin.service.kubemq.model.Order;
import pl.piomin.service.kubemq.repository.AccountRepository;

import org.springframework.stereotype.Component;

@Component
public class TransactionCountListener implements StreamObserver<EventReceive> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionCountListener.class);
    private Map<Integer, Integer> transactionsCount = new HashMap<>();

    private Subscriber subscriber;
    private AccountRepository accountRepository;

    public TransactionCountListener(Subscriber subscriber, AccountRepository accountRepository) {
        this.subscriber = subscriber;
        this.accountRepository = accountRepository;
    }

    @Override
    public void onNext(EventReceive eventReceive) {
        try {
            Order order = (Order) Converter.FromByteArray(eventReceive.getBody());
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
        } catch (IOException | ClassNotFoundException | InsufficientFundsException e) {
            LOGGER.error("Error", e);
        }
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onCompleted() {

    }

    @PostConstruct
    public void init() {
        final SubscribeRequest subscribeRequest = new SubscribeRequest();
        subscribeRequest.setChannel("transactions");
        subscribeRequest.setClientID("count-listener-" + System.currentTimeMillis());
        subscribeRequest.setSubscribeType(SubscribeType.EventsStore);
        subscribeRequest.setEventsStoreType(EventsStoreType.StartFromFirst);
        try {
            subscriber.SubscribeToEvents(subscribeRequest, this);
        } catch (ServerAddressNotSuppliedException | SSLException e) {
            e.printStackTrace();
        }
    }

}
