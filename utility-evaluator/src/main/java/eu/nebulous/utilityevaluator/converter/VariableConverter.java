package eu.nebulous.utilityevaluator.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.fasterxml.jackson.databind.JsonNode;

import eu.nebulous.utilityevaluator.model.VariableDTO;
import eu.nebulous.utilityevaluator.model.VariableType;
import eu.nebulous.utilityevaluator.model.message.Variable;
import lombok.extern.slf4j.Slf4j;

/* 
 * This class is responsible for converting variables from generic.dsl message to a map < String, List<VariableDTO>>, 
 * where the key is the component name and the value is the list of variables related to this component
 */
@Slf4j
 public class VariableConverter{

    
    public static Map<String, List<VariableDTO>> convertAndGroupVariables(JsonNode variables){
        
        Map<String, List<VariableDTO>> mapOfVariables = new HashMap<>();
        List<String> componentNames = new ArrayList<>();
            
        if (!variables.isArray()) {
            log.warn("No variables have been defined, is not possible to perform any optimization");
            return mapOfVariables;
        }

        for (JsonNode var : variables) {
                //var.get("key").asText(); //to get the name of the variable
                //var.get("meaning").asText(); //to get the type
                 //to get the path
            String component = getPrefix(var.get("path").asText());
            log.info("Component: {}", component);
            String meaning = getLastPart(var.get("meaning").asText());
            log.info("meaning: {}", meaning);
            VariableDTO variableDTO = new VariableDTO(var.get("key").asText(), component, meaning);
            
            //if it is a first variable related to this component
            if (!mapOfVariables.containsKey(component)) { 
                componentNames.add(component);
                ArrayList<VariableDTO> variablesList = new ArrayList<>();
                variablesList.add(variableDTO);
                mapOfVariables.put(component, variablesList);
                log.info("Adding new variable: {} for component: {}", variableDTO.getName(), variableDTO.getComponentName());
                
            }
            else {
                mapOfVariables.get(component).add(variableDTO);
            }
            
        }
        return mapOfVariables;
    }
    
    private static String getPrefix(String str) {
        int endIndex = str.indexOf('/', "/spec/components/".length());
        if (endIndex == -1) {
            return str;
        }
        return str.substring(0, endIndex);
    }

    private static String getLastPart(String input) {
        String[] parts = input.split("\\.");
        return parts[parts.length - 1];
    }




    //old message, to be deleted
    public static Map<String, List<VariableDTO>> convertAndGroupVariables(List<Variable> variables){
        
        Map<String, List<VariableDTO>> mapOfVariables = new HashMap<>();
        List<String> componentNames = new ArrayList<>();
        for (Variable v: variables){
            String prefix = getPrefix(v.getPath());
            VariableDTO variableDTO = new VariableDTO(v.getKey(), prefix, v.getMeaning());
            if (!mapOfVariables.containsKey(prefix)) { //if it is a first variable related to this component
                componentNames.add(prefix);
                ArrayList<VariableDTO> variablesList = new ArrayList<>();
                variablesList.add(variableDTO);
                mapOfVariables.put(prefix, variablesList);
            }
            mapOfVariables.get(prefix).add(variableDTO);
            
        }
        return mapOfVariables;
    

    }    
}
