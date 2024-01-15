package eu.nebulous.utilityevaluator.communication.activemq.message;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class NodeCandidatesTensorMessage implements Serializable{

    private static final long serialVersionUID = 1L;

    @NonNull
    private String applicationID;
    @NonNull
    private String nodeCandidatesTensorCSV;
    
}
