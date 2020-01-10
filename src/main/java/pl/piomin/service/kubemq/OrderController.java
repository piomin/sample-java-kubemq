package pl.piomin.service.kubemq;

import io.kubemq.sdk.basic.ServerAddressNotSuppliedException;
import io.kubemq.sdk.queue.Message;
import io.kubemq.sdk.queue.Queue;
import io.kubemq.sdk.queue.SendMessageResult;
import io.kubemq.sdk.queue.Transaction;
import io.kubemq.sdk.tools.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.piomin.service.kubemq.model.Order;
import pl.piomin.service.kubemq.model.OrderStatus;

import java.io.IOException;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    private Queue queue;

    public OrderController(Queue queue) {
        this.queue = queue;
    }

    @PostMapping
    public Order sendOrder(@RequestBody Order order) {
        try {
            final SendMessageResult result = queue.SendQueueMessage(new Message()
                    .setBody(Converter.ToByteArray(order)));
            order.setId(result.getMessageID());
            order.setStatus(OrderStatus.IN_PROGRESS);
        } catch (ServerAddressNotSuppliedException | IOException e) {
            order.setStatus(OrderStatus.ERROR);
        }
        return order;
    }

}
