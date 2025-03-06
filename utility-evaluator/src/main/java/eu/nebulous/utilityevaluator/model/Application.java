package eu.nebulous.utilityevaluator.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import eu.nebulous.utilityevaluator.converter.VariableConverter;
import eu.nebulous.utilityevaluator.external.KubevelaAnalyzer;
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

    @Getter @NonNull
    private Map<String, Set<String>> clouds;

    /** Location of the kubevela yaml file in the app creation message (String) */
    private static final JsonPointer KUBEVELA_PATH = JsonPointer.compile("/content");
    /** Location of the variables (optimizable locations) of the kubevela file
     * in the app creation message. (Array of objects) */
    private static final JsonPointer VARIABLES_PATH = JsonPointer.compile("/variables");
    /** Locations of the UUID and name in the app creation message (String) */
    private static final JsonPointer UUID_PATH = JsonPointer.compile("/uuid");
    private static final JsonPointer NAME_PATH = JsonPointer.compile("/title");
    /** Location of the list of cloud and regions in the app creation
     * message. (Array of objects) */
    private static final JsonPointer CLOUDS_PATH = JsonPointer.compile("/resources");

    public Application (JsonNode appMessage) throws JsonProcessingException {
        //this.kubevela = appMessage.at(KUBEVELA_PATH);
        this.kubevela= KubevelaAnalyzer.parseKubevela(appMessage.at(KUBEVELA_PATH).textValue());
        this.applicationId = appMessage.at(UUID_PATH).textValue();
        this.applicationName = appMessage.at(NAME_PATH).textValue();
        JsonNode variables = appMessage.at(VARIABLES_PATH);
        this.variables = VariableConverter.convertAndGroupVariables(variables);
        this.costPerformanceIndicators = new HashMap<>();
        this.clouds = new HashMap<>();
        for (JsonNode cloud : appMessage.withArray(CLOUDS_PATH)) {
            if (!cloud.at("/enabled").asBoolean()) continue;
            String name = cloud.at("/uuid").asText();
            Set<String> regions = Arrays.stream(cloud.at("/regions").asText().split(","))
                .filter(regionName -> !regionName.isBlank())
                .filter(regionName -> !regionName.equals("null")) // https://github.com/eu-nebulous/optimiser-controller/issues/56
                .collect(Collectors.toSet());
            if (!regions.isEmpty()) {
                this.clouds.put(name, regions);
            }
        }
        // deployment without clouds will fail, so no need to worry too much here
        if (clouds.isEmpty()) log.info("No enabled clouds in the app creation message.");
        log.info("Application message successfully parsed");
    }

    /*public Application(GenericDSLMessage message){
        this.applicationId = message.getUuid();
        this.kubevela = message.getContent();
        this.applicationName = message.getTitle();
        
        message.getVariables();
        message.getResources();
    }*/
}
