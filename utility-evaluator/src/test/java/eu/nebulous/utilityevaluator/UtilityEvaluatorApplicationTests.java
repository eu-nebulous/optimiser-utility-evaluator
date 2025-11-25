package eu.nebulous.utilityevaluator;


import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.nebulous.utilityevaluator.converter.NodeCandidateConverter;
import eu.nebulous.utilityevaluator.external.sal.Cloud;
import eu.nebulous.utilityevaluator.external.sal.Hardware;
import eu.nebulous.utilityevaluator.external.sal.NodeCandidate;
import eu.nebulous.utilityevaluator.model.Application;
import eu.nebulous.utilityevaluator.model.NodeCandidateDTO;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;


class UtilityEvaluatorApplicationTests {

    /** Get the contents of a test file. */
    static String getResourceContent(String filename) throws URISyntaxException, IOException {
        URL resourceUrl = UtilityEvaluatorApplicationTests.class.getClassLoader()
            .getResource(filename);
        return Files.readString(Paths.get(resourceUrl.toURI()),
            StandardCharsets.UTF_8);
    }

    @Test
    public void testConvertToDtoList_AllValidCandidates() {
        // Arrange
        NodeCandidate mockCandidate = Mockito.mock(NodeCandidate.class);
        NodeCandidate.NodeCandidateTypeEnum mockType = Mockito.mock(NodeCandidate.NodeCandidateTypeEnum.class);
        Cloud mockCloud = Mockito.mock(Cloud.class);
        Hardware mockHardware = Mockito.mock(Hardware.class);

        Mockito.when(mockCandidate.getNodeCandidateType()).thenReturn(mockType);
        Mockito.when(mockCandidate.getPrice()).thenReturn(100.0);
        Mockito.when(mockCandidate.getCloud()).thenReturn(mockCloud);
        Mockito.when(mockCloud.getId()).thenReturn("provider-id");
        Mockito.when(mockCandidate.getHardware()).thenReturn(mockHardware);
        Mockito.when(mockHardware.getCores()).thenReturn(4);
        Mockito.when(mockHardware.getFpga()).thenReturn(1);
        Mockito.when(mockHardware.getRam()).thenReturn(16L);
        Mockito.when(mockCandidate.getId()).thenReturn("candidate-id");

        // Act
        List<NodeCandidateDTO> dtos = NodeCandidateConverter.convertToDtoList(Collections.singletonList(mockCandidate));

        // Assert
        assertEquals(1, dtos.size());
        NodeCandidateDTO dto = dtos.get(0);
        assertEquals(mockType, dto.getType());
        assertEquals(100.0, dto.getPrice());
        assertEquals("provider-id", dto.getProvider());
        assertEquals(4, dto.getCpu());
        assertEquals(1, dto.getGpu());
        assertEquals(16L, dto.getRam());
        assertEquals("candidate-id", dto.getId());
    }

    @Test
    public void testConvertToDtoList_CandidateWithNullPrice() {
        // Arrange
        NodeCandidate mockCandidate = Mockito.mock(NodeCandidate.class);
        NodeCandidate.NodeCandidateTypeEnum mockType = Mockito.mock(NodeCandidate.NodeCandidateTypeEnum.class);
        Cloud mockCloud = Mockito.mock(Cloud.class);
        Hardware mockHardware = Mockito.mock(Hardware.class);

        Mockito.when(mockCandidate.getNodeCandidateType()).thenReturn(mockType);
        Mockito.when(mockCandidate.getPrice()).thenReturn(null);
        Mockito.when(mockCandidate.getCloud()).thenReturn(mockCloud);
        Mockito.when(mockCloud.getId()).thenReturn("provider-id");
        Mockito.when(mockCandidate.getHardware()).thenReturn(mockHardware);
        Mockito.when(mockHardware.getCores()).thenReturn(4);
        Mockito.when(mockHardware.getFpga()).thenReturn(1);
        Mockito.when(mockHardware.getRam()).thenReturn(16L);
        Mockito.when(mockCandidate.getId()).thenReturn("candidate-id");

        // Act
        List<NodeCandidateDTO> dtos = NodeCandidateConverter.convertToDtoList(Collections.singletonList(mockCandidate));

        // Assert
        assertEquals(1, dtos.size());
        NodeCandidateDTO dto = dtos.get(0);
        assertEquals(0.0, dto.getPrice()); // Check default price value
    }

    @Test
    public void testConvertToDtoList_CandidateWithNullParameters() {
        // Arrange
        NodeCandidate mockCandidate = Mockito.mock(NodeCandidate.class);
        Mockito.when(mockCandidate.getNodeCandidateType()).thenReturn(null);

        // Act
        List<NodeCandidateDTO> dtos = NodeCandidateConverter.convertToDtoList(Collections.singletonList(mockCandidate));

        // Assert
        assertEquals(0, dtos.size()); // The candidate should be skipped
    }

    @Test
    public void testConvertToDtoList_AllNullCandidates() {
        // Arrange
        List<NodeCandidate> nullCandidates = Arrays.asList(null, null);

        // Act
        List<NodeCandidateDTO> dtos = NodeCandidateConverter.convertToDtoList(nullCandidates);

        // Assert
        assertTrue(dtos.isEmpty()); // No candidates should be converted
    }

    @Test
    public void testConvertToDtoList_EmptyList() {
        // Arrange
        List<NodeCandidate> emptyList = Collections.emptyList();

        // Act
        List<NodeCandidateDTO> dtos = NodeCandidateConverter.convertToDtoList(emptyList);

        // Assert
        assertTrue(dtos.isEmpty()); // No candidates should be converted
    }

    // https://github.com/eu-nebulous/optimiser-utility-evaluator/issues/26
    @Test
    public void testBug26() throws URISyntaxException, IOException {
        String body = getResourceContent("bug-26.json");
        ObjectMapper mapper = new ObjectMapper();
	JsonNode appMessage = mapper.readTree(body);
	Application app = new Application(appMessage);
        assertNotNull(app);
    }

}
