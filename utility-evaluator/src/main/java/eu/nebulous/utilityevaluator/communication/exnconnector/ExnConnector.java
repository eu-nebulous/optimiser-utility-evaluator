package eu.nebulous.utilityevaluator.communication.exnconnector;

import java.util.List;


import org.springframework.stereotype.Component;

import eu.nebulouscloud.exn.Connector;
import eu.nebulouscloud.exn.core.Consumer;
import eu.nebulouscloud.exn.handlers.ConnectorHandler;
import eu.nebulouscloud.exn.settings.StaticExnConfig;
import lombok.Getter;
import eu.nebulouscloud.exn.core.Context;
import eu.nebulouscloud.exn.core.Publisher;
import eu.nebulouscloud.exn.core.SyncedPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class ExnConnector {

    private static final String GENERAL_APP_CREATION_MESSAGE_TOPIC = "eu.nebulouscloud.ui.dsl.generic.>";
    private static final String PERFOMANCE_INDICATORS_TOPIC = "eu.nebulouscloud.optimiser.utilityevaluator.performanceindicators";
    private static final String GET_NODE_CANDIDATES_TOPIC= "eu.nebulouscloud.exn.sal.nodecandidate.get";
    
    private ConnectionProperties properties;

    private final DslGenericMessageHandler genericDSLHandler;
    @Getter 
    private final Publisher performanceIndicatorPublisher;
    @Getter
    private final SyncedPublisher nodeCandidatesGetter;

    public ExnConnector(ConnectionProperties properties) {
        super();
        this.performanceIndicatorPublisher = new Publisher("costPerformanceIndicators", PERFOMANCE_INDICATORS_TOPIC, true, true);
        this.nodeCandidatesGetter = new SyncedPublisher("getNodeCandidates",  GET_NODE_CANDIDATES_TOPIC, true, true);
        this.genericDSLHandler = new DslGenericMessageHandler(nodeCandidatesGetter, performanceIndicatorPublisher);
        this.properties = properties;
        init();

    }
    private void init() {
        try {
            Connector c = new Connector(
                    "utilityevaluator",
                    new MyConnectorHandler(),
                    List.of(performanceIndicatorPublisher, nodeCandidatesGetter),
                    List.of(new Consumer("ui_generic_message", GENERAL_APP_CREATION_MESSAGE_TOPIC, genericDSLHandler ,true,true)),
                    false,
                    false,
                    new StaticExnConfig(
                            properties.getBrokerUrl(),
                            properties.getBrokerPort(),
                            properties.getBrokerUsername(),
                            properties.getBrokerPassword()
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