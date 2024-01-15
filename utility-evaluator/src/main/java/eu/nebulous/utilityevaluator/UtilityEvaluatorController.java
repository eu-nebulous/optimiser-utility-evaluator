package eu.nebulous.utilityevaluator;

import org.ow2.proactive.sal.model.NodeCandidate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import eu.nebulous.utilityevaluator.communication.activemq.message.FetchNodeCandidatesMessage;
import eu.nebulous.utilityevaluator.communication.sal.NodeCandidatesFetchingService;
import eu.nebulous.utilityevaluator.nodecandidates.NodeCandidateConverter;
import eu.nebulous.utilityevaluator.nodecandidates.NodeCandidateDTO;
import jline.internal.Log;
import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class UtilityEvaluatorController {

    private final NodeCandidatesFetchingService nodeCandidatesService;


//this is the main method of Utiliy Evaluator. It creates a .csv file with available Node Candidates
    public Optional<String> createNodeCandidatesTensor(FetchNodeCandidatesMessage message){

        Log.info("Creating Node Candidates tensor...");
        List<NodeCandidate> nodeCandidates = nodeCandidatesService.getNodeCandidates(message.getCloudProviders());

        //convert Node Candidates, possibly also filter (in the future)
        List<NodeCandidateDTO> convertedNodeCandidates = NodeCandidateConverter.convertToDtoList(nodeCandidates);
        String csv = NodeCandidateConverter.convertToCsv(convertedNodeCandidates);
    
        return Optional.of(csv);

    }
}

