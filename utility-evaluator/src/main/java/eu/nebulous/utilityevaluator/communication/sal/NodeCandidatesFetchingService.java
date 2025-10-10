package eu.nebulous.utilityevaluator.communication.sal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.nebulous.utilityevaluator.communication.exnconnector.ExnConnector;
import eu.nebulous.utilityevaluator.external.KubevelaAnalyzer;
import eu.nebulous.utilityevaluator.external.sal.AttributeRequirement;
import eu.nebulous.utilityevaluator.external.sal.NodeCandidate;
import eu.nebulous.utilityevaluator.external.sal.NodeType;
import eu.nebulous.utilityevaluator.external.sal.NodeTypeRequirement;
import eu.nebulous.utilityevaluator.external.sal.Requirement;
import eu.nebulous.utilityevaluator.external.sal.RequirementOperator;
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

    /**
     * Ask the CFSB for a list of node candidates for the application.  Only
     * ask for node candidates that are available in the clouds and regions
     * that are specified in the dsl message.
     */
    public List<NodeCandidate> getNodeCandidatesViaBroker(Application app, String componentId){
        // TODO: do we do this once per component, and if so, should we check
        // whether the component is edge-only or cloud-only?
        List<List<Requirement>> requirements = new ArrayList<>();
        // organization-wide edge nodes
        String orgWideName = "application_id|all-applications|";
        requirements.add(List.of(
            new NodeTypeRequirement(List.of(NodeType.EDGE), "", ""),
            new AttributeRequirement("hardware", "name", RequirementOperator.INC, orgWideName)));
        // per-app edge nodes
        String appAssignedName = "application_id|" + app.getApplicationId() + "|";
        requirements.add(List.of(
            new NodeTypeRequirement(List.of(NodeType.EDGE), "", ""),
            new AttributeRequirement("hardware", "name", RequirementOperator.INC, appAssignedName)));
        // one requirement list per active cloud
        app.getClouds().forEach((id, regions) -> {
            List<Requirement> cloud_reqs = new ArrayList<>();
            cloud_reqs.add(new NodeTypeRequirement(List.of(NodeType.IAAS), "", ""));
            cloud_reqs.add(new AttributeRequirement("cloud", "id", RequirementOperator.EQ, id));
            if (!regions.isEmpty()) {
                cloud_reqs.add(new AttributeRequirement("location", "name", RequirementOperator.IN, String.join(" ", regions)));
            }
            requirements.add(cloud_reqs);
        });

        final Map<String, Object> message;
        try {
	    message = Map.of(
		"metaData", Map.of("user", "admin"),
		"body", mapper.writeValueAsString(requirements));
	} catch (JsonProcessingException e) {
            log.error("Could not convert requirements list to JSON string (this should never happen); failed to get node candidates",
                e);
            return List.of();
	}

        SyncedPublisher nodeCandidatesConnector = new SyncedPublisher(
            "getNodeCandidatesMultiple" + nRequest++,  ExnConnector.getNodeCandidatesMultipleTopic(),
            true, true,60*1000);
        try {
            exnContext.registerPublisher(nodeCandidatesConnector);
            Map<String, Object> response = nodeCandidatesConnector.sendSync(message, app.getApplicationId(),
                null, false);
            log.info("Received a response");
            // Note: we do not call extractPayloadFromExnResponse here, since this
            // response does not come from the exn-middleware, so will not be
            // packaged into a string.
            ObjectNode jsonBody = mapper.convertValue(response, ObjectNode.class);
            log.info("Correctly return CFSB response for component {}, payload: {}", componentId, jsonBody.asText());
            List<JsonNode> result = Arrays.asList(mapper.convertValue(jsonBody.withArray("/body", JsonNode.OverwriteMode.ALL, true), JsonNode[].class));
            // Strip the CFSB ranking attributes "score", "rank" so that the
            // result entries convert cleanly into NodeCandidate instances
            return result.stream()
                .map(candidate ->
                    mapper.convertValue(
                        ((ObjectNode)candidate).deepCopy().remove(List.of("score", "rank")),
                        NodeCandidate.class))
                .collect(Collectors.toList());
        } finally {
            exnContext.unregisterPublisher(nodeCandidatesConnector.key());
        }
    }


    public List<NodeCandidate> getNodeCandidatesViaMiddleware(Application app, String componentId){
        //generate requirements (based on kubevela), and providers, call SAL
        //via EXN Middleware get node candidates

        Map<String, List<Requirement>> requirements = KubevelaAnalyzer.getBoundedRequirements(app.getKubevela());
        // rudi, 2024-09-25: Note that we send an empty requirements list, so
        // we get all known node candidates, not only the ones required by
        // component `componentId`.
        Map<String, Object> message = Map.of("metaData", Map.of("user", "admin"), "body", "[]");
        SyncedPublisher nodeCandidatesConnector = new SyncedPublisher(
            "getNodeCandidates" + nRequest++,  ExnConnector.getNodeCandidatesTopic(), true, true,60*1000);
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
