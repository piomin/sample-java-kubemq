package pl.piomin.service.kubemq.listener;

import io.kubemq.sdk.pubsub.PubSubClient;
import io.kubemq.sdk.pubsub.EventMessage;
import io.kubemq.sdk.queues.QueuesClient;
import io.kubemq.sdk.queues.QueuesPollRequest;
import io.kubemq.sdk.queues.QueuesPollResponse;
import io.kubemq.sdk.queues.QueueMessageReceived;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import pl.piomin.service.kubemq.model.Order;
import pl.piomin.service.kubemq.model.OrderStatus;
import pl.piomin.service.kubemq.service.OrderProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderListener.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private QueuesClient queuesClient;
	private PubSubClient pubSubClient;
	private OrderProcessor orderProcessor;
	private TaskExecutor taskExecutor;

	public OrderListener(QueuesClient queuesClient, PubSubClient pubSubClient,
	                     OrderProcessor orderProcessor, TaskExecutor taskExecutor) {
		this.queuesClient = queuesClient;
		this.pubSubClient = pubSubClient;
		this.orderProcessor = orderProcessor;
		this.taskExecutor = taskExecutor;
	}

	@PostConstruct
	public void listen() {
		taskExecutor.execute(() -> {
			while (true) {
			    try {
                    QueuesPollRequest pollRequest = QueuesPollRequest.builder()
                            .channel("transactions")
                            .pollMaxMessages(1)
                            .pollWaitTimeoutInSeconds(10)
                            .build();

                    QueuesPollResponse response = queuesClient.receiveQueuesMessages(pollRequest);

                    if (response.isError()) {
                        LOGGER.error("Error receiving message: {}", response.getError());
                        Thread.sleep(10000);
                        continue;
                    }

                    if (!response.getMessages().isEmpty()) {
                        QueueMessageReceived message = response.getMessages().get(0);
                        Order order = objectMapper.readValue(message.getBody(), Order.class);
                        order = orderProcessor.process(order);
                        LOGGER.info("Processed: {}", order);

                        if (order.getStatus().equals(OrderStatus.CONFIRMED)) {
                            message.ack();

                            byte[] eventBody = objectMapper.writeValueAsBytes(order);
                            EventMessage event = EventMessage.builder()
                                    .channel("transactions")
                                    .body(eventBody)
                                    .id(message.getId())
                                    .build();

                            LOGGER.info("Sending event: id={}", message.getId());
                            pubSubClient.sendEventsMessage(event);
                        } else {
                            message.reject();
                        }
                    } else {
                        LOGGER.info("No messages");
                    }
                    Thread.sleep(10000);
                } catch (Exception e) {
					LOGGER.error("Error", e);
                }
			}
		});

	}

}
