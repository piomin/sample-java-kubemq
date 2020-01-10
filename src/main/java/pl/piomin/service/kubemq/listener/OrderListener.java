package pl.piomin.service.kubemq.listener;

import io.kubemq.sdk.basic.ServerAddressNotSuppliedException;
import io.kubemq.sdk.queue.Queue;
import io.kubemq.sdk.queue.ReceiveMessagesResponse;
import io.kubemq.sdk.queue.Transaction;
import io.kubemq.sdk.queue.TransactionMessagesResponse;
import io.kubemq.sdk.tools.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.piomin.service.kubemq.OrderController;
import pl.piomin.service.kubemq.model.Order;
import pl.piomin.service.kubemq.model.OrderStatus;
import pl.piomin.service.kubemq.service.OrderProcessor;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLException;
import java.io.IOException;

@Component
public class OrderListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    private Queue queue;
    private OrderProcessor orderProcessor;

    public OrderListener(Queue queue, OrderProcessor orderProcessor) {
        this.queue = queue;
        this.orderProcessor = orderProcessor;
    }

    @PostConstruct
    public void listen() throws ServerAddressNotSuppliedException, IOException, ClassNotFoundException {
        while (true) {
            Transaction transaction = queue.CreateTransaction();
            TransactionMessagesResponse response = transaction.Receive(10, 10);
            Order order = orderProcessor.process((Order) Converter.FromByteArray(response.getMessage().getBody()));
            if (order.getStatus().equals(OrderStatus.CONFIRMED)) {
                transaction.AckMessage();
            } else {
                transaction.RejectMessage();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
        }
    }

}
