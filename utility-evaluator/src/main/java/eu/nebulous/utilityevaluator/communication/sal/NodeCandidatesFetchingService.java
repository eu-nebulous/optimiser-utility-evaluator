package eu.nebulous.utilityevaluator.communication.sal;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ow2.proactive.sal.model.NodeCandidate;
import org.ow2.proactive.sal.model.Requirement;
import org.springframework.stereotype.Component;

import eu.nebulous.utilityevaluator.communication.sal.error.ProactiveClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; 

@Slf4j
@RequiredArgsConstructor
@Component
public class NodeCandidatesFetchingService {

    private static final int NUMBER_OF_REPEATS_FOR_NODE_CANDIDATES = 120;
    private static final int DELAY_BETWEEN_REQUESTS = 5000;

    private final ProactiveConnector proactiveClientConnectorService;

    
    /*public NodeCandidatesFetchingService(ProactiveClientProperties properties){
        ProactiveConnector connector = new ProactiveConnector(properties);
        this.proactiveClientConnectorService = connector;
    }*/

        //https://gitlab.ow2.org/melodic/melodic-upperware/-/tree/morphemic-rc4.0/cp_generator/src/main/java/eu/paasage/upperware/profiler/generator/communication/impl 
    
    public List<NodeCandidate> getNodeCandidates(Map<String,String> cloudProviders){
        List<Requirement> providerRequirements = convertProviderRequirements(cloudProviders);
        return findNodeCandidates(providerRequirements);
    }
    
    private List<Requirement> convertProviderRequirements(Map<String,String> cloudProviders){
        //todo: filter based on the chosen cloud providers 
        return List.of();
    }

    private List<NodeCandidate> findNodeCandidates(List<Requirement> requirements) {
        List<NodeCandidate> nodeCandidates = new LinkedList<>();
        boolean isAnyAsyncNodeCandidatesProcessesInProgress = true;
        int requestNo = 0;
        try {
            while (isAnyAsyncNodeCandidatesProcessesInProgress && (requestNo < NUMBER_OF_REPEATS_FOR_NODE_CANDIDATES)) {
                log.info("Checking if nodeCandidates downlaod process is finished. Trye: {}", requestNo);
                isAnyAsyncNodeCandidatesProcessesInProgress = proactiveClientConnectorService.isAnyAsyncNodeCandidatesProcessesInProgress();
                Thread.sleep(DELAY_BETWEEN_REQUESTS);
                requestNo++;
            }
            if (isAnyAsyncNodeCandidatesProcessesInProgress) {
                throw new RuntimeException("NodeCandidates are not yet present inside proactive scheduler");
            }
            nodeCandidates = proactiveClientConnectorService.fetchNodeCandidates(requirements);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        } catch (ProactiveClientException e2) {
            log.error("Error message body: {}", e2.getMessage());
        }
        return nodeCandidates; 
    }





    
}
