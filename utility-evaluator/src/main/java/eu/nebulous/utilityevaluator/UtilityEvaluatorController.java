package eu.nebulous.utilityevaluator;

import org.ow2.proactive.sal.model.NodeCandidate;
import java.util.List;

import org.springframework.stereotype.Component;

import eu.nebulous.utilityevaluator.communication.exnconnector.ExnConnector;
import eu.nebulous.utilityevaluator.communication.exnconnector.PerformanceIndicatorSendingService;
import eu.nebulous.utilityevaluator.communication.sal.NodeCandidatesFetchingService;
import eu.nebulous.utilityevaluator.converter.NodeCandidateConverter;
import eu.nebulous.utilityevaluator.model.Application;
import eu.nebulous.utilityevaluator.model.NodeCandidateDTO;
import eu.nebulous.utilityevaluator.model.VariableDTO;
import eu.nebulous.utilityevaluator.regression.SimpleCostRegression;
import eu.nebulouscloud.exn.core.Publisher;
import eu.nebulouscloud.exn.core.SyncedPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/* The main controlling component. It coordinates the work TODO*/
@Slf4j
//@Component
public class UtilityEvaluatorController {

    private NodeCandidatesFetchingService nodeCandidatesService;
    private PerformanceIndicatorSendingService performanceIndicatorSendingService;

    public UtilityEvaluatorController(SyncedPublisher nodeCandidatesGetter, Publisher performanceIndicatorPublisher){
        this.nodeCandidatesService = new NodeCandidatesFetchingService(nodeCandidatesGetter);
        this.performanceIndicatorSendingService = new PerformanceIndicatorSendingService(performanceIndicatorPublisher);
    }

    public Application createInitialCostPerformanceIndicators(Application application){
        /*
         * for each component of the application (that has variables), it should:
         *  
         *  convert them to DTO
         *  for types variables that are there, create a list of arguments to the regression
         *  create regression object
         *  save it back in the application
         *  send the parameters via ActiveMQ (maybe in the handler?)
         */
        for (String component : application.getVariables().keySet()){

            List<NodeCandidate> nodeCandidates = nodeCandidatesService.getNodeCandidatesViaMiddleware(application, component);
            log.info("Number of Node Candidates: {}", nodeCandidates.size());
            if (nodeCandidates.isEmpty()){
                log.error("SAL returned empty list, it is not possible to create cost performance indicator");
                continue;
            }
            List<NodeCandidateDTO> convertedNodeCandidates = NodeCandidateConverter.convertToDtoList(nodeCandidates);
            List<VariableDTO> componentVariables = application.getVariables().get(component);
            SimpleCostRegression regression = new SimpleCostRegression(component, convertedNodeCandidates, componentVariables);
            application.getCostPerformanceIndicators().put(component, regression);

        };
        
        log.info("Creating regression for cost performance indicators has been successfully finished");
        
        performanceIndicatorSendingService.sendPerformanceIndicators(application);
        log.info("Performance indicators sent");

        return application;
    }

}

