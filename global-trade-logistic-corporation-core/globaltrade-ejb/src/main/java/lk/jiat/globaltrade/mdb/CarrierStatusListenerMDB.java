package lk.jiat.globaltrade.mdb;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSConnectionFactoryDefinition;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import java.util.logging.Logger;

@JMSDestinationDefinition(
    name = "jms/queue/CarrierStatusQueue",
    interfaceName = "jakarta.jms.Queue",
    destinationName = "CarrierStatusQueue"
)
@JMSConnectionFactoryDefinition(
    name = "jms/queue/CarrierStatusConnectionFactory",
    interfaceName = "jakarta.jms.ConnectionFactory"
)
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/queue/CarrierStatusQueue"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
public class CarrierStatusListenerMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(CarrierStatusListenerMDB.class.getName());

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String text = ((TextMessage) message).getText();
                LOGGER.info("[EJB MDB RECEIVED CARRIER UPDATE] JMS Queue Message: " + text);
            } else {
                LOGGER.info("[EJB MDB RECEIVED CARRIER UPDATE] Non-text message received.");
            }
        } catch (Exception e) {
            LOGGER.severe("MDB message processing error: " + e.getMessage());
        }
    }
}
