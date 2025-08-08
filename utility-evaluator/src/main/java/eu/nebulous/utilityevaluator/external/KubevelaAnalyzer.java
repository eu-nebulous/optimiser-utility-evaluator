package eu.nebulous.utilityevaluator.external;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import eu.nebulous.utilityevaluator.external.sal.AttributeRequirement;
import eu.nebulous.utilityevaluator.external.sal.Requirement;
import eu.nebulous.utilityevaluator.external.sal.RequirementOperator;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A collection of methods to extract node requirements from KubeVela files.
 */
@Slf4j
public class KubevelaAnalyzer {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Return true if a component is a persistent volume storage node.<p>
     *
     * We look for one of the following structures:
     *
     * <pre>{@code
     * type: raw
     * properties:
     *   apiVersion: v1
     *   kind: PersistentVolumeClaim
     * }</pre>
     *
     * or
     *
     * <pre>{@code
     * type: k8s-objects
     * properties:
     *   objects:
     *   - apiVersion: v1
     *     kind: PersistentVolumeClaim
     * }</pre>
     *
     * Note: In the two samples above, the objects will have other attributes
     * as well, which we omit for brevity.<p>
     *
     * @param component The component; should be a child of the {@code spec:}
     *  top-level array in the KubeVela YAML.
     *
     * @return true if component is a persisten volume component, false if not
     */
    public static boolean isVolumeComponent(JsonNode component) {
        boolean form1 = component.at("/type").asText().equals("raw")
            && component.at("/properties/kind").asText().equals("PersistentVolumeClaim");
        boolean form2 = component.at("/type").asText().equals("k8s-objects")
            && StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                    component.withArray("/properties/objects")
                        .elements(), Spliterator.ORDERED), false)
                .anyMatch((o) -> o.at("/kind").asText()
                    .equals("PersistentVolumeClaim"));
        return form1 || form2;
    }

    /**
     * Return true if the component is a serverless component.<p>
     *
     * A component is serverless if it has {@code type: knative-serving}.
     *
     * @param component The component; should be a child of the {@code spec:}
     *  top-level array in the KubeVela YAML.
     *
     * @return true if component is serverless, false if not.
     */
    public static final boolean isServerlessComponent(JsonNode component) {
        return component.at("/type").asText().equals("knative-serving");
    }

    /**
     * Return true if a component should not be rewritten during deployment,
     * and no virtual machine should be generated.<p>
     *
     * Currently, we do not deploy volume storage nodes and serverless
     * nodes.<p>
     *
     * <pre>{@code
     * type: knative-serving
     * }</pre>
     *
     * @param component The component; should be a child of the {@code spec:}
     *  top-level array in the KubeVela YAML.
     *
     * @return true if component should get a VM, false if not.
     */
    public static boolean componentNeedsNode(JsonNode component) {
        return !isVolumeComponent(component) && !isServerlessComponent(component);
    }

    public static boolean hasServerlessComponents(JsonNode kubevela) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                kubevela.withArray("/spec/components").elements(), Spliterator.ORDERED),
            false)
            .anyMatch(KubevelaAnalyzer::isServerlessComponent);
    }

    /**
     * Return true if the component is a serverless-platform component.<p>
     *
     * A component is a serverless platform, i.e., should run serverless
     * components, if it has {@code type: serverless-platform}.
     *
     * @param component The component; should be a child of the {@code spec:}
     *  top-level array in the KubeVela YAML.
     *
     * @return true if component is a serverless platform, false if not.
     */
    public static final boolean isServerlessPlatform(JsonNode component) {
        return component.at("/type").asText().equals("serverless-platform");
    }

    /**
     * Find the names of all {@code serverless-platform} nodes.
     */
    public static final List<String> findServerlessPlatformNames(JsonNode kubevela) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                kubevela.withArray("/spec/components").elements(), Spliterator.ORDERED),
            false)
            .filter(KubevelaAnalyzer::isServerlessPlatform)
            .map((component) -> component.at("/name").asText())
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Given a KubeVela file, extract how many nodes to deploy for each
     * component.
     *
     * We currently look for the following component trait:
     *
     * <pre>{@code
     * traits:
     *  - type: scaler
     *    properties:
     *      replicas: 2
     * }</pre>
     *
     * If this trait is not found for a component, its count will be 1.
     *
     * <p>Note that the map entry can be zero when the component should not be
     * deployed at all.  This can happen for example when there is a cloud and
     * an edge version of the component and only one of them should run.
     *
     * <p>Note that some components named in kubevela will not have entries in
     * the map, among them serverless and volume components.
     *
     * @param kubevela the parsed KubeVela file.
     * @return A map from component name to number of instances to generate.
     */
    public static Map<String, Integer> getNodeCount(JsonNode kubevela) {
        Map<String, Integer> result = new HashMap<>();
        for (final JsonNode c : getNodeComponents(kubevela).values()) {
            result.put(c.get("name").asText(), 1); // default value; might get overwritten
            for (final JsonNode t : c.withArray("/traits")) {
                if (t.at("/type").asText().equals("scaler")
                    && t.at("/properties/replicas").canConvertToExactIntegral())
                    {
                        result.put(c.get("name").asText(),
                            t.at("/properties/replicas").asInt());
                    }
            }
        }
        return result;
    }

    /**
     * Extract node count from a KubeVela file.
     *
     * @see #getNodeCount(JsonNode)
     * @param kubevela The KubeVela file, as a YAML string.
     * @return A map from component name to number of instances to generate.
     * @throws JsonProcessingException if the argument does not contain valid YAML.
     */
    public static Map<String, Integer> getNodeCount(String kubevela) throws JsonProcessingException {
        return getNodeCount(parseKubevela(kubevela));
    }

    /**
     * Extract all components that need to be deployed on a node.
     *
     * <p>Serverless and volume components do not get their own node.
     *
     * @param kubevela The parsed kubevela file.
     * @return A map from component name to its JSON node.
     */
    public static Map<String, JsonNode> getNodeComponents(JsonNode kubevela) {
        Map<String, JsonNode> result = new HashMap<>();
        for (final JsonNode c : kubevela.withArray("/spec/components")) {
            if (componentNeedsNode(c)) {
                result.put(c.get("name").asText(), c);
            }
        }
        return result;
    }

    /**
     * Add requirements that are nebulous-specific and either not present in
     * the KubeVela file (minimum node size) or non-standard and cannot be
     * processed by SAL (component geolocation).
     *
     * We currently add the following requirements:
     * <ul>
     * <li> ~3GB of RAM
     * <li> 3 cores
     * <li> The geolocation of the component -- this uses an {@link
     *   AttributeRequirement} that SAL doesn't know about; the CFSB is
     *   expected to filter this out before sending the requirements to SAL.
     * </ul>
     *
     * @param component The nebulous component in question.
     * @param reqs The list of requirements for that component; will be modified.
     */
    public static void addNebulousRequirements(JsonNode component, List<Requirement> reqs) {
        String loc = getComponentGeolocation(component);
        if (loc != null) {
            reqs.add(new AttributeRequirement(
                "hardware", "CFSB-datasource-geolocations",
                RequirementOperator.EQ, loc));
        }
        reqs.add(new AttributeRequirement("hardware", "ram", RequirementOperator.GEQ, "3048"));
        reqs.add(new AttributeRequirement("hardware", "cores", RequirementOperator.GEQ, "3"));
    }

    /**
     * Get cpu requirement, taken from "cpu" resource requirement in KubeVela
     * and rounding up to nearest whole number.
     *
     * @param c A Component branch of the parsed KubeVela file.
     * @param componentName the component name, used only for logging.
     * @return an integer of number of cores required, or -1 in case of no
     *  requirement.
     */
    private static long getCpuRequirement(JsonNode c, String componentName) {
        JsonNode cpu = c.at("/properties/cpu");
        if (cpu.isMissingNode()) cpu = c.at("/properties/resources/requests/cpu");
        if (cpu.isMissingNode()) cpu = c.at("/properties/requests/cpu");
        if (!cpu.isMissingNode()) {
            try {
                return kubevelaNumberToLong(cpu, "cpu");
            } catch (NumberFormatException e) {
                log.warn("CPU spec in {} is not a number, value seen is {} -- optimistically resuming without CPU requirement", componentName, cpu.asText());
                return -1;
            }
        } else {
            // no spec given
            return -1;
        }
    }

    /**
     * Check if the number should be an integer already in the rewritten
     * KubeVela file. The replica count should be an integer.  CPU can be a
     * float in KubeVela and is converted to a ProActive requirement via
     * {@link #getCpuRequirement}.
     */
    public static boolean isKubevelaInteger(String meaning) {
        List<String> integerMeanings = List.of("memory", "replicas");
        return integerMeanings.contains(meaning);
    }

    /**
     * Return the long value of the given JSON node.  If the meaning is
     * "memory", also handle "Mi" and "Gi" suffixes.  If the meaning is "cpu",
     * round up to the nearest integer.  For all other values of meaning,
     * parse as long.
     *
     * @throws NumberFormatException if first argument cannot be parsed as a
     *  number according to the given meaning.
     */
    public static long kubevelaNumberToLong(JsonNode number, String meaning) throws NumberFormatException {
        if ("memory".equals(meaning)) {
            String numericString = number.asText();
            if (numericString.endsWith("Mi")) {
                return Long.parseLong(numericString.substring(0, numericString.length() - 2));
            } else if (numericString.endsWith("Gi")) {
                return Long.parseLong(numericString.substring(0, numericString.length() - 2)) * 1024;
            } else {
                log.warn("Unsupported memory specification in component: '" + numericString + "' (wanted 'Mi' or 'Gi') ");
                if (number.canConvertToLong()) {
                    // we got no suffix at all; optimistically continue
                    return number.asLong();
                } else {
                    // continue even more optimistically (this throws NumberFormatException)
                    return Long.parseLong(numericString);
                }
            }
        } else if ("cpu".equals(meaning)) {
            // KubeVela has fractional core/cpu requirements, and the value
            // might be given as a string instead of a number, so parse string
            // in all cases.  Note that we don't protect against cpu=0 or
            // negative values here.
            double kubevela_cpu = Double.parseDouble(number.asText());
            long sal_cores = Math.round(Math.ceil(kubevela_cpu));
            return sal_cores;
        } else {
            if (number.canConvertToLong()) {
                return number.asLong();
            } else {
                throw new NumberFormatException("Unable to parse " + meaning + " value '" + number + "' as integer (long) value");
            }
        }
    }

    /**
     * Get memory requirement, taken from "memory" resource requirement in KubeVela
     * and converted to Megabytes.
     *
     * We currently handle the "Mi" and "Gi" suffixes that KubeVela uses.  The
     * number can be integer or floating-point.  Floating-point values might
     * come from the user, when specifying memory in GB, or from the solver.
     *
     * @param c A Component branch of the parsed KubeVela file.
     * @param componentName the component name, used only for logging.
     * @return an integer of memory required in Mb, or -1 in case of no
     *  requirement.
     */
    public static long getMemoryRequirement(JsonNode c, String componentName) {
        JsonNode memory = c.at("/properties/memory");
        if (memory.isMissingNode()) memory = c.at("/properties/resources/requests/memory");
        if (memory.isMissingNode()) memory = c.at("/properties/requests/memory");
        if (!memory.isMissingNode()) {
            long sal_memory = -1;
            String sal_memory_str = memory.asText();
            if (sal_memory_str.endsWith("Mi")) {
                sal_memory = Double.valueOf(sal_memory_str.substring(0, sal_memory_str.length() - 2))
                    .longValue();
            } else if (sal_memory_str.endsWith("Gi")) {
                sal_memory = Double.valueOf(sal_memory_str.substring(0, sal_memory_str.length() - 2))
                    .longValue() * 1024;
            } else {
                log.warn("Unsupported memory specification in component " + componentName + " : " + memory.asText() + " (wanted 'Mi' or 'Gi') ");
                sal_memory = Double.valueOf(sal_memory_str).longValue();
            }
            return sal_memory;
        } else {
            return -1;
        }
    }

    /**
     * Given a KubeVela component, extract the node location, if present.<p>
     *
     * We currently look for the following component trait:
     *
     * <pre>{@code
     * traits:
     *   - type: annotations
     *     properties:
     *       datasource_geolocations: "[[54.5798,-3.5820],[45.4298,13.5820]]"
     * }</pre>
     *
     * @param component the parsed KubeVela file.
     * @return The component's geolocation information, or null.
     */
    private static String getComponentGeolocation(JsonNode component) {
        for (final JsonNode t : component.withArray("/traits")) {
            if (t.at("/type").asText().equals("annotations")
                && !t.at("/properties/datasource_geolocations").isMissingNode())
            {
                return t.at("/properties/datasource_geolocations").asText();
            }
        }
        return null;
    }


    /**
     * Extract node requirements from a KubeVela file in a form we can send to
     * the SAL `findNodeCandidates` endpoint. <p>
     *
     * We read the following attributes for each component:
     *
     * - `properties.cpu`, `properties.resources.requests.cpu`: round up to
     *   next integer and generate requirement `hardware.cores`
     *
     * - `properties.memory`, `properties.resources.requests.memory`: Handle
     *   "200Mi", "0.2Gi" and bare number, convert to MB and generate
     *   requirement `hardware.memory`
     *
     * Notes:<p>
     *
     * - When asked to, we add the requirement that memory >= 2GB.<p>
     *
     * - We skip volume storage components, since there is no virtual machine
     *   created for those.<p>
     *
     * - For the first version, we specify all requirements as "greater or
     *   equal", i.e., we might not find precisely the node candidates that
     *   are asked for. <p>
     *
     * - Related, KubeVela specifies "cpu" as a fractional value, while SAL
     *   wants the number of cores as a whole number.  We round up to the
     *   nearest integer and ask for "this or more" cores, since we might end
     *   up with needing, e.g., 3 cores, which is not a configuration commonly
     *   provided by cloud providers. <p>
     *
     * <p>Note that some components named in kubevela will not have entries in
     * the map, among them serverless and volume components.
     *
     * @param kubevela the parsed KubeVela file.
     * @param includeNebulousRequirements if true, include requirements for
     *  minimum memory size, Ubuntu OS.  These requirements ensure that the
     *  node candidate can run the Nebulous software.
     * @return a map of component name to (potentially empty) list of
     *  requirements for that component.  No requirements mean any node will
     *  suffice.  No requirements are generated for volume storage components.
     */
    public static Map<String, List<Requirement>> getBoundedRequirements(JsonNode kubevela, boolean includeNebulousRequirements) {
        Map<String, List<Requirement>> result = new HashMap<>();
        for (final JsonNode c : getNodeComponents(kubevela).values()) {
            String componentName = c.get("name").asText();
            ArrayList<Requirement> reqs = new ArrayList<>();
            if (includeNebulousRequirements) {
                addNebulousRequirements(c, reqs);
            }
            long cores = getCpuRequirement(c, componentName);
            if (cores > 0) {
                reqs.add(new AttributeRequirement("hardware", "cores",
                    RequirementOperator.GEQ, Long.toString(cores)));
            }
            long memory = getMemoryRequirement(c, componentName);
            if (memory > 0) {
                    reqs.add(new AttributeRequirement("hardware", "ram",
                        RequirementOperator.GEQ, Long.toString(memory)));
            }
            for (final JsonNode t : c.withArray("/traits")) {
                // TODO: Check for node affinity / country / node type (edge
                // or cloud)
            }
            // Finally, add requirements for this job to the map
            result.put(componentName, reqs);
        }
        return result;
    }

    /**
     * Get node requirements for app components, including nebulous-specific
     * requirements.  This method calls {@link #getBoundedRequirements(JsonNode,
     * boolean)} with second parameter {@code true}.
     *
     * @see #getBoundedRequirements(JsonNode, boolean)
     */
    public static Map<String, List<Requirement>> getBoundedRequirements(JsonNode kubevela) {
        return getBoundedRequirements(kubevela, true);
    }

    /**
     * Get node requirements for app components, including nebulous-specific
     * requirements.  Like {@link #getBoundedRequirements} but also include an
     * upper bound of twice the requirement size.  I.e., for cpu=2, we ask for
     * cpu >= 2, cpu <= 4.  Take care to not ask for less than 2048Mb of
     * memory since that's the minimum Nebulous requirement for now.
     *
     * <p>Note that some components named in kubevela will not have entries in
     * the map, among them serverless and volume components.
     */
    public static Map<String, List<Requirement>> getClampedRequirements(JsonNode kubevela) {
        Map<String, List<Requirement>> result = new HashMap<>();
        for (final JsonNode c : getNodeComponents(kubevela).values()) {
            String componentName = c.get("name").asText();
            ArrayList<Requirement> reqs = new ArrayList<>();
            addNebulousRequirements(c, reqs);
            long cores = getCpuRequirement(c, componentName);
            if (cores > 0) {
                reqs.add(new AttributeRequirement("hardware", "cores",
                    RequirementOperator.GEQ, Long.toString(cores)));
                reqs.add(new AttributeRequirement("hardware", "cores",
                    RequirementOperator.LEQ, Long.toString(cores * 2)));
            }
            long memory = getMemoryRequirement(c, componentName);
            if (memory > 0) {
                reqs.add(new AttributeRequirement("hardware", "ram",
                    RequirementOperator.GEQ, Long.toString(memory)));
                reqs.add(new AttributeRequirement("hardware", "ram",
                    // See addNebulousRequirements(), don't ask for both more
                    // and less than 2048
                    RequirementOperator.LEQ, Long.toString(Math.max(memory * 2, 2048))));
            }
            for (final JsonNode t : c.withArray("/traits")) {
                // TODO: Check for node affinity / country / node type (edge
                // or cloud)
            }
            // Finally, add requirements for this job to the map
            result.put(componentName, reqs);
        }
        return result;
    }

    /**
     * Get node requirements for app components, including nebulous-specific
     * requirements.  Like {@link #getBoundedRequirements} but require precise
     * amounts, i.e., ask for precisely cpu == 2, memory == 2048 instead of
     * asking for >= or <=.  Note that we still ask for >= 2048 Mb since
     * that's the nebulous lower bound for now.
     *
     * <p>Note that some components named in kubevela will not have entries in
     * the map, among them serverless and volume components.
     */
    public static Map<String, List<Requirement>> getPreciseRequirements(JsonNode kubevela) {
        Map<String, List<Requirement>> result = new HashMap<>();
        for (final JsonNode c : getNodeComponents(kubevela).values()) {
            String componentName = c.get("name").asText();
            ArrayList<Requirement> reqs = new ArrayList<>();
            addNebulousRequirements(c, reqs);
            long cores = getCpuRequirement(c, componentName);
            if (cores > 0) {
                reqs.add(new AttributeRequirement("hardware", "cores",
                    RequirementOperator.EQ, Long.toString(cores)));
            }
            long memory = getMemoryRequirement(c, componentName);
            if (memory > 0) {
                reqs.add(new AttributeRequirement("hardware", "ram",
                    // See addNebulousRequirements; don't ask for less than
                    // the other constraint allows
                    RequirementOperator.EQ, Long.toString(Math.max(memory, 2048))));
            }
            for (final JsonNode t : c.withArray("/traits")) {
                // TODO: Check for node affinity / country / node type (edge
                // or cloud)
            }
            // Finally, add requirements for this job to the map
            result.put(componentName, reqs);
        }
        return result;
    }

    /**
     * Extract node requirements from a KubeVela file.
     *
     * @see #getBoundedRequirements(JsonNode)
     * @param kubevela The KubeVela file, as a YAML string.
     * @return a map of component name to (potentially empty, except for OS
     *  family) list of requirements for that component.  No requirements mean
     *  any node will suffice.
     * @throws JsonProcessingException if kubevela does not contain valid YAML.
     */
    public static Map<String, List<Requirement>> getBoundedRequirements(String kubevela) throws JsonProcessingException {
        return getBoundedRequirements(parseKubevela(kubevela));
    }

    /**
     * Convert YAML KubeVela into a parsed representation.
     *
     * @param kubevela The KubeVela YAML.
     * @return A parsed representation of the KubeVela file, or null for a parse error.
     * @throws JsonProcessingException if kubevela does not contain valid YAML.
     */
    public static JsonNode parseKubevela(String kubevela) throws JsonProcessingException {
        if (kubevela == null) {
            throw new JsonParseException("The provided string value was null");
        }
        return yamlMapper.readTree(kubevela);
    }

    /**
     * Convert the parsed representation of a KubeVela file to yaml.
     *
     * @param kubevela The KubeVela parsed file.
     * @return A YAML representation of the KubeVela file.
     * @throws JsonProcessingException if YAML cannot be generated from kubevela.
     */
    public static String generateKubevela(JsonNode kubevela) throws JsonProcessingException {
        return yamlMapper.writeValueAsString(kubevela);
    }
}
