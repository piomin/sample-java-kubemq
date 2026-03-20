package pl.piomin.service.kubemq.controller;

import io.kubemq.sdk.queues.QueuesClient;
import io.kubemq.sdk.queues.QueueMessage;
import io.kubemq.sdk.queues.QueueSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.piomin.service.kubemq.model.Order;
import pl.piomin.service.kubemq.model.OrderStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private QueuesClient queuesClient;

    public OrderController(QueuesClient queuesClient) {
        this.queuesClient = queuesClient;
    }

    @PostMapping
    public Order sendOrder(@RequestBody Order order) {
        try {
            LOGGER.info("Sending: {}", order);
            byte[] orderBytes = objectMapper.writeValueAsBytes(order);

            QueueMessage message = QueueMessage.builder()
                    .channel("transactions")
                    .body(orderBytes)
                    .build();

            QueueSendResult result = queuesClient.sendQueuesMessage(message);
            order.setId(result.getId());
            order.setStatus(OrderStatus.ACCEPTED);
            LOGGER.info("Sent: {}", order);
        } catch (Exception e) {
            LOGGER.error("Error sending", e);
            order.setStatus(OrderStatus.ERROR);
        }
        return order;
    }

}
