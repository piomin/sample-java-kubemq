package pl.piomin.service.kubemq.listener;

import javax.annotation.PostConstruct;

import io.kubemq.sdk.event.Channel;
import io.kubemq.sdk.event.Event;
import io.kubemq.sdk.event.Result;
import io.kubemq.sdk.queue.Queue;
import io.kubemq.sdk.queue.Transaction;
import io.kubemq.sdk.queue.TransactionMessagesResponse;
import io.kubemq.sdk.tools.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.piomin.service.kubemq.model.Order;
import pl.piomin.service.kubemq.model.OrderStatus;
import pl.piomin.service.kubemq.service.OrderProcessor;

import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class OrderListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderListener.class);

	private Queue queue;
	private Channel channel;
	private OrderProcessor orderProcessor;
	private TaskExecutor taskExecutor;

	public OrderListener(Queue queue, Channel channel, OrderProcessor orderProcessor, TaskExecutor taskExecutor) {
		this.queue = queue;
		this.channel = channel;
		this.orderProcessor = orderProcessor;
		this.taskExecutor = taskExecutor;
	}

	@PostConstruct
	public void listen() {
		taskExecutor.execute(() -> {
			while (true) {
			    try {
                    Transaction transaction = queue.CreateTransaction();
                    TransactionMessagesResponse response = transaction.Receive(10, 10);
                    if (response.getMessage().getBody().length > 0) {
                        Order order = orderProcessor
                                .process((Order) Converter.FromByteArray(response.getMessage().getBody()));
                        LOGGER.info("Processed: {}", order);
                        if (order.getStatus().equals(OrderStatus.CONFIRMED)) {
                            transaction.AckMessage();
                            Event event = new Event();
                            event.setEventId(response.getMessage().getMessageID());
                            event.setBody(Converter.ToByteArray(order));
							LOGGER.info("Sending event: id={}", event.getEventId());
                            channel.SendEvent(event);
                        } else {
                            transaction.RejectMessage();
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
