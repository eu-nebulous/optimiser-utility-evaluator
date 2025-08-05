package eu.nebulous.utilityevaluator;

import java.util.List;

import eu.nebulous.utilityevaluator.communication.exnconnector.PerformanceIndicatorSendingService;
import eu.nebulous.utilityevaluator.communication.sal.NodeCandidatesFetchingService;
import eu.nebulous.utilityevaluator.converter.NodeCandidateConverter;
import eu.nebulous.utilityevaluator.external.sal.NodeCandidate;
import eu.nebulous.utilityevaluator.model.Application;
import eu.nebulous.utilityevaluator.model.NodeCandidateDTO;
import eu.nebulous.utilityevaluator.model.VariableDTO;
import eu.nebulous.utilityevaluator.model.VariableType;
import eu.nebulous.utilityevaluator.regression.SimpleCostRegression;
import eu.nebulouscloud.exn.core.Context;
import eu.nebulouscloud.exn.core.Publisher;
import lombok.extern.slf4j.Slf4j;

/* The main controlling component. It coordinates the process of creating the initial performance indicators*/
@Slf4j
//@Component
public class UtilityEvaluatorController {

    private NodeCandidatesFetchingService nodeCandidatesService;
    private PerformanceIndicatorSendingService performanceIndicatorSendingService;

    public UtilityEvaluatorController(Context exnContext, Publisher performanceIndicatorPublisher){
        this.nodeCandidatesService = new NodeCandidatesFetchingService(exnContext);
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
         *  send the parameters via ActiveMQ
         */
        for (String component : application.getVariables().keySet()){

            List<NodeCandidate> nodeCandidates = nodeCandidatesService.getNodeCandidatesViaMiddleware(application, component);
            log.info("Number of Node Candidates: {}", nodeCandidates.size());
            if (nodeCandidates.isEmpty()){
                log.error("SAL returned empty list, it is not possible to create cost performance indicator for component {}", component);
                continue;
            }
            List<NodeCandidateDTO> convertedNodeCandidates = NodeCandidateConverter.convertToDtoList(nodeCandidates);
            if (convertedNodeCandidates.isEmpty()){
                log.error("There are no Node Candidates for component {} available after the filtering, it is not possible to create any price estimator", component);
                continue; 
            }
            List<VariableDTO> componentVariables = application.getVariables().get(component);

            if (componentVariables.stream().filter(var -> var.getType().equals(VariableType.CPU) || var.getType().equals(VariableType.RAM)).findAny().isPresent()){
                SimpleCostRegression regression = new SimpleCostRegression(component, convertedNodeCandidates, componentVariables);
                application.getCostPerformanceIndicators().put(component, regression);
            }
            else {
                log.warn("There are no variables for component {} = it is not possible to create any cost performance indicator!", component);
            }


        };
        
        log.info("Creating regression for cost performance indicators has been successfully finished");
        
        performanceIndicatorSendingService.sendPerformanceIndicators(application);
        log.info("Performance indicators sent");

        return application;
    }

}

