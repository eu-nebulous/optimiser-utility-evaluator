package eu.nebulous.utilityevaluator.communication.exnconnector;

import eu.nebulous.utilityevaluator.UtilityEvaluatorController;
import eu.nebulouscloud.exn.core.Context;
import eu.nebulouscloud.exn.core.Handler;

import java.util.Map;

import org.apache.qpid.protonj2.client.Message;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class GeneralMessageHandler extends Handler {

    public final UtilityEvaluatorController controller;

    public GeneralMessageHandler(UtilityEvaluatorController controller){
        super();
        this.controller = controller;
    }

    @Override
    public void onMessage(String key, String address, Map body, Message message, Context context) {
        log.info("Received by custom handler {} => {} = {}", key,address,String.valueOf(body));
        log.info("Received message: {}", message.toString());
        log.info("Body={}", body.toString());
        JSONObject jsonObject = new JSONObject(body);
        Map<String, Object> map = jsonObject.toMap();
        try {
            String applicationId = message.subject();
            //todo: transform the old code to get it in the right format
        } catch (ClientException e) {
            
            e.printStackTrace();
            log.error(e.getMessage());
        }
        

        /*FetchNodeCandidatesMessage clearedMessage = new FetchNodeCandidatesMessage(message);
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

        }*/

    }
    
}
