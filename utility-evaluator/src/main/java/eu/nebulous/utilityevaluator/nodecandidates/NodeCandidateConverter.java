package eu.nebulous.utilityevaluator.nodecandidates;

import java.util.List;
import java.util.stream.Collectors;
import org.ow2.proactive.sal.model.NodeCandidate;


//static class that converts Node Candidates from SAL to NodeCandidatesDTO, which are then directly saved in node Candidates tensor
public class NodeCandidateConverter {

    public static final String CSV_HEADER = "id;gpu;cpu;ram;location;latitude;longitude;provider;type;price\n";

    public static List<NodeCandidateDTO> convertToDtoList(List<NodeCandidate> nodeCandidates) {
        return nodeCandidates.stream()
                .map(NodeCandidateConverter::convertToDto)
                .collect(Collectors.toList());
    }
 
    private static NodeCandidateDTO convertToDto(NodeCandidate nodeCandidate) {
        NodeCandidateDTO dto = new NodeCandidateDTO(
            nodeCandidate.getNodeCandidateType(), 
            nodeCandidate.getPrice(), 
            nodeCandidate.getCloud().getId(), 
            nodeCandidate.getHardware().getCores(), 
            nodeCandidate.getHardware().getFpga(), 
            nodeCandidate.getHardware().getRam(),
            nodeCandidate.getLocation().getGeoLocation().getCountry(),
            nodeCandidate.getLocation().getGeoLocation().getLatitude(), 
            nodeCandidate.getLocation().getGeoLocation().getLongitude(),
            nodeCandidate.getId()
        );
        return dto;
    }



    public static String convertToCsv(List<NodeCandidateDTO> nodeCandidates) {
        // Create CSV header
        String csv = CSV_HEADER;

        // Append node candidates to CSV
        csv += nodeCandidates.stream()
                .map(nc->NodeCandidateConverter.convertNodeCandidateToCsv(nc))
                .collect(Collectors.joining("\n"));

        return csv;
    }

    private static String convertNodeCandidateToCsv(NodeCandidateDTO nodeCandidate) {
        // Convert a single NodeCandidate to CSV format
        return String.format("%s;%d;%d;%d;%s;%f;%f;%s;%s;%f",
                nodeCandidate.getId(),
                nodeCandidate.getGpu(),
                nodeCandidate.getCpu(),
                nodeCandidate.getRam(),
                nodeCandidate.getLocation(),
                nodeCandidate.getLatitude(),
                nodeCandidate.getLongitude(),
                nodeCandidate.getProvider(),
                nodeCandidate.getType(),
                nodeCandidate.getPrice());
    }

}
