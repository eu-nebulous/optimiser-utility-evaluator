package eu.nebulous.utilityevaluator.model;


public enum VariableType {
    CPU ("cpu"),
    RAM ("memory"),
    LOCATION ("location"),
    STORAGE("storage"),
    REPLICAS ("replicas");

    private final String value;
    
    VariableType (String value){
        this.value = value;
    }

    public static VariableType fromValue(String value) {
        for (VariableType enumValue : VariableType.values()) {
            if (enumValue.value.equals(value)) {
                return enumValue;
            }
        }
        throw new IllegalArgumentException("No enum constant with value: " + value);
    }

}
