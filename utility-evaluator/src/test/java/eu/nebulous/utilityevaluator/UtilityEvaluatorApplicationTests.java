package eu.nebulous.utilityevaluator;

import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.ow2.proactive.sal.model.Cloud;
import org.ow2.proactive.sal.model.CloudType;
import org.ow2.proactive.sal.model.GeoLocation;
import org.ow2.proactive.sal.model.Hardware;
import org.ow2.proactive.sal.model.NodeCandidate;
import org.ow2.proactive.sal.model.NodeCandidate.NodeCandidateTypeEnum;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.nebulous.utilityevaluator.converter.NodeCandidateConverter;
import eu.nebulous.utilityevaluator.model.NodeCandidateDTO;
import eu.nebulous.utilityevaluator.model.message.FetchNodeCandidatesMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SpringBootTest
class UtilityEvaluatorApplicationTests {

    @MockBean
    private JmsTemplate jmsTemplate;

    //@Test
    void testConvertingNodeCandidates(){
        String filename = "/Users/martarozanska/nebulous/git/optimiser-utility-evaluator/utility-evaluator/src/test/java/resources/response-all-clouds.json";

        File fileNC = new File(filename);
        ObjectMapper mapper = new ObjectMapper();
        //List<NodeCandidate> nodeCandidates = mapper.readValue(fileNC, );


        
    }
    
    //@Test
    void testNodeCandidatesConverter() {
        // Arrange
        List<NodeCandidate> mockNodeCandidates = Collections.singletonList(createMockNodeCandidate());
        Map<String,String> cloudProviders = Map.of("aws-ec2", "longsalid", "openstack", "longsalid2");
  
        Mockito.when(jmsTemplate.receiveAndConvert("utilityEvaluatorInitialize"))
               .thenReturn(new FetchNodeCandidatesMessage("appID",cloudProviders ));

        // Act
        List<NodeCandidateDTO> result = NodeCandidateConverter.convertToDtoList(mockNodeCandidates);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(3.40, result.get(0).getLongitude());
        assertEquals(56, result.get(0).getPrice());


        
        // Add your assertions here to verify that the conversion is correct based on your logic
    }

    private NodeCandidate createMockNodeCandidate() {
        // Create and return a mock NodeCandidate object
        // Customize the object based on your requirements for testing
        
        Hardware mockHardware = mock(Hardware.class);
        mockHardware.setCores(10);
        mockHardware.setRam(1000L);
        mockHardware.setFpga("f");
        
        Cloud mockCloud = new Cloud();
        mockCloud.setId("testCloudID");
        mockCloud.setCloudType(CloudType.PUBLIC);
        
        org.ow2.proactive.sal.model.Location mockLocation = mock(org.ow2.proactive.sal.model.Location.class);
        GeoLocation mockGeoLocation = new GeoLocation("Dublin", "IE", 15.40, 3.40);
        mockLocation.setGeoLocation(mockGeoLocation);
        when(mockLocation.getGeoLocation()).thenReturn(mockGeoLocation);
        
        NodeCandidate nc = new NodeCandidate();
        nc.setCloud(mockCloud);
        nc.setHardware(mockHardware);
        nc.setLocation(mockLocation);
        nc.setPrice(56.0);
        nc.setId("That'stestid");
        nc.setNodeCandidateType(NodeCandidateTypeEnum.IAAS);
        return nc;
    }
}
