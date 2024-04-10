package eu.nebulous.utilityevaluator.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;

import eu.nebulous.utilityevaluator.converter.VariableConverter;
import eu.nebulous.utilityevaluator.model.message.GenericDSLMessage;
import eu.nebulous.utilityevaluator.regression.SimpleCostRegression;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
@ToString
public class Application {
    
    @NonNull
    private String applicationId;
    private String applicationName;
    @NonNull
    private JsonNode kubevela;
    private List<String> chosenProviders;
    @NonNull
    private Map<String, List<VariableDTO>> variables;
    @Setter
    private Map<String, SimpleCostRegression> costPerformanceIndicators;

/** Location of the kubevela yaml file in the app creation message (String) */
private static final JsonPointer KUBEVELA_PATH = JsonPointer.compile("/content");
/** Location of the variables (optimizable locations) of the kubevela file
 * in the app creation message. (Array of objects) */
private static final JsonPointer VARIABLES_PATH = JsonPointer.compile("/variables");
/** Locations of the UUID and name in the app creation message (String) */
private static final JsonPointer UUID_PATH = JsonPointer.compile("/uuid");
private static final JsonPointer NAME_PATH = JsonPointer.compile("/title");
/** Location of the variables (optimizable locations) of the kubevela file
 * in the app creation message. (Array of objects) */
private static final JsonPointer PROVIDERS_PATH = JsonPointer.compile("/resources");

    public Application (JsonNode appMessage) {
        try {
            this.kubevela = appMessage.at(KUBEVELA_PATH);
            this.applicationId = appMessage.at(UUID_PATH).textValue();
            this.applicationName = appMessage.at(NAME_PATH).textValue();
            JsonNode variables = appMessage.at(VARIABLES_PATH);
            this.variables = VariableConverter.convertAndGroupVariables(variables);
            log.info("Application message successfully parsed");

        } catch (Exception e) {
            log.error("Could not read app creation message", e);
        }
    }

    /*public Application(GenericDSLMessage message){
        this.applicationId = message.getUuid();
        this.kubevela = message.getContent();
        this.applicationName = message.getTitle();
        
        message.getVariables();
        message.getResources();
    }*/
}
