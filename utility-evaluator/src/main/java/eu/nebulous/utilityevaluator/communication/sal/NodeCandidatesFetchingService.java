package eu.nebulous.utilityevaluator.communication.sal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.nebulous.utilityevaluator.communication.exnconnector.ExnConnector;
import eu.nebulous.utilityevaluator.external.KubevelaAnalyzer;
import eu.nebulous.utilityevaluator.external.sal.NodeCandidate;
import eu.nebulous.utilityevaluator.external.sal.Requirement;
import eu.nebulous.utilityevaluator.model.Application;
import eu.nebulouscloud.exn.core.Context;
import eu.nebulouscloud.exn.core.SyncedPublisher;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; 


@Slf4j
@RequiredArgsConstructor
//@Component
public class NodeCandidatesFetchingService {

    private static final int NUMBER_OF_REPEATS_FOR_NODE_CANDIDATES = 120;
    private static final int DELAY_BETWEEN_REQUESTS = 5000;

    @NonNull
    private Context exnContext;
    private static final ObjectMapper mapper = new ObjectMapper();

    private long nRequest = 0;   // Used to generate unique SyncedPublisher key

    //https://gitlab.ow2.org/melodic/melodic-upperware/-/tree/morphemic-rc4.0/cp_generator/src/main/java/eu/paasage/upperware/profiler/generator/communication/impl

    public List<NodeCandidate> getNodeCandidatesViaMiddleware(Application app, String componentId){
        //generate requirements (based on kubevela), and providers, call SAL
        //via EXN Middleware get node candidates

        Map<String, List<Requirement>> requirements = KubevelaAnalyzer.getBoundedRequirements(app.getKubevela());
        // rudi, 2024-09-25: Note that we send an empty requirements list, so
        // we get all known node candidates, not only the ones required by
        // component `componentId`.
        Map<String, Object> message = Map.of("metaData", Map.of("user", "admin"), "body", "[]");
        SyncedPublisher nodeCandidatesConnector = new SyncedPublisher(
            "getNodeCandidates" + nRequest++,  ExnConnector.getNodeCandidatesTopic(), true, true);
        try {
            exnContext.registerPublisher(nodeCandidatesConnector);
            Map<String, Object> response = nodeCandidatesConnector.sendSync(message, app.getApplicationId(), null, false);
            log.info("Received a response");
            JsonNode payload = extractPayloadFromExnResponse(response, app.getApplicationId(), "getNodeCandidates");
            if (payload.isMissingNode()) {
                log.error("Got invalid SAL response for component {}, continuing with empty node candidate list", componentId);
                return List.of();
            } else {
                log.info("Correctly return SAL response for component {}, payload: {}", componentId, payload.asText());
                return Arrays.asList(mapper.convertValue(payload, NodeCandidate[].class));
            }
        } finally {
            exnContext.unregisterPublisher(nodeCandidatesConnector.key());
        }
    }

    //copied from Optimizer Controller: https://opendev.org/nebulous/optimiser-controller/src/branch/master/optimiser-controller/src/main/java/eu/nebulouscloud/optimiser/controller/NebulousApp.java
    private static JsonNode extractPayloadFromExnResponse(Map<String, Object> responseMessage, String appID, String caller) {
        JsonNode response = mapper.valueToTree(responseMessage);
        String salRawResponse = response.at("/body").asText(); // it's already a string, asText() is for the type system
        JsonNode metadata = response.at("/metaData");
        JsonNode salResponse = mapper.missingNode(); // the data coming from SAL
	    try {
	        salResponse = mapper.readTree(salRawResponse);
	    } 
        catch (JsonProcessingException e) {
            log.error("Could not read message body as JSON: body = '{}', for app: {}", salRawResponse, appID, e);
            return mapper.missingNode();
	    }
        if (!metadata.at("/status").asText().startsWith("2")) {
            // we only accept 200, 202, numbers of that nature
            log.error("exn-middleware-sal request failed with error code '{}' and message '{}'", metadata.at("/status"), salResponse.at("/message").asText());
            return mapper.missingNode();
        }
        return salResponse;
    }
    

    //old method used to connect to SAL directly
    /*private List<NodeCandidate> findNodeCandidates(List<Requirement> requirements) {
        List<NodeCandidate> nodeCandidates = new LinkedList<>();
        boolean isAnyAsyncNodeCandidatesProcessesInProgress = true;
        int requestNo = 0;
        try {
            while (isAnyAsyncNodeCandidatesProcessesInProgress && (requestNo < NUMBER_OF_REPEATS_FOR_NODE_CANDIDATES)) {
                log.info("Checking if nodeCandidates downlaod process is finished. Trye: {}", requestNo);
                //isAnyAsyncNodeCandidatesProcessesInProgress = proactiveClientConnectorService.isAnyAsyncNodeCandidatesProcessesInProgress();
                Thread.sleep(DELAY_BETWEEN_REQUESTS);
                requestNo++;
            }
            if (isAnyAsyncNodeCandidatesProcessesInProgress) {
                throw new RuntimeException("NodeCandidates are not yet present inside proactive scheduler");
            }
            //nodeCandidates = proactiveClientConnectorService.fetchNodeCandidates(requirements);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        } catch (ProactiveClientException e2) {
            log.error("Error message body: {}", e2.getMessage());
        }
        return nodeCandidates; 
    }*/





    
}
