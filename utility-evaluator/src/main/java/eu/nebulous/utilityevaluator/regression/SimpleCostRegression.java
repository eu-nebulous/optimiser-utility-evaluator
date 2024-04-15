package eu.nebulous.utilityevaluator.regression;

import java.util.List;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import eu.nebulous.utilityevaluator.converter.NodeCandidateConverter;
import eu.nebulous.utilityevaluator.model.NodeCandidateDTO;
import eu.nebulous.utilityevaluator.model.VariableDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SimpleCostRegression {
    
    private String componentName; //indicates for which component this cost regression is done
    private OLSMultipleLinearRegression regression;
    @Getter
    private double[] coefficients; //the results of the regression


    public SimpleCostRegression(String componentName, List<NodeCandidateDTO> nodeCandidates, List<VariableDTO> variables){
        this.componentName = componentName;

        this.regression = new OLSMultipleLinearRegression();
        //double[][] argumentsForRegression = NodeCandidateConverter.convertListToDoubleArray (nodeCandidates, variables);
        regression.newSampleData(convertPricesToArray(nodeCandidates), NodeCandidateConverter.convertListToDoubleArray (nodeCandidates, variables));
        log.info("Data for component {} was loaded", componentName);
        this.coefficients = regression.estimateRegressionParameters();
        log.info("Coefficients: {}", coefficients);

    }

    private static double[] convertPricesToArray(List<NodeCandidateDTO> nodeCandidates) {
        int size = nodeCandidates.size();
        double[] pricesArray = new double[size];
        
        for (int i = 0; i < size; i++) {
            NodeCandidateDTO node = nodeCandidates.get(i);
            pricesArray[i] = node.getPrice();
        }
        
        return pricesArray;
    }

}
