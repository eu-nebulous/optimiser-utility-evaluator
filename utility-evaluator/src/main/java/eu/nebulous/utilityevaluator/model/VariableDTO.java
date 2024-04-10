package eu.nebulous.utilityevaluator.model;

import lombok.Getter;

@Getter
public class VariableDTO {
    private String name;
    private String componentName;
    private VariableType type;
    
    public VariableDTO(String name, String componentName, String meaning){
        this.name = name;
        this.componentName = componentName;
        this.type = VariableType.fromValue(meaning);
    }

}
