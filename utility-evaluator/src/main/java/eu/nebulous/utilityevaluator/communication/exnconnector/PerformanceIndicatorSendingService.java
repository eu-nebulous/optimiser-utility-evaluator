package eu.nebulous.utilityevaluator.communication.exnconnector;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.nebulous.utilityevaluator.model.Application;
import eu.nebulous.utilityevaluator.model.VariableDTO;
import eu.nebulous.utilityevaluator.regression.SimpleCostRegression;
import eu.nebulouscloud.exn.core.Publisher;
import io.micrometer.common.lang.NonNull;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class PerformanceIndicatorSendingService {
    
    @NonNull
    private Publisher performanceIndicatorPublisher;
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    
    /* Example of the message:
    {
        "performanceIndicators": 
        {
    	    "name": "cost_pi_0",
            "variables": "spec_components_0_traits_0_properties_cpu", "spec_components_0_traits_0_properties_ram",
            "coefficientsName": "COEFFICIENTS_0",
            "initialCoefficients": "COEFFICIENTS_0 := 1 0.1 2 0.3;"
        },
        {
    	    "name": "cost_pi_1",
            "variables": "spec_components_1_traits_0_properties_cpu",
            "coefficientsName": "COEFFICIENTS_1",
            "initialCoefficients": "COEFFICIENTS_1 := 1 0.1 2 0.3;"
        }
        
    }
     */
    
    public void sendPerformanceIndicators(Application app){
        ObjectNode msg = jsonMapper.createObjectNode();
        ObjectNode performanceIndicators = msg.withObject("PerformanceIndicators");
        
        for (String component: app.getCostPerformanceIndicators().keySet()){
            String piName = "cost_pi"+component;
            performanceIndicators.put("name", piName);
            //array of variables
            List<String> variableNames = app.getVariables().get(component).stream().map(var -> var.getName()).collect(Collectors.toList());
            performanceIndicators.put("variables", variableNames.toString());
            //coefficientsName
            String coefficientsName = "COEFFICIENTS_"+component;
            performanceIndicators.put("coefficientsName", coefficientsName);
            //initial coefficients
            String initialCoefficients = mapInitialCoefficientsToString(coefficientsName, app.getCostPerformanceIndicators().get(component).getCoefficients());
            performanceIndicators.put("initialCoefficients", initialCoefficients);
            log.info("Prepared performance indicator {}", performanceIndicators);
        }
        log.info("Message to be sent: {}", msg);
        performanceIndicatorPublisher.send(jsonMapper.convertValue(msg, Map.class), app.getApplicationId(), true);
        
    }
    private String mapInitialCoefficientsToString(String coefficientsName, double[] values) {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add(coefficientsName + " :=");
        for (double num : values) {
            joiner.add(Double.toString(num));
        }
        return joiner.toString();
    }
/* 
    private String mapInitialCoefficientsToString(Map<String, double[]> map) {
        StringJoiner joiner = new StringJoiner(" ");
        map.forEach((componentName, value) -> {
            joiner.add("COEFFICIENTS_"+componentName + " :=");
            for (double num : value) {
                joiner.add(Double.toString(num));
            }
        });
        return joiner.toString();
    }*/
    /*public void sendAMPL() {
        String ampl = AMPLGenerator.generateAMPL(this);
        ObjectNode msg = jsonMapper.createObjectNode();
        msg.put("ObjectiveFunction", getObjectiveFunction());
        ObjectNode constants = msg.withObject("Constants");
          // Define initial values for constant utility functions:
          // "Constants" : {
          //   <constant utility function name> : {
          //        "Variable" : <AMPL Variable Name>
          //        "Value"    : <value at the variable's path in original KubeVela>
          //   }
          // }
        for (final JsonNode function : originalAppMessage.withArray(utility_function_path)) {
            if (!(function.get("type").asText().equals("constant")))
                continue;
            // NOTE: for a constant function, we rely on the fact that the
            // function body is a single variable defined in the "Variables"
            // section and pointing to KubeVela, and the
            // `functionExpressionVariables` array contains one entry.
            JsonNode variable = function.withArray("/expression/variables").get(0);
            String variableName = variable.get("value").asText();
            JsonPointer path = kubevelaVariablePaths.get(variableName);
            JsonNode value = originalKubevela.at(path);
            ObjectNode constant = constants.withObject(function.get("name").asText());
            constant.put("Variable", variableName);
            constant.set("Value", value);
        }
        log.info("Sending AMPL file to solver", keyValue("amplMessage", msg), keyValue("appId", UUID));
        Main.logFile("to-solver-" + getUUID() + ".json", msg.toString());
        Main.logFile("to-solver-" + getUUID() + ".ampl", ampl);
    }*/
}
