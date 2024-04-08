package eu.nebulous.utilityevaluator.model.message;

import java.util.List;

import lombok.Getter;

@Getter
public class GenericDSLMessage {
    

    private String title;
    private String uuid;
    private String content;
    private List<Variable> variables;
    private List<Resource> resources;
    private List<Template> templates;
    private List<Parameter> parameters;
    private List<Metric> metrics;
    private SloViolations sloViolations;
    private List<UtilityFunction> utilityFunctions;
    private List<Object> environmentVariables;
    
}
  
    class Value {
        private int lower_bound;
        private int higher_bound;
    
        // Getters and setters
    }
    
    @Getter
    class Resource {
        private String uuid;
        private String title;
        private String platform;
        private boolean enabled;
    
        // Getters and setters
    }
    
    class Template {
        private String id;
        private String type;
        private int minValue;
        private int maxValue;
        private String unit;
    
        // Getters and setters
    }
    
    class Parameter {
        private String name;
        private String template;
    
        // Getters and setters
    }
    
    class Metric {
        private String type;
        private String name;
        private String formula;
        private boolean isWindowInput;
        private Input input;
        private boolean isWindowOutput;
        private Output output;
        private List<String> arguments;
    
        // Getters and setters
    }
    
    class Input {
        private String type;
        private int interval;
        private String unit;
    
        // Getters and setters
    }
    
    class Output {
        private String type;
        private int interval;
        private String unit;
    
        // Getters and setters
    }
    
    class SloViolations {
        private String nodeKey;
        private boolean isComposite;
        private String condition;
        private boolean not;
        private List<SloViolation> children;
    
        // Getters and setters
    }
    
    class SloViolation {
        private String nodeKey;
        private boolean isComposite;
        private String metricName;
        private String operator;
        private String value;
    
        // Getters and setters
    }
    
    class UtilityFunction {
        private String name;
        private String type;
        private Expression expression;
    
        // Getters and setters
    }
    
    class Expression {
        private String formula;
        private List<VariableValue> variables;
    
        // Getters and setters
    }
    
    class VariableValue {
        private String name;
        private String value;
    
        // Getters and setters
    }
    



