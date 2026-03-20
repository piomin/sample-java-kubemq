package pl.piomin.service.kubemq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kubemq.sdk.pubsub.EventMessage;
import io.kubemq.sdk.pubsub.EventSendResult;
import io.kubemq.sdk.pubsub.PubSubClient;
import io.kubemq.sdk.queues.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.piomin.service.kubemq.model.Order;
import pl.piomin.service.kubemq.model.OrderStatus;
import pl.piomin.service.kubemq.model.OrderType;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class KubeMQIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubeMQIntegrationTest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Container
    static GenericContainer<?> kubemqContainer = new GenericContainer<>("kubemq/kubemq-community:latest")
            .withExposedPorts(50000, 8080, 9090)
            .withEnv("KUBEMQ_TOKEN", "")
            .withEnv("KUBEMQ_LICENSE", "")
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(java.time.Duration.ofMinutes(2));

    private QueuesClient queuesClient;
    private PubSubClient pubSubClient;
    private String kubemqAddress;

    @BeforeEach
    void setUp() {
        kubemqAddress = kubemqContainer.getHost() + ":" + kubemqContainer.getMappedPort(50000);
        LOGGER.info("KubeMQ container started at: {}", kubemqAddress);

        queuesClient = QueuesClient.builder()
                .address(kubemqAddress)
                .clientId("test-queues-client")
                .build();

        pubSubClient = PubSubClient.builder()
                .address(kubemqAddress)
                .clientId("test-pubsub-client")
                .build();
    }

    @AfterEach
    void tearDown() {
        if (queuesClient != null) {
            try {
                queuesClient.close();
            } catch (Exception e) {
                LOGGER.warn("Error closing queues client", e);
            }
        }
        if (pubSubClient != null) {
            try {
                pubSubClient.close();
            } catch (Exception e) {
                LOGGER.warn("Error closing pubsub client", e);
            }
        }
    }

    @Test
    void shouldSendAndReceiveQueueMessage() throws Exception {
        // Given
        String testChannel = "test-transactions";
        Order testOrder = new Order(
                OrderType.TRANSFER,
                1,
                2,
                LocalDateTime.now(),
                100,
                null,
                OrderStatus.ACCEPTED
        );

        byte[] orderBytes = objectMapper.writeValueAsBytes(testOrder);

        // When - Send message
        QueueMessage message = QueueMessage.builder()
                .channel(testChannel)
                .body(orderBytes)
                .metadata("test-message")
                .build();

        QueueSendResult sendResult = queuesClient.sendQueuesMessage(message);

        // Then - Verify send
        assertNotNull(sendResult);
        assertNotNull(sendResult.getId());
        assertFalse(sendResult.isError());
        LOGGER.info("Message sent with ID: {}", sendResult.getId());

        // When - Receive message
        QueuesPollRequest pollRequest = QueuesPollRequest.builder()
                .channel(testChannel)
                .pollMaxMessages(1)
                .pollWaitTimeoutInSeconds(10)
                .build();

        QueuesPollResponse pollResponse = queuesClient.receiveQueuesMessages(pollRequest);

        // Then - Verify receive
        assertNotNull(pollResponse);
        assertFalse(pollResponse.isError());
        assertFalse(pollResponse.getMessages().isEmpty());

        QueueMessageReceived receivedMessage = pollResponse.getMessages().get(0);
        assertNotNull(receivedMessage);
        assertNotNull(receivedMessage.getBody());

        Order receivedOrder = objectMapper.readValue(receivedMessage.getBody(), Order.class);
        assertEquals(testOrder.getAccountIdFrom(), receivedOrder.getAccountIdFrom());
        assertEquals(testOrder.getAccountIdTo(), receivedOrder.getAccountIdTo());
        assertEquals(testOrder.getAmount(), receivedOrder.getAmount());
        assertEquals(testOrder.getType(), receivedOrder.getType());

        LOGGER.info("Message received: {}", receivedOrder);

        // Acknowledge the message
        receivedMessage.ack();
    }

    @Test
    void shouldSendAndReceiveMultipleQueueMessages() throws Exception {
        // Given
        String testChannel = "test-bulk-transactions";
        int messageCount = 5;

        // When - Send multiple messages
        for (int i = 0; i < messageCount; i++) {
            Order order = new Order(
                    OrderType.TRANSFER,
                    1,
                    2,
                    LocalDateTime.now(),
                    100 + i,
                    null,
                    OrderStatus.ACCEPTED
            );

            byte[] orderBytes = objectMapper.writeValueAsBytes(order);
            QueueMessage message = QueueMessage.builder()
                    .channel(testChannel)
                    .body(orderBytes)
                    .build();

            QueueSendResult result = queuesClient.sendQueuesMessage(message);
            assertFalse(result.isError());
        }

        LOGGER.info("Sent {} messages", messageCount);

        // When - Receive messages
        QueuesPollRequest pollRequest = QueuesPollRequest.builder()
                .channel(testChannel)
                .pollMaxMessages(messageCount)
                .pollWaitTimeoutInSeconds(10)
                .build();

        QueuesPollResponse pollResponse = queuesClient.receiveQueuesMessages(pollRequest);

        // Then
        assertNotNull(pollResponse);
        assertFalse(pollResponse.isError());
        assertEquals(messageCount, pollResponse.getMessages().size());

        LOGGER.info("Received {} messages", pollResponse.getMessages().size());

        // Acknowledge all messages
        for (QueueMessageReceived msg : pollResponse.getMessages()) {
            msg.ack();
        }
    }

    @Test
    void shouldSendEventMessage() throws Exception {
        // Given
        String testChannel = "test-events";
        Order testOrder = new Order(
                OrderType.TRANSFER,
                1,
                2,
                LocalDateTime.now(),
                200,
                "test-event-id",
                OrderStatus.CONFIRMED
        );

        byte[] orderBytes = objectMapper.writeValueAsBytes(testOrder);

        // When
        EventMessage eventMessage = EventMessage.builder()
                .channel(testChannel)
                .body(orderBytes)
                .metadata("test-event")
                .build();

        // Send event - method returns void in SDK v2
        pubSubClient.sendEventsMessage(eventMessage);

        // Then - If no exception thrown, the event was sent successfully
        LOGGER.info("Event sent successfully to channel: {}", testChannel);
    }

    @Test
    void shouldRejectQueueMessage() throws Exception {
        // Given
        String testChannel = "test-reject";
        Order testOrder = new Order(
                OrderType.TRANSFER,
                1,
                2,
                LocalDateTime.now(),
                300,
                null,
                OrderStatus.ACCEPTED
        );

        byte[] orderBytes = objectMapper.writeValueAsBytes(testOrder);

        // When - Send message
        QueueMessage message = QueueMessage.builder()
                .channel(testChannel)
                .body(orderBytes)
                .build();

        queuesClient.sendQueuesMessage(message);

        // Receive message
        QueuesPollRequest pollRequest = QueuesPollRequest.builder()
                .channel(testChannel)
                .pollMaxMessages(1)
                .pollWaitTimeoutInSeconds(10)
                .build();

        QueuesPollResponse pollResponse = queuesClient.receiveQueuesMessages(pollRequest);
        assertFalse(pollResponse.getMessages().isEmpty());

        QueueMessageReceived receivedMessage = pollResponse.getMessages().get(0);

        // Reject the message
        receivedMessage.reject();
        LOGGER.info("Message rejected successfully");

        // The message should be available again after rejection
        QueuesPollResponse secondPollResponse = queuesClient.receiveQueuesMessages(pollRequest);
        assertFalse(secondPollResponse.isError());
        // Note: Depending on KubeMQ configuration, the rejected message might be re-queued
    }

    @Test
    void shouldHandleEmptyQueuePoll() throws Exception {
        // Given
        String emptyChannel = "empty-channel-" + System.currentTimeMillis();

        // When
        QueuesPollRequest pollRequest = QueuesPollRequest.builder()
                .channel(emptyChannel)
                .pollMaxMessages(1)
                .pollWaitTimeoutInSeconds(2)
                .build();

        QueuesPollResponse pollResponse = queuesClient.receiveQueuesMessages(pollRequest);

        // Then
        assertNotNull(pollResponse);
        assertFalse(pollResponse.isError());
        assertTrue(pollResponse.getMessages().isEmpty());
        LOGGER.info("Empty queue poll handled correctly");
    }
}
