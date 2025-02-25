package eu.nebulous.utilityevaluator.model.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Variable {
    private String key;
    private String path;
    private String type;
    private String meaning; 
    private Value value;


}

