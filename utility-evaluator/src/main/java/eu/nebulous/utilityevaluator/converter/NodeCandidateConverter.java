package eu.nebulous.utilityevaluator.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.ow2.proactive.sal.model.NodeCandidate;
import eu.nebulous.utilityevaluator.model.NodeCandidateDTO;
import eu.nebulous.utilityevaluator.model.VariableDTO;
import lombok.extern.slf4j.Slf4j;

//static class that converts Node Candidates from SAL to NodeCandidatesDTO, which are then directly saved in node Candidates tensor
@Slf4j
public class NodeCandidateConverter {

    public static final String CSV_HEADER = "id;gpu;cpu;ram;location;latitude;longitude;provider;type;price\n";

    public static List<NodeCandidateDTO> convertToDtoList(List<NodeCandidate> nodeCandidates) {
        return nodeCandidates.stream()
                .map(NodeCandidateConverter::convertToDto)
                .filter(dto -> dto != null) // Remove unsuccessful conversions
                .collect(Collectors.toList());
    }

    private static NodeCandidateDTO convertToDto(NodeCandidate nodeCandidate) {
        try {
            if (nodeCandidate == null ||
                nodeCandidate.getNodeCandidateType() == null ||
                nodeCandidate.getCloud() == null ||
                nodeCandidate.getCloud().getId() == null ||
                nodeCandidate.getHardware() == null ||
                nodeCandidate.getHardware().getCores() == null ||
                nodeCandidate.getHardware().getFpga() == null ||
                nodeCandidate.getHardware().getRam() == null ||
                nodeCandidate.getId() == null) {
                log.warn("Node candidate was skipped due to null parameter: {}", nodeCandidate.getId());
                return null;
            }

            Double price = nodeCandidate.getPrice() != null ? nodeCandidate.getPrice() : 0.0;

            return new NodeCandidateDTO(
                nodeCandidate.getNodeCandidateType(),
                price,
                nodeCandidate.getCloud().getId(),
                nodeCandidate.getHardware().getCores(),
                nodeCandidate.getHardware().getFpga(),
                nodeCandidate.getHardware().getRam(),
                nodeCandidate.getId()
            );

        } catch (Exception e) {
            log.error("Error while converting node candidate", e);
            return null;
        }
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
                //nodeCandidate.getLocation(),
                //nodeCandidate.getLatitude(),
                //nodeCandidate.getLongitude(),
                nodeCandidate.getProvider(),
                nodeCandidate.getType(),
                nodeCandidate.getPrice());
    }

    //only for variables that are used
    public static double[][] convertListToDoubleArray(List<NodeCandidateDTO> nodeList, List<VariableDTO> variables) {
        int size = nodeList.size();
        double[][] dataArray = new double[size][];

        for (int i = 0; i < size; i++) {
            NodeCandidateDTO node = nodeList.get(i);
            List<Integer> usedNodeParameters = new ArrayList<>();
            for (VariableDTO variable : variables){
                switch (variable.getType()){
                    case CPU:
                        usedNodeParameters.add(node.getCpu());
                        break;
                    case RAM:
                        usedNodeParameters.add(Long.valueOf(node.getRam()).intValue());
                        break;
                    default:
                        log.debug("Variable type {} is not usable in cost performance indicators", variable.getType());
                        break;


                }
            }
            double[] data = new double[usedNodeParameters.size()];
            for (int j = 0; j < usedNodeParameters.size(); j++){
                data[j]=usedNodeParameters.get(j);
            }
            /*{
                node.getCpu(),
                node.getGpu(),
                node.getRam(),
                node.getLatitude(),
                node.getLongitude()
            };*/
            dataArray[i] = data;
        }
        return dataArray;
    }

}
