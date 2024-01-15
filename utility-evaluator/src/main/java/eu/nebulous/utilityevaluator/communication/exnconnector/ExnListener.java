package eu.nebulous.utilityevaluator.communication.exnconnector;

import eu.nebulouscloud.exn.Connector;
import eu.nebulouscloud.exn.core.Consumer;
import eu.nebulouscloud.exn.core.Context;
import eu.nebulouscloud.exn.core.Handler;
import eu.nebulouscloud.exn.handlers.ConnectorHandler;
import eu.nebulouscloud.exn.settings.StaticExnConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.apache.qpid.protonj2.client.Message;

import java.util.List;
import java.util.Map;

@Component
public class ExnListener extends Handler {

    private static final Logger log = LoggerFactory.getLogger(ExnListener.class);
    private String topicName;
    private String address;
    private Connector connector;

    public ExnListener(String topicName, String address, String brokerHost, int brokerPort, String username, String password) {
        this.topicName = topicName;
        this.address = address;

        ConnectorHandler connectorHandler = new ConnectorHandler() {
            @Override
            public void onReady(Context context) {
                log.info("Connector ready. Registering consumer for topic: {}", topicName);
                // Register this handler as a consumer for the specified topic and address
                context.registerConsumer(new Consumer(topicName, address, ExnListener.this, true));
            }
        };

        // Initialize the connector with the connector handler and configuration
        this.connector = new Connector("ui", connectorHandler, List.of(), List.of(), false, false,
                new StaticExnConfig(brokerHost, brokerPort, username, password));
    }

    @Override
    public void onMessage(String key, String address, Map body, Message message, Context context) {
        log.info("Received message on topic {}: key={}, address={}, body={}", topicName, key, address, body);
        // Implement custom message processing logic here
        processMessage(key, address, body, message, context);
    }

    public void start() {
        try {
            connector.start();
            log.info("ExnListener started for topic: {}", topicName);
        } catch (Exception e) {
            log.error("Error starting ExnListener: {}", e.getMessage());
        }
    }

    public void stop() {
        try {
            connector.stop();
            log.info("ExnListener stopped for topic: {}", topicName);
        } catch (Exception e) {
            log.error("Error stopping ExnListener: {}", e.getMessage());
        }
    }

    protected void processMessage(String key, String address, Map body, Message message, Context context) {
        // This method can be overridden in subclasses for custom message processing
    }
}