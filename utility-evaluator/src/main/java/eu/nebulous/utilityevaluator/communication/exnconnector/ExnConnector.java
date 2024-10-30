package eu.nebulous.utilityevaluator.communication.exnconnector;

import java.util.List;


import org.springframework.stereotype.Component;

import eu.nebulouscloud.exn.Connector;
import eu.nebulouscloud.exn.core.Consumer;
import eu.nebulouscloud.exn.handlers.ConnectorHandler;
import eu.nebulouscloud.exn.settings.StaticExnConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import eu.nebulouscloud.exn.core.Context;
import eu.nebulouscloud.exn.core.Publisher;


@Slf4j
@Component
public class ExnConnector {

    private static final String GENERAL_APP_CREATION_MESSAGE_TOPIC = "eu.nebulouscloud.ui.dsl.generic.>";
    private static final String PERFOMANCE_INDICATORS_TOPIC = "eu.nebulouscloud.optimiser.utilityevaluator.performanceindicators";
    @Getter
    private static final String nodeCandidatesTopic= "eu.nebulouscloud.exn.sal.nodecandidate.get";

    @Getter
    private final Publisher performanceIndicatorPublisher;

    public ExnConnector(ConnectionProperties properties) {
        this.performanceIndicatorPublisher = new Publisher("costPerformanceIndicators", PERFOMANCE_INDICATORS_TOPIC, true, true);
        // this.nodeCandidatesGetter = new SyncedPublisher("getNodeCandidates",  GET_NODE_CANDIDATES_TOPIC, true, true);
        Connector c = new Connector(
            "utilityevaluator",
            new ConnectorHandler() {
            public void onReady(Context context) {
                ExnConnector.log.info ("ExnConnector got context {}", context);
                DslGenericMessageHandler genericDSLHandler = new DslGenericMessageHandler(context, performanceIndicatorPublisher);
                context.registerConsumer(new Consumer("ui_generic_message", GENERAL_APP_CREATION_MESSAGE_TOPIC, genericDSLHandler ,true,true));
            }
            },
            List.of(performanceIndicatorPublisher),
            List.of(),
            false,
            false,
            new StaticExnConfig(
                properties.getBrokerUrl(),
                properties.getBrokerPort(),
                properties.getBrokerUsername(),
                properties.getBrokerPassword()
            ));
        c.start();
    }
}
