package eu.nebulous.utilityevaluator.communication.exnconnector;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.nebulouscloud.exn.Connector;
import eu.nebulouscloud.exn.core.Consumer;
import eu.nebulouscloud.exn.handlers.ConnectorHandler;
import eu.nebulouscloud.exn.settings.StaticExnConfig;

import eu.nebulouscloud.exn.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class ExnConnector {

    @Value("${spring.activemq.broker-url}")
    String BROKER_URL;
    @Value("${spring.activemq.broker-port}")
    Integer BROKER_PORT;
    @Value("${spring.activemq.user}")
    String BROKER_USERNAME;
    @Value("${spring.activemq.password}")
    String BROKER_PASSWORD;
 

    public static final String GENERAL_APP_CREATION_MESSAGE_TOPIC = "eu.nebulouscloud.ui.dsl.generic.>";
    public final GeneralMessageHandler generalHandler;

    public ExnConnector(GeneralMessageHandler handler) {
        super();
        this.generalHandler = handler;
        init();
        

    }
    private void init() {
        try {
            Connector c = new Connector(
                    "utilityevaluator",
                    new MyConnectorHandler(),
                    List.of(),
                    List.of(new Consumer("ui_all", GENERAL_APP_CREATION_MESSAGE_TOPIC, generalHandler ,true,true)),
                    false,
                    false,
                    new StaticExnConfig(
                            "localhost",
                            5672,
                            BROKER_USERNAME,
                            BROKER_PASSWORD
                    )
            );
            c.start();
            } catch (Exception e) {
            e.printStackTrace();
        }
    }
       
}

class MyConnectorHandler extends ConnectorHandler {

    Logger logger = LoggerFactory.getLogger(MyConnectorHandler.class);

    @Override
    public void onReady(Context context) {
        logger.info ("Ready start working");
        //context.registerConsumer(new Consumer("ue_health","health", generalHandler, true));


        /**
         * We can then de-register the consumer
         */
        new Thread(){
            @Override
            public void run() {

                try {
                    logger.debug("Waiting for 50 s to unregister consumer");
                    Thread.sleep(30000);
                    context.unregisterConsumer("ue_health");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();


    }
}