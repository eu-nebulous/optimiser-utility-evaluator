package eu.nebulous.utilityevaluator.communication.exnconnector;

import eu.nebulous.utilityevaluator.UtilityEvaluatorController;
import eu.nebulous.utilityevaluator.model.Application;
import eu.nebulouscloud.exn.core.Context;
import eu.nebulouscloud.exn.core.Handler;
import eu.nebulouscloud.exn.core.Publisher;
import java.util.Map;

import org.apache.qpid.protonj2.client.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;


@Slf4j
//@Component
public class DslGenericMessageHandler extends Handler {
// This class handles the dsl.generic message that contains all information needed for application deployment
// The example of this message is provided: TODO


    private UtilityEvaluatorController controller;
    private static final ObjectMapper mapper = new ObjectMapper();


    public DslGenericMessageHandler(Context exnContext, Publisher performanceIndicatorPublisher){
        super();
        this.controller = new UtilityEvaluatorController(exnContext, performanceIndicatorPublisher);
    }

    @Override
    public void onMessage(String key, String address, Map body, Message message, Context context) {
        log.info("Received by custom handler {} => {} = {}", key,address,String.valueOf(body));
        log.info("Body={}", body.toString());

        //ObjectMapper objectMapper = new ObjectMapper();
        //GenericDSLMessage genericDSLMessage = objectMapper.readValue(body.toString(), GenericDSLMessage.class);
        //Application appFromMessage = new Application(genericDSLMessage);

        JsonNode appMessage = mapper.valueToTree(body);
        Application app;
	try {
	    app = new Application(appMessage);
	} catch (JsonProcessingException e) {
            log.error("Could not read app creation message", e);
            return;
	}
        log.info("Application {}, with name {}, has variables: {}", app.getApplicationId(), app.getApplicationName(), app.getVariables().toString());
        app = controller.createInitialCostPerformanceIndicators(app);
    }

        
      
}
