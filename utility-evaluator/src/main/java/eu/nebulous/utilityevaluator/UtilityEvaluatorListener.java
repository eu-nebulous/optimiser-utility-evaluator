package eu.nebulous.utilityevaluator;

import java.util.Optional;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import eu.nebulous.utilityevaluator.communication.activemq.message.FetchNodeCandidatesMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class UtilityEvaluatorListener {
// This is an old component that is going to be removed after tests

    private final UtilityEvaluatorController controller;

    //@JmsListener(destination = "eu.nebulouscloud.dsl.general")
    //@JmsListener(destination = "TestTopic")
    public void handleGeneralApplicationMessage(JSONObject message) {
        // Process the received message
        log.info("Received message: {}", message.toString());

        FetchNodeCandidatesMessage clearedMessage = new FetchNodeCandidatesMessage(message);
        log.info("Cleared message: {}", clearedMessage.toString());
        Optional<String> nodeCandidatesTensor = controller.createNodeCandidatesTensor(clearedMessage);
        if (nodeCandidatesTensor.isPresent()){
            log.info("Tensor successfully created");
            // If needed, you can also send a response back to another queue or topic
            //jmsTemplate.convertAndSend("eu.nebulouslcloud.optimizer.solver.tensor", new NodeCandidatesTensorMessage(clearedMessage.getApplicationID(), nodeCandidatesTensor.get()));
        log.info("Tensor was passed via ActiveMQ");
        }
        else {
            log.error("There was an error during creating the tensor");
        }
    }

}
