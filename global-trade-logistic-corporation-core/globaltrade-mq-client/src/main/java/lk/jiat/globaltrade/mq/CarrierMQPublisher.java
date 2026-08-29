package lk.jiat.globaltrade.mq;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import java.util.Random;
import java.util.logging.Logger;

public class CarrierMQPublisher {

    private static final Logger LOGGER = Logger.getLogger(CarrierMQPublisher.class.getName());
    private static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";
    private static final String QUEUE_NAME = "jms/queue/CarrierStatusQueue";

    public static void main(String[] args) {
        String brokerUrl = (args.length > 0) ? args[0] : DEFAULT_BROKER_URL;
        int messageCount = (args.length > 1) ? Integer.parseInt(args[1]) : 1;

        LOGGER.info("[ACTIVE MQ CLIENT] Connecting to JMS Broker at " + brokerUrl + " (Target Messages: " + messageCount + ")");

        String[] carriers = {"Maersk Line A/S", "Ocean Network Express (ONE)", "DHL Global Forwarding", "MSC Mediterranean Shipping"};
        String[] locations = {"Port of Los Angeles (USA)", "Port of Rotterdam (Netherlands)", "Singapore Deepwater Terminal", "Port of Colombo (Sri Lanka)"};
        String[] statuses = {"IN_TRANSIT", "ARRIVED_PORT", "CUSTOMS_INSPECTION", "DELIVERED"};
        Random random = new Random();

        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
            Connection connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(QUEUE_NAME);
            MessageProducer producer = session.createProducer(destination);

            long startTime = System.currentTimeMillis();
            for (int i = 1; i <= messageCount; i++) {
                String trackingNum = "GTL-2026-" + (1000 + random.nextInt(8999));
                String carrier = carriers[random.nextInt(carriers.length)];
                String status = statuses[random.nextInt(statuses.length)];
                String location = locations[random.nextInt(locations.length)];

                String payload = String.format(
                    "{\"trackingNumber\": \"%s\", \"carrier\": \"%s\", \"status\": \"%s\", \"location\": \"%s\", \"timestamp\": %d}",
                    trackingNum, carrier, status, location, System.currentTimeMillis()
                );

                TextMessage message = session.createTextMessage(payload);
                producer.send(message);

                if (i % 100 == 0 || i == messageCount) {
                    LOGGER.info(String.format("[ACTIVE MQ CLIENT] Sent %d / %d JMS telemetry updates to MDB Queue: %s", i, messageCount, payload));
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            double msgsPerSec = totalTime > 0 ? (messageCount / (totalTime / 1000.0)) : messageCount;
            LOGGER.info(String.format("[ACTIVE MQ SUCCESS] Completed sending %d messages in %d ms (%.2f msgs/sec)",
                messageCount, totalTime, msgsPerSec));

            session.close();
            connection.close();
        } catch (Exception e) {
            LOGGER.severe("[ACTIVE MQ CLIENT ERROR] Could not publish JMS message: " + e.getMessage());
        }
    }
}
