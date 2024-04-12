package eu.nebulous.utilityevaluator.communication.exnconnector;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.nebulous.utilityevaluator.model.Application;
import eu.nebulouscloud.exn.core.Publisher;
import io.micrometer.common.lang.NonNull;
import lombok.AllArgsConstructor;
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
            "variables": "spec_components_0_traits_0_properties_cpu, spec_components_0_traits_0_properties_ram",
            "coefficientsName": "COEFFICIENTS_0"
        },
        {
    	    "name": "cost_pi_1",
            "variables": "spec_components_1_traits_0_properties_cpu",
            "coefficientsName": "COEFFICIENTS_1"
        }
        "initialDataFile": "param: COEFFICIENTS_0 := 1 0.1 2 0.3; \n param: COEFFICIENTS_1 := 1 0.1;"
        
}
     */
    
    public void sendPerformanceIndicators(Application app){
        ObjectNode msg = jsonMapper.createObjectNode();
        //ObjectNode performanceIndicators = msg.withObject("PerformanceIndicators");
        
        ArrayNode performanceIndicators = msg.withArray("PerformanceIndicators");
        
        StringJoiner initialDataFileJoiner = new StringJoiner(" ");
        for (String component: app.getCostPerformanceIndicators().keySet()){


            String componentNameValidAMPL=component.replaceAll("/", "_").substring(1);

            String piName = "cost_pi"+componentNameValidAMPL;

            ObjectNode pi = performanceIndicators.addObject();
            //ObjectNode pi = performanceIndicators.withObject(piName);


            pi.put("name", piName);
            //array of variables
            List<String> variableNames = app.getVariables().get(component).stream().map(var -> var.getName()).collect(Collectors.toList());
            //performanceIndicators.put("variables", variableNames.toString());
            ArrayNode arrayNode = jsonMapper.valueToTree(variableNames);
            pi.set("variables", arrayNode);
            /*try {
                jsonArrayVariableNames = jsonMapper.writeValueAsString(variableNames);
                performanceIndicators.put("variables", jsonArrayVariableNames);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                log.error("Something went wrong when converting variables");
            }*/
            //coefficientsName
            String coefficientsName = "COEFFICIENTS_"+componentNameValidAMPL;
            pi.put("coefficientsName", coefficientsName);
            //initial coefficients
            /*String initialCoefficients = mapInitialCoefficientsToString(coefficientsName, app.getCostPerformanceIndicators().get(component).getCoefficients());
            performanceIndicators.put("initialCoefficients", initialCoefficients);*/
            log.info("Prepared performance indicator {}", performanceIndicators);
            initialDataFileJoiner.add(mapInitialCoefficientToInitialDataFileString(coefficientsName, app.getCostPerformanceIndicators().get(component).getCoefficients()));
        }
        //initial data file:
        String initialDataFile = initialDataFileJoiner.toString();
        log.info("InitialDatafile:{}", initialDataFile);
        msg.put("initialDataFile", initialDataFile);
        log.info("Message to be sent: {}", msg);
        performanceIndicatorPublisher.send(jsonMapper.convertValue(msg, Map.class), app.getApplicationId(), true);
        
    }

    //"param: COEFFICIENTS_0 := 1 0.1 2 0.3; \n param: COEFFICIENTS_1 := 1 0.1;"
    private String mapInitialCoefficientToInitialDataFileString(String coefficientsName, double[] values) {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add("param:" + coefficientsName + " :=");
        int counter = 1;
        for (double num : values) {
            joiner.add(Integer.toString(counter));
            joiner.add(Double.toString(num));
            counter++;
        }
        joiner.add("\n");
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

}



//THIS WORKS:
/* 
public void sendPerformanceIndicators(Application app){
    ObjectNode msg = jsonMapper.createObjectNode();
    ObjectNode performanceIndicators = msg.withObject("PerformanceIndicators");
    
    StringJoiner initialDataFileJoiner = new StringJoiner(" ");
    for (String component: app.getCostPerformanceIndicators().keySet()){


        String componentNameValidAMPL=component.replaceAll("/", "_").substring(1);

        String piName = "cost_pi"+componentNameValidAMPL;

        ObjectNode pi = performanceIndicators.withObject(piName);


        pi.put("name", piName);
        //array of variables
        List<String> variableNames = app.getVariables().get(component).stream().map(var -> var.getName()).collect(Collectors.toList());
        //performanceIndicators.put("variables", variableNames.toString());
        ArrayNode arrayNode = jsonMapper.valueToTree(variableNames);
        pi.set("variables", arrayNode);

        //coefficientsName
        String coefficientsName = "COEFFICIENTS_"+componentNameValidAMPL;
        pi.put("coefficientsName", coefficientsName);
        //initial coefficients
        String initialCoefficients = mapInitialCoefficientsToString(coefficientsName, app.getCostPerformanceIndicators().get(component).getCoefficients());
        performanceIndicators.put("initialCoefficients", initialCoefficients);
        log.info("Prepared performance indicator {}", performanceIndicators);
        initialDataFileJoiner.add(mapInitialCoefficientToInitialDataFileString(coefficientsName, app.getCostPerformanceIndicators().get(component).getCoefficients()));
    }
    //initial data file:
    String initialDataFile = initialDataFileJoiner.toString();
    log.info("InitialDatafile:{}", initialDataFile);
    msg.put("initialDataFile", initialDataFile);
    log.info("Message to be sent: {}", msg);
    performanceIndicatorPublisher.send(jsonMapper.convertValue(msg, Map.class), app.getApplicationId(), true);
    
}*/


