package eu.nebulous.utilityevaluator.model;

import org.ow2.proactive.sal.model.NodeCandidate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class NodeCandidateDTO {
    @NonNull
    private NodeCandidate.NodeCandidateTypeEnum type;
    @NonNull
    private Double price; 
    @NonNull
    private String provider;
    @NonNull
    private Integer cpu; 
    @NonNull
    private Integer gpu;
    @NonNull
    private Long ram; 
    @NonNull
    private String location;
    @NonNull
    private Double latitude;
    @NonNull
    private Double longitude;

    private String id;
}
    

