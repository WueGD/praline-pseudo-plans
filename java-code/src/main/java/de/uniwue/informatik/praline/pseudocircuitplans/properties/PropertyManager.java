package de.uniwue.informatik.praline.pseudocircuitplans.properties;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.Label;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.placements.Orientation;
import edu.uci.ics.jung.graph.UndirectedGraph;

import java.util.*;
import java.util.function.Function;

/**
 * Every Property should exist only once.
 * Therefore, this class exists with all methods and variables being static.
 * All default properties are defined in the code here.
 * All default properties are all added to the existing properties.
 * You may find existing {@link Property}s by calling {@link PropertyManager#getProperty(String)}
 * and create new {@link Property}s by calling
 * {@link NumericalProperty#createNewProperty(String, Function)} and
 * {@link NumberDistributionProperty#createNewProperty(String, Function)}.
 */
public class PropertyManager {

    private static final LinkedHashMap<String, Property> ALL_PROPERTIES = new LinkedHashMap<>();
    
    private static final List<Property> ALL_DEFAULT_PROPERTIES = createAllDefaultProperties();

    /**
     * Find an existing property.
     *
     * @param propertyName
     * @return
     *      null if not yet created
     */
    public static Property getProperty(String propertyName) {
        if (!ALL_PROPERTIES.containsKey(propertyName)) {
            return null;
        }
        return ALL_PROPERTIES.get(propertyName);
    }

    public static Collection<Property> getAllProperties() {
        return new ArrayList<>(ALL_PROPERTIES.values());
    }

    public static void addProperty(Property property) {
        ALL_PROPERTIES.put(property.getPropertyName(), property);
    }

    public static Property removeProperty(String propertyName) {
        return ALL_PROPERTIES.remove(propertyName);
    }

    public static boolean removeProperty(Property property) {
        return ALL_PROPERTIES.remove(property.getPropertyName(), property);
    }


    //helper methods for the default properties

    /**
     *
     * @param portCompositions
     * @param allPortGroups
     * @return
     *      maximum recursion depth
     */
    private static int addAllContainedPortGroupsRecursively(Collection<PortComposition> portCompositions,
                                                            LinkedHashSet<PortGroup> allPortGroups) {
        int maxRecursionDepth = 0;
        for (PortComposition portComposition : portCompositions) {
            if (portComposition instanceof PortGroup) {
                allPortGroups.add((PortGroup) portComposition);
                maxRecursionDepth = Math.max(maxRecursionDepth, 1 + addAllContainedPortGroupsRecursively(
                        ((PortGroup) portComposition).getPortCompositions(),allPortGroups));
            }
        }
        return maxRecursionDepth;
    }

    /**
     *
     * @param vertexGroups
     * @param allVertexGroups
     * @return
     *      maximum recursion depth
     */
    private static int addAllContainedVertexGroupsRecursively(Collection<VertexGroup> vertexGroups,
                                                              LinkedHashSet<VertexGroup> allVertexGroups) {
        int maxRecursionDepth = 1;
        for (VertexGroup vertexGroup : vertexGroups) {
            allVertexGroups.add(vertexGroup);
            if (vertexGroup.getContainedVertexGroups() != null) {
                maxRecursionDepth = Math.max(maxRecursionDepth, 1 + addAllContainedVertexGroupsRecursively(
                        vertexGroup.getContainedVertexGroups(), allVertexGroups));
            }
        }
        return maxRecursionDepth;
    }

    /**
     *
     * @param edgeBundles
     * @param allEdgeBundles
     * @return
     *      maximum recursion depth
     */
    private static int addAllContainedEdgeBundlesRecursively(Collection<EdgeBundle> edgeBundles,
                                                             LinkedHashSet<EdgeBundle> allEdgeBundles) {
        int maxRecursionDepth = 1;
        for (EdgeBundle edgeBundle : edgeBundles) {
            if (edgeBundle == null) {
                continue;
            }
            allEdgeBundles.add(edgeBundle);
            if (edgeBundle.getContainedEdgeBundles() != null) {
                maxRecursionDepth = Math.max(maxRecursionDepth, 1 +
                        addAllContainedEdgeBundlesRecursively(edgeBundle.getContainedEdgeBundles(), allEdgeBundles));
            }
        }
        return maxRecursionDepth;
    }


    //////////////////
    // all default properties
    //////////////////

    private static List<Property> createAllDefaultProperties() {

        ArrayList<Property> allProperties = new ArrayList<>();

        // # vertices

        allProperties.add(NumericalProperty.createNewProperty("vertexCount",
                graph -> graph.getVertices().size()));

        // # splices, connectors, ...

        allProperties.addAll(createCountVertexTypeProperties());

        // # edges

        allProperties.add(NumericalProperty.createNewProperty("edgeCount", graph ->
                graph.getEdges().size()));

        // # vertexGroups

        allProperties.add(NumericalProperty.createNewProperty("vertexGroupCount", graph -> {
            LinkedHashSet<VertexGroup> allVertexGroups = new LinkedHashSet<>();
            for (VertexGroup vertexGroup : graph.getVertexGroups()) {
                allVertexGroups.add(vertexGroup);
                addAllContainedVertexGroupsRecursively(vertexGroup.getContainedVertexGroups(), allVertexGroups);
            }
            return allVertexGroups.size();
        }));

        // # connectors, device connectors, undefined

        allProperties.addAll(createCountVertexGroupTypeProperties());

        // # edgeBundles

        allProperties.add(NumericalProperty.createNewProperty("edgeBundleCount", graph -> {
            LinkedHashSet<EdgeBundle> allEdgeBundles = new LinkedHashSet<>();
            for (EdgeBundle edgeBundle : graph.getEdgeBundles()) {
                allEdgeBundles.add(edgeBundle);
                addAllContainedEdgeBundlesRecursively(edgeBundle.getContainedEdgeBundles(), allEdgeBundles);
            }
            return allEdgeBundles.size();
        }));

        // # edgeBundles where not all edges connect the same vertices

        allProperties.add(NumericalProperty.createNewProperty("edgeBundleOfDifferentEdgesRelativeToVerticesCount",
                graph -> {
                    int sum = 0;
                    for (EdgeBundle edgeBundle : graph.getEdgeBundles()) {
                        LinkedHashSet<EdgeBundle> allContainedEdgeBundles = new LinkedHashSet<>();
                        allContainedEdgeBundles.add(edgeBundle);
                        addAllContainedEdgeBundlesRecursively(edgeBundle.getContainedEdgeBundles(),
                                allContainedEdgeBundles);
                        ArrayList<Edge> allContainedEdges = new ArrayList<>();
                        for (EdgeBundle containedEdgeBundle : allContainedEdgeBundles) {
                            allContainedEdges.addAll(containedEdgeBundle.getContainedEdges());
                        }
                        //check if the contained edges all connect the same set of vertices
                        if (!allContainedEdges.isEmpty()) {
                            //the first entry determines the reference set
                            LinkedHashSet<Vertex> connectedVerticesReferenceSet = new LinkedHashSet<>();
                            for (Port port : allContainedEdges.get(0).getPorts()) {
                                connectedVerticesReferenceSet.add(port.getVertex());
                            }
                            //all others should contain the reference set
                            for (Edge containedEdge : allContainedEdges) {
                                LinkedHashSet<Vertex> connectedVertices = new LinkedHashSet<>();
                                for (Port port : containedEdge.getPorts()) {
                                    connectedVertices.add(port.getVertex());
                                }
                                boolean setsAreEqual = true;
                                if (connectedVertices.size() != connectedVerticesReferenceSet.size()) {
                                    setsAreEqual = false;
                                }
                                for (Vertex connectedVertex : connectedVertices) {
                                    if (!connectedVerticesReferenceSet.contains(connectedVertex)) {
                                        setsAreEqual = false;
                                        break;
                                    }
                                }
                                if (!setsAreEqual) {
                                    ++sum;
                                    break;
                                }
                            }
                        }
                    }
                    return sum;
                }));

        // # edgeBundles with unrelated edges -- this is there are two edges in the bundle that do not have a
        // common vertex

        allProperties.add(NumericalProperty.createNewProperty("edgeBundleOfUnrelatedEdgesCount",
                graph -> {
                    int sum = 0;
                    for (EdgeBundle edgeBundle : graph.getEdgeBundles()) {
                        LinkedHashSet<EdgeBundle> allContainedEdgeBundles = new LinkedHashSet<>();
                        allContainedEdgeBundles.add(edgeBundle);
                        addAllContainedEdgeBundlesRecursively(edgeBundle.getContainedEdgeBundles(),
                                allContainedEdgeBundles);
                        ArrayList<Edge> allContainedEdges = new ArrayList<>();
                        for (EdgeBundle containedEdgeBundle : allContainedEdgeBundles) {
                            allContainedEdges.addAll(containedEdgeBundle.getContainedEdges());
                        }
                        //check if two edges do not even have one common vertex
                        boolean bundleIsUnrelated = false;
                        for (Edge edge0 : allContainedEdges) {
                            LinkedHashSet<Vertex> connectedVerticesEdge0 = new LinkedHashSet<>();
                            for (Port port : edge0.getPorts()) {
                                connectedVerticesEdge0.add(port.getVertex());
                            }
                            for (Edge edge1 : allContainedEdges) {
                                boolean unrelated = true;
                                LinkedHashSet<Vertex> connectedVerticesEdge1 = new LinkedHashSet<>();
                                for (Port port : edge1.getPorts()) {
                                    connectedVerticesEdge1.add(port.getVertex());
                                }
                                for (Vertex vertex : connectedVerticesEdge1) {
                                    if (connectedVerticesEdge0.contains(vertex)) {
                                        unrelated = false;
                                        break;
                                    }
                                }
                                if (unrelated) {
                                    bundleIsUnrelated = true;
                                    break;
                                }
                            }
                            if (bundleIsUnrelated) {
                                break;
                            }
                        }
                        if (bundleIsUnrelated) {
                            ++sum;
                        }
                    }
                    return sum;
                }));

        // # ports

        allProperties.add(NumericalProperty.createNewProperty("portCount", graph -> {
            int sum = 0;
            for (Vertex vertex : graph.getVertices()) {
                sum += vertex.getPorts().size();
            }
            return sum;
        }));

        // # ports without edge

        allProperties.add(NumericalProperty.createNewProperty("portWithoutEdgeCount", graph -> {
            int sum = 0;
            for (Vertex vertex : graph.getVertices()) {
                for (Port port : vertex.getPorts()) {
                    if (port.getEdges() == null || port.getEdges().isEmpty()) {
                        ++sum;
                    }
                }
            }
            return sum;
        }));

        // # ports with multiple edges

        allProperties.add(NumericalProperty.createNewProperty("portWithMultipleEdgesCount", graph -> {
            int sum = 0;
            for (Vertex vertex : graph.getVertices()) {
                for (Port port : vertex.getPorts()) {
                    if (port.getEdges().size() > 1) {
                        ++sum;
                    }
                }
            }
            return sum;
        }));

        // # regular ports

        allProperties.add(NumericalProperty.createNewProperty("regularPortCount", PropertyManager::countRegularPorts));

        // # touching pairs

        allProperties.add(NumericalProperty.createNewProperty("touchingPairCount", graph -> {
            int sum = 0;
            for (VertexGroup vertexGroup : graph.getVertexGroups()) {
                sum += vertexGroup.getTouchingPairs().size();
            }
            return sum;
        }));

        // # port pairings

        allProperties.add(NumericalProperty.createNewProperty("portPairingCount", graph -> {
            int sum = 0;
            for (VertexGroup vertexGroup : graph.getVertexGroups()) {
                sum += vertexGroup.getPortPairings().size();
            }
            return sum;
        }));

        // # port pairings within a vertex

        allProperties.add(NumericalProperty.createNewProperty("portPairingWithinAVertexCount", graph -> {
            int sum = 0;
            for (VertexGroup vertexGroup : graph.getVertexGroups()) {
                for (PortPairing portPairing : vertexGroup.getPortPairings()) {
                    if (portPairing.getPort0().getVertex().equals(portPairing.getPort1().getVertex())) {
                        ++sum;
                    }
                }
            }
            return sum;
        }));

        // # port groups

        allProperties.add(NumericalProperty.createNewProperty("portGroupCount", graph -> {
            LinkedHashSet<PortGroup> allPortGroups = new LinkedHashSet<>();
            for (Vertex vertex : graph.getVertices()) {
                addAllContainedPortGroupsRecursively(vertex.getPortCompositions(), allPortGroups);
            }
            return allPortGroups.size();
        }));

        // # port groups with fixed order

        allProperties.add(NumericalProperty.createNewProperty("orderedPortGroupCount", graph -> {
            LinkedHashSet<PortGroup> allPortGroups = new LinkedHashSet<>();
            for (Vertex vertex : graph.getVertices()) {
                addAllContainedPortGroupsRecursively(vertex.getPortCompositions(), allPortGroups);
            }
            for (PortGroup portGroup : new ArrayList<>(allPortGroups)) {
                if (!portGroup.isOrdered()) {
                    allPortGroups.remove(portGroup);
                }
            }
            return allPortGroups.size();
        }));


        // # ports with fixed orientation

        allProperties.add(NumericalProperty.createNewProperty("portWithFixedOrientationCount", graph -> {
            int sum = 0;
            for (Vertex vertex : graph.getVertices()) {
                for (Port port : vertex.getPorts()) {
                    if (port.getOrientationAtVertex() != Orientation.FREE) {
                        ++sum;
                    }
                }
            }
            return sum;
        }));

        // # port pairing-ports with missing vertex in vertex group

        allProperties.add(NumericalProperty.createNewProperty("portPairingPortsWithMissingVertexInVertexGroup", graph -> {
            int count = 0;
            for (VertexGroup vertexGroup : graph.getVertexGroups()) {
                for (PortPairing portPairing : vertexGroup.getPortPairings()) {
                    for (Port port : portPairing.getPorts()) {
                        if (!vertexGroup.getContainedVertices().contains(port.getVertex())) {
                            ++count;
                        }
                    }
                }
            }
            return count;
        }));

        // # self loop edges

        allProperties.add(NumericalProperty.createNewProperty("selfLoopEdges", graph -> {
            int count = 0;
            for (Edge edge : graph.getEdges()) {
                LinkedHashSet<Vertex> incidentVertices = new LinkedHashSet<>();
                for (Port port : edge.getPorts()) {
                    incidentVertices.add(port.getVertex());
                }
                if (incidentVertices.size() == 1) {
                    count += 1;
                }
            }
            return count;
        }));

        // # parallel edges

        allProperties.add(NumberDistributionProperty.createNewProperty("parallelEdges", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();

            LinkedHashSet<Vertex> alreadyProcessedVertices = new LinkedHashSet<>();
            for (Vertex vertex : graph.getVertices()) {
                LinkedHashMap<Vertex, Integer> otherVertex2numberOfParallelEdges = new LinkedHashMap<>();
                for (Port port : vertex.getPorts()) {
                    for (Edge edge : port.getEdges()) {
                        for (Port otherPort : edge.getPorts()) {
                            Vertex otherVertex = otherPort.getVertex();
                            if (!otherPort.equals(port) && !alreadyProcessedVertices.contains(otherVertex)) {
                                int numberOfParallelEdges = 0;
                                if (otherVertex2numberOfParallelEdges.containsKey(otherVertex)) {
                                    numberOfParallelEdges = otherVertex2numberOfParallelEdges.get(otherVertex);
                                }
                                ++numberOfParallelEdges;
                                otherVertex2numberOfParallelEdges.put(otherVertex, numberOfParallelEdges);
                            }
                        }
                    }
                }
                //save number of parallel edges
                for (Vertex otherVertex : otherVertex2numberOfParallelEdges.keySet()) {
                    distribution.add(otherVertex2numberOfParallelEdges.get(otherVertex));
                }
                alreadyProcessedVertices.add(vertex);
            }
            return distribution;
        }));

        // # connected components

        allProperties.add(NumericalProperty.createNewProperty("componentCount", graph ->
                JungUtils.getConnectedComponents(graph).size()));

        // # connected components of size 1

        allProperties.add(NumericalProperty.createNewProperty("componentOfSize1Count", graph ->{
            int count = 0;
            for (Set<JungUtils.PseudoVertex> connectedComponent : JungUtils.getConnectedComponents(graph)) {
                count += connectedComponent.size() == 1 ? 1 : 0;
            }
            return count;
        }));

        // # vertices per connected component

        allProperties.add(NumberDistributionProperty.createNewProperty("verticesPerComponent", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Set<JungUtils.PseudoVertex> connectedComponent : JungUtils.getConnectedComponents(graph)) {
                distribution.add(connectedComponent.size());
            }
            return distribution;
        }));

        // # vertices in largest connected component

        allProperties.add(NumericalProperty.createNewProperty("verticesInLargestComponent", graph ->
                JungUtils.getLargestConnectedComponent(JungUtils.getConnectedComponents(graph)).size()));

        // diameter in largest component

        allProperties.add(NumericalProperty.createNewProperty("diameterLargestComponent", graph ->
                JungUtils.getDiameterOfLargestComponent(graph)));

        // average distances in largest component

        allProperties.add(NumberDistributionProperty.createNewProperty("averageDistancesInLargestComponent", graph -> {
            NumberDistribution<Double> distribution = new NumberDistribution<>();
            UndirectedGraph<JungUtils.PseudoVertex, Integer> jungGraph = JungUtils.transformToJungGraph(graph);
            JungUtils.removeEverythingButLargestComponent(jungGraph);
            com.google.common.base.Function<JungUtils.PseudoVertex, Double> averageDistances =
                    JungUtils.getAverageDistances(jungGraph);
            for (JungUtils.PseudoVertex vertex : jungGraph.getVertices()) {
                distribution.add(averageDistances.apply(vertex));
            }
            return distribution;
        }));

        // average distances

        allProperties.add(NumberDistributionProperty.createNewProperty("averageDistances", graph -> {
            NumberDistribution<Double> distribution = new NumberDistribution<>();
            UndirectedGraph<JungUtils.PseudoVertex, Integer> jungGraph = JungUtils.transformToJungGraph(graph);
            com.google.common.base.Function<JungUtils.PseudoVertex, Double> averageDistances =
                    JungUtils.getAverageDistances(jungGraph);
            for (JungUtils.PseudoVertex vertex : jungGraph.getVertices()) {
                distribution.add(averageDistances.apply(vertex));
            }
            return distribution;
        }));

        // # ports per vertex

        allProperties.add(NumberDistributionProperty.createNewProperty("ports/vertex", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                distribution.add(vertex.getPorts().size());
            }
            return distribution;
        }));

        // # ports per vertex type

        allProperties.addAll(createPortsPerVertexTypeProperties());

        // # port groups per vertex type

        allProperties.addAll(createPortGroupsPerVertexTypeProperties());

        // # vertices per vertex group type

        allProperties.addAll(createVerticesPerVertexGroupTypeProperties());

        // # ports per vertex group type

        allProperties.addAll(createPortsPerVertexGroupTypeProperties());

        // # port pairings per vertex type

        allProperties.addAll(createPortPairingsPerVertexTypeProperties());

        // # port pairings per vertex group type

        allProperties.addAll(createPortPairingsPerVertexGroupTypeProperties());

        // # unpaired ports per vertex group type

        allProperties.addAll(createUnpairedPortsPerVertexGroupTypeProperties());

        // # ports outside a port group per vertex

        allProperties.add(NumberDistributionProperty.createNewProperty("portsOutsideAPortGroup/vertex", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                //count only top-level ports
                int count = 0;
                for (PortComposition portComposition : vertex.getPortCompositions()) {
                    if (portComposition instanceof Port) {
                        ++count;
                    }
                }
                distribution.add(count);
            }
            return distribution;
        }));

        // # ports per edge (degree of hyperedge)

        allProperties.add(NumberDistributionProperty.createNewProperty("ports/edge(degreeOfHyperedge)", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Edge edge : graph.getEdges()) {
                distribution.add(edge.getPorts().size());
            }
            return distribution;
        }));

        // # hyperedges being adjacent to i ports (degree of hyperedges)

        allProperties.addAll(createDegreeOfHyperedgeProperties());

        // # edges per port

        allProperties.add(NumberDistributionProperty.createNewProperty("edges/port", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                for (Port port : vertex.getPorts()) {
                    distribution.add(port.getEdges().size());
                }
            }
            return distribution;
        }));

        // # i edges per regular port (for definition of regular port see class ImplicitCharacteristics)

        allProperties.addAll(createEdgesPerRegularPortProperties());

        // # edge-splice-port-incidences per # edge-port-incidences

        allProperties.add(NumericalProperty.createNewProperty("edgeSpliceIncidences/edgePortIncidences", graph -> {
            int edgeSpliceIncidences = 0;
            int edgePortIncidences = 0;
            for (Vertex vertex : graph.getVertices()) {
                boolean isSplice = ImplicitCharacteristics.isSplice(vertex, graph);
                for (Port port : vertex.getPorts()) {
                    int numberOfEdgesAtPort = port.getEdges().size();
                    edgePortIncidences += numberOfEdgesAtPort;
                    if (isSplice) {
                        edgeSpliceIncidences += numberOfEdgesAtPort;
                    }
                }
            }
            return (double) edgeSpliceIncidences / (double) edgePortIncidences;
        }));

        // # edges per splice

        allProperties.add(NumberDistributionProperty.createNewProperty("edges/splice", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                if (ImplicitCharacteristics.isSplice(vertex, graph)) {
                    int edgeCount = 0;
                    for (Port port : vertex.getPorts()) {
                        edgeCount += port.getEdges().size();
                    }
                    distribution.add(edgeCount);
                }
            }
            return distribution;
        }));

        // # port groups per vertex

        allProperties.add(NumberDistributionProperty.createNewProperty("portGroups/vertex", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                LinkedHashSet<PortGroup> allContainedPortGroups = new LinkedHashSet<>();
                if (vertex.getPortCompositions() != null) {
                    addAllContainedPortGroupsRecursively(vertex.getPortCompositions(),
                            allContainedPortGroups);
                }
                distribution.add(allContainedPortGroups.size());
            }
            return distribution;
        }));

        // # port groups per port group

        allProperties.add(NumberDistributionProperty.createNewProperty("portGroups/portGroup", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                LinkedHashSet<PortGroup> allContainedPortGroups = new LinkedHashSet<>();
                if (vertex.getPortCompositions() != null) {
                    addAllContainedPortGroupsRecursively(vertex.getPortCompositions(),
                            allContainedPortGroups);
                }
                //count port groups directly contained in each port group
                for (PortGroup portGroup : allContainedPortGroups) {
                    int countPortGroups = 0;
                    for (PortComposition containedPortComposition : portGroup.getPortCompositions()) {
                        if (containedPortComposition instanceof PortGroup) {
                            ++countPortGroups;
                        }
                    }
                    distribution.add(countPortGroups);
                }
            }
            return distribution;
        }));

        // # port group containment depth

        allProperties.add(NumberDistributionProperty.createNewProperty("portGroupContainmentDepth", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                if (vertex.getPortCompositions() != null) {
                    distribution.add(addAllContainedPortGroupsRecursively(vertex.getPortCompositions(),
                            new LinkedHashSet<>()));
                }
            }
            return distribution;
        }));

        // # ports per port group

        allProperties.add(NumberDistributionProperty.createNewProperty("ports/portGroup", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                LinkedHashSet<PortGroup> allContainedPortGroups = new LinkedHashSet<>();
                if (vertex.getPortCompositions() != null) {
                    addAllContainedPortGroupsRecursively(vertex.getPortCompositions(),
                            allContainedPortGroups);
                }
                //count ports directly contained in each port group
                for (PortGroup portGroup : allContainedPortGroups) {
                    int countPorts = 0;
                    for (PortComposition containedPortComposition : portGroup.getPortCompositions()) {
                        if (containedPortComposition instanceof Port) {
                            ++countPorts;
                        }
                    }
                    distribution.add(countPorts);
                }
            }
            return distribution;
        }));

        // # ports not in any port group per vertex

        allProperties.add(NumberDistributionProperty.createNewProperty("portsOutsidePortGroup/vertex", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                int count = 0;
                for (PortComposition portComposition : vertex.getPortCompositions()) {
                    //count only top level ports
                    if (portComposition instanceof Port) {
                        ++count;
                    }
                }
                distribution.add(count);
            }
            return distribution;
        }));

        // # ports per port group over ports per vertex

        allProperties.add(NumberDistributionProperty.createNewProperty("(ports/portGroup)/(ports/vertex)", graph -> {
            NumberDistribution<Double> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                LinkedHashSet<PortGroup> allContainedPortGroups = new LinkedHashSet<>();
                if (vertex.getPortCompositions() != null) {
                    addAllContainedPortGroupsRecursively(vertex.getPortCompositions(),
                            allContainedPortGroups);
                }
                //count ports directly contained in each port group
                for (PortGroup portGroup : allContainedPortGroups) {
                    int countPorts = 0;
                    for (PortComposition containedPortComposition : portGroup.getPortCompositions()) {
                        if (containedPortComposition instanceof Port) {
                            ++countPorts;
                        }
                    }
                    distribution.add((double) countPorts / (double) vertex.getPorts().size());
                }
            }
            return distribution;
        }));

        // # vertex groups per vertex

        allProperties.add(NumberDistributionProperty.createNewProperty("vertexGroups/vertex", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                int count = 0;
                LinkedHashSet<VertexGroup> allContainedVertexGroups = new LinkedHashSet<>();
                addAllContainedVertexGroupsRecursively(graph.getVertexGroups(), allContainedVertexGroups);
                for (VertexGroup vertexGroup : allContainedVertexGroups) {
                    if (vertexGroup.getContainedVertices().contains(vertex)){
                        ++count;
                    }
                }
                distribution.add(count);
            }
            return distribution;
        }));

        // # vertices per vertex group

        allProperties.add(NumberDistributionProperty.createNewProperty("vertices/vertexGroup", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            LinkedHashSet<VertexGroup> allContainedVertexGroups = new LinkedHashSet<>();
            addAllContainedVertexGroupsRecursively(graph.getVertexGroups(), allContainedVertexGroups);
            for (VertexGroup vertexGroup : allContainedVertexGroups) {
                distribution.add(vertexGroup.getContainedVertices().size());
            }
            return distribution;
        }));

        // # vertices connected to the outside per vertex group

        allProperties.add(NumberDistributionProperty.createNewProperty("verticesConnectedToTheOutside/vertexGroup", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            LinkedHashSet<VertexGroup> allContainedVertexGroups = new LinkedHashSet<>();
            addAllContainedVertexGroupsRecursively(graph.getVertexGroups(), allContainedVertexGroups);
            for (VertexGroup vertexGroup : allContainedVertexGroups) {
                int count = 0;
                for (Vertex vertex : vertexGroup.getContainedVertices()) {
                    boolean connectedToTheOutside = false;
                    for (Port port : vertex.getPorts()) {
                        for (Edge edge : port.getEdges()) {
                            for (Port portOfEdge : edge.getPorts()) {
                                Vertex vertexOfEdge = portOfEdge.getVertex();
                                if (vertexOfEdge.getVertexGroup() == null
                                        || !vertexOfEdge.getVertexGroup().equals(vertexGroup)) {
                                    connectedToTheOutside = true;
                                }
                            }
                        }
                    }
                    if (connectedToTheOutside) {
                        ++count;
                    }
                }
                distribution.add(count);
            }
            return distribution;
        }));

        // # vertices not connected to the outside per vertex group

        allProperties.add(NumberDistributionProperty.createNewProperty("verticesWithoutEdge/vertexGroup", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            LinkedHashSet<VertexGroup> allContainedVertexGroups = new LinkedHashSet<>();
            addAllContainedVertexGroupsRecursively(graph.getVertexGroups(), allContainedVertexGroups);
            for (VertexGroup vertexGroup : allContainedVertexGroups) {
                int count = 0;
                for (Vertex vertex : vertexGroup.getContainedVertices()) {
                    boolean hasEdges = false;
                    for (Port port : vertex.getPorts()) {
                        hasEdges |= !port.getEdges().isEmpty();
                    }
                    if (!hasEdges) {
                        ++count;
                    }
                }
                distribution.add(count);
            }
            return distribution;
        }));

        // # vertices not in a touching pair per vertex group

        allProperties.add(NumberDistributionProperty.createNewProperty("verticesNotInATouchingPair/vertexGroup", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            LinkedHashSet<VertexGroup> allContainedVertexGroups = new LinkedHashSet<>();
            addAllContainedVertexGroupsRecursively(graph.getVertexGroups(), allContainedVertexGroups);
            for (VertexGroup vertexGroup : allContainedVertexGroups) {
                int count = 0;
                for (Vertex vertex : vertexGroup.getContainedVertices()) {
                    boolean isContained = false;
                    for (TouchingPair touchingPair : vertexGroup.getTouchingPairs()) {
                        if (touchingPair.getVertices().contains(vertex)) {
                            isContained = true;
                            break;
                        }
                    }
                    if (!isContained) {
                        ++count;
                    }
                }
                distribution.add(count);
            }
            return distribution;
        }));

        // # vertices in more than one touching pair per vertex group

        allProperties.add(NumberDistributionProperty.createNewProperty("verticesInMultipleTouchingPairs/vertexGroup", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            LinkedHashSet<VertexGroup> allContainedVertexGroups = new LinkedHashSet<>();
            addAllContainedVertexGroupsRecursively(graph.getVertexGroups(), allContainedVertexGroups);
            for (VertexGroup vertexGroup : allContainedVertexGroups) {
                int count = 0;
                for (Vertex vertex : vertexGroup.getContainedVertices()) {
                    int countContainmentInTouchingPair = 0;
                    for (TouchingPair touchingPair : vertexGroup.getTouchingPairs()) {
                        if (touchingPair.getVertices().contains(vertex)) {
                            ++countContainmentInTouchingPair;
                        }
                    }
                    if (countContainmentInTouchingPair > 1) {
                        ++count;
                    }
                }
                distribution.add(count);
            }
            return distribution;
        }));

        // # vertices in more than one touching pair and with edges per vertex group

        allProperties.add(NumberDistributionProperty.createNewProperty("verticesInMultipleTouchingPairsHavingEdges/vertexGroup",
                graph -> {
                    NumberDistribution<Integer> distribution = new NumberDistribution<>();
                    LinkedHashSet<VertexGroup> allContainedVertexGroups = new LinkedHashSet<>();
                    addAllContainedVertexGroupsRecursively(graph.getVertexGroups(), allContainedVertexGroups);
                    for (VertexGroup vertexGroup : allContainedVertexGroups) {
                        int count = 0;
                        for (Vertex vertex : vertexGroup.getContainedVertices()) {
                            //determine if vertex is in multiple touching pairs
                            int countContainmentInTouchingPair = 0;
                            for (TouchingPair touchingPair : vertexGroup.getTouchingPairs()) {
                                if (touchingPair.getVertices().contains(vertex)) {
                                    ++countContainmentInTouchingPair;
                                }
                            }
                            //determine if vertex has edges
                            boolean hasEdges = false;
                            for (Port port : vertex.getPorts()) {
                                hasEdges |= !port.getEdges().isEmpty();
                            }
                            //combine
                            if (countContainmentInTouchingPair > 1 && hasEdges) {
                                ++count;
                            }
                        }
                        distribution.add(count);
                    }
                    return distribution;
                }));

        // # port pairings per port

        allProperties.add(NumberDistributionProperty.createNewProperty("portPairings/port", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                for (Port port : vertex.getPorts()) {
                    //now for each port count its port pairings
                    int count = 0;
                    if (vertex.getVertexGroup() != null) {
                        for (PortPairing portPairing : vertex.getVertexGroup().getPortPairings()) {
                            if (portPairing.getPorts().contains(port)) {
                                ++count;
                            }
                        }
                    }
                    distribution.add(count);
                }
            }
            return distribution;
        }));

        // # ports not in a port pairing per vertex group

        allProperties.add(NumberDistributionProperty.createNewProperty("portsNotInAPortPairing/vertexGroup", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            LinkedHashSet<VertexGroup> allContainedVertexGroups = new LinkedHashSet<>();
            addAllContainedVertexGroupsRecursively(graph.getVertexGroups(), allContainedVertexGroups);
            for (VertexGroup vertexGroup : allContainedVertexGroups) {
                int count = 0;
                for (Vertex vertex : vertexGroup.getContainedVertices()) {
                    for (Port port : vertex.getPorts()) {
                        boolean isContained = false;
                        for (PortPairing portPairing : vertexGroup.getPortPairings()) {
                            if (portPairing.getPorts().contains(port)) {
                                isContained = true;
                                break;
                            }
                        }
                        if (!isContained) {
                            ++count;
                        }

                    }
                }
                distribution.add(count);
            }
            return distribution;
        }));

        // # vertex groups per top level vertex group

        allProperties.add(NumberDistributionProperty.createNewProperty("vertexGroups/vertexGroup", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (VertexGroup vertexGroup : graph.getVertexGroups()) {
                LinkedHashSet<VertexGroup> allContainedVertexGroups = new LinkedHashSet<>();
                if (vertexGroup.getContainedVertexGroups() != null) {
                    addAllContainedVertexGroupsRecursively(vertexGroup.getContainedVertexGroups(),
                            allContainedVertexGroups);
                }
                distribution.add(allContainedVertexGroups.size());
            }
            return distribution;
        }));

        // # vertex group containment depth

        allProperties.add(NumberDistributionProperty.createNewProperty("vertexGroupContainmentDepth", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (VertexGroup vertexGroup : graph.getVertexGroups()) {
                if (vertexGroup.getContainedVertexGroups() != null) {
                    distribution.add(addAllContainedVertexGroupsRecursively(vertexGroup.getContainedVertexGroups(),
                            new LinkedHashSet<>()));
                }
            }
            return distribution;
        }));

        // # vertices per top level vertex group

        allProperties.add(NumberDistributionProperty.createNewProperty("vertices/vertexGroup", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (VertexGroup vertexGroup : graph.getVertexGroups()) {
                distribution.add(vertexGroup.getAllRecursivelyContainedVertices().size());
            }
            return distribution;
        }));

        // # edge bundles per top level edge bundle

        allProperties.add(NumberDistributionProperty.createNewProperty("edgeBundles/edgeBundle", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (EdgeBundle edgeBundle : graph.getEdgeBundles()) {
                LinkedHashSet<EdgeBundle> allContainedEdgeBundles = new LinkedHashSet<>();
                if (edgeBundle.getContainedEdgeBundles() != null) {
                    addAllContainedEdgeBundlesRecursively(edgeBundle.getContainedEdgeBundles(),
                            allContainedEdgeBundles);
                }
                distribution.add(allContainedEdgeBundles.size());
            }
            return distribution;
        }));

        // # edge bundle containment depth

        allProperties.add(NumberDistributionProperty.createNewProperty("edgeBundleContainmentDepth", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (EdgeBundle edgeBundle : graph.getEdgeBundles()) {
                if (edgeBundle.getContainedEdgeBundles() != null) {
                    distribution.add(addAllContainedEdgeBundlesRecursively(edgeBundle.getContainedEdgeBundles(),
                            new LinkedHashSet<>()));
                }
            }
            return distribution;
        }));

        // # edges per top level edge bundle

        allProperties.add(NumberDistributionProperty.createNewProperty("edges/edgeBundle", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (EdgeBundle edgeBundle : graph.getEdgeBundles()) {
                LinkedHashSet<EdgeBundle> thisAndAllContainedEdgeBundles = new LinkedHashSet<>();
                thisAndAllContainedEdgeBundles.add(edgeBundle);
                if (edgeBundle.getContainedEdgeBundles() != null) {
                    addAllContainedEdgeBundlesRecursively(edgeBundle.getContainedEdgeBundles(),
                            thisAndAllContainedEdgeBundles);
                }
                //find edges in all these recursively contained edge bundles
                int edgeCount = 0;
                for (EdgeBundle containedEdgeBundle : thisAndAllContainedEdgeBundles) {
                    edgeCount += containedEdgeBundle.getContainedEdges().size();
                }
                distribution.add(edgeCount);
            }
            return distribution;
        }));

        // # hyperedges per top level edgeBundle

        allProperties.add(NumberDistributionProperty.createNewProperty("hyperedges/edgeBundle", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (EdgeBundle edgeBundle : graph.getEdgeBundles()) {
                LinkedHashSet<EdgeBundle> thisAndAllContainedEdgeBundles = new LinkedHashSet<>();
                thisAndAllContainedEdgeBundles.add(edgeBundle);
                if (edgeBundle.getContainedEdgeBundles() != null) {
                    addAllContainedEdgeBundlesRecursively(edgeBundle.getContainedEdgeBundles(),
                            thisAndAllContainedEdgeBundles);
                }
                //find hyperedges in all these recursively contained edge bundles
                int hyperedgeCount = 0;
                for (EdgeBundle containedEdgeBundle : thisAndAllContainedEdgeBundles) {
                    for (Edge containedEdge : containedEdgeBundle.getContainedEdges()) {
                        if (containedEdge.getPorts().size() > 2) {
                            ++hyperedgeCount;
                        }
                    }
                }
                distribution.add(hyperedgeCount);
            }
            return distribution;
        }));

        // # labels / vertex

        allProperties.add(NumberDistributionProperty.createNewProperty("labels/vertex", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                distribution.add(vertex.getLabelManager().getLabels().size());
            }
            return distribution;
        }));

        // # labels / edge

        allProperties.add(NumberDistributionProperty.createNewProperty("labels/edge", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Edge edge : graph.getEdges()) {
                distribution.add(edge.getLabelManager().getLabels().size());
            }
            return distribution;
        }));

        // # labels / vertex group

        allProperties.add(NumberDistributionProperty.createNewProperty("labels/vertexGroup", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            LinkedHashSet<VertexGroup> allVertexGroups = new LinkedHashSet<>();
            for (VertexGroup vertexGroup : graph.getVertexGroups()) {
                allVertexGroups.add(vertexGroup);
                addAllContainedVertexGroupsRecursively(vertexGroup.getContainedVertexGroups(), allVertexGroups);
            }
            for (VertexGroup vertexGroup : allVertexGroups) {
                distribution.add(vertexGroup.getLabelManager().getLabels().size());
            }
            return distribution;
        }));

        // # labels / edge bundle

        allProperties.add(NumberDistributionProperty.createNewProperty("labels/edgeBundle", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            LinkedHashSet<EdgeBundle> allEdgeBundles = new LinkedHashSet<>();
            for (EdgeBundle edgeBundle : graph.getEdgeBundles()) {
                allEdgeBundles.add(edgeBundle);
                addAllContainedEdgeBundlesRecursively(edgeBundle.getContainedEdgeBundles(), allEdgeBundles);
            }
            for (EdgeBundle edgeBundle : allEdgeBundles) {
                distribution.add(edgeBundle.getLabelManager().getLabels().size());
            }
            return distribution;
        }));

        // # labels / port

        allProperties.add(NumberDistributionProperty.createNewProperty("labels/port", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                for (Port port : vertex.getPorts()) {
                    distribution.add(vertex.getLabelManager().getLabels().size());
                }
            }
            return distribution;
        }));


        // # main label text length / vertex

        allProperties.add(NumberDistributionProperty.createNewProperty("mainLabelTextLength/vertex", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                Label mainLabel = vertex.getLabelManager().getMainLabel();
                if (mainLabel instanceof TextLabel) {
                    distribution.add(((TextLabel) mainLabel).getInputText().length());
                }
                else {
                    distribution.add(0);
                }
            }
            return distribution;
        }));

        // # main label text length / edge

        allProperties.add(NumberDistributionProperty.createNewProperty("mainLabelTextLength/edge", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Edge edge : graph.getEdges()) {
                Label mainLabel = edge.getLabelManager().getMainLabel();
                if (mainLabel instanceof TextLabel) {
                    distribution.add(((TextLabel) mainLabel).getInputText().length());
                }
                else {
                    distribution.add(0);
                }
            }
            return distribution;
        }));

        // # main label text length / vertex group

        allProperties.add(NumberDistributionProperty.createNewProperty("mainLabelTextLength/vertexGroup", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            LinkedHashSet<VertexGroup> allVertexGroups = new LinkedHashSet<>();
            for (VertexGroup vertexGroup : graph.getVertexGroups()) {
                allVertexGroups.add(vertexGroup);
                addAllContainedVertexGroupsRecursively(vertexGroup.getContainedVertexGroups(), allVertexGroups);
            }
            for (VertexGroup vertexGroup : allVertexGroups) {
                Label mainLabel = vertexGroup.getLabelManager().getMainLabel();
                if (mainLabel instanceof TextLabel) {
                    distribution.add(((TextLabel) mainLabel).getInputText().length());
                }
                else {
                    distribution.add(0);
                }
            }
            return distribution;
        }));

        // # main label text length / edge bundle

        allProperties.add(NumberDistributionProperty.createNewProperty("mainLabelTextLength/edgeBundle", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            LinkedHashSet<EdgeBundle> allEdgeBundles = new LinkedHashSet<>();
            for (EdgeBundle edgeBundle : graph.getEdgeBundles()) {
                allEdgeBundles.add(edgeBundle);
                addAllContainedEdgeBundlesRecursively(edgeBundle.getContainedEdgeBundles(), allEdgeBundles);
            }
            for (EdgeBundle edgeBundle : allEdgeBundles) {
                Label mainLabel = edgeBundle.getLabelManager().getMainLabel();
                if (mainLabel instanceof TextLabel) {
                    distribution.add(((TextLabel) mainLabel).getInputText().length());
                }
                else {
                    distribution.add(0);
                }
            }
            return distribution;
        }));

        // # main label text length / port

        allProperties.add(NumberDistributionProperty.createNewProperty("mainLabelTextLength/port", graph -> {
            NumberDistribution<Integer> distribution = new NumberDistribution<>();
            for (Vertex vertex : graph.getVertices()) {
                for (Port port : vertex.getPorts()) {
                    Label mainLabel = port.getLabelManager().getMainLabel();
                    if (mainLabel instanceof TextLabel) {
                        distribution.add(((TextLabel) mainLabel).getInputText().length());
                    }
                    else {
                        distribution.add(0);
                    }
                }
            }
            return distribution;
        }));





        return allProperties;
    }

    private static List<Property> createCountVertexTypeProperties() {
        List<Property> allProperties = new ArrayList<>(VertexType.values().length);

        for (int i = 0; i < VertexType.values().length; i++) {
            VertexType vertexType = VertexType.values()[i];
            allProperties.add(NumericalProperty.createNewProperty(vertexType + "Count", graph -> {
                        int count = 0;
                        for (Vertex vertex : graph.getVertices()) {
                            if (ImplicitCharacteristics.isOfType(vertexType, vertex, graph)) {
                                ++count;
                            }
                        }
                        return count;
                    })
            );
        }

        return allProperties;
    }

    private static List<Property> createPortsPerVertexTypeProperties() {
        List<Property> allProperties = new ArrayList<>(VertexType.values().length);

        for (int i = 0; i < VertexType.values().length; i++) {
            VertexType vertexType = VertexType.values()[i];
            allProperties.add(NumberDistributionProperty.createNewProperty("ports/" + vertexType, graph -> {
                        NumberDistribution<Integer> distribution = new NumberDistribution<>();
                        for (Vertex vertex : graph.getVertices()) {
                            if (ImplicitCharacteristics.isOfType(vertexType, vertex, graph)) {
                                distribution.add(vertex.getPorts().size());
                            }
                        }
                        return distribution;
                    })
            );
        }

        return allProperties;
    }

    private static List<Property> createPortGroupsPerVertexTypeProperties() {
        List<Property> allProperties = new ArrayList<>(VertexType.values().length);

        for (int i = 0; i < VertexType.values().length; i++) {
            VertexType vertexType = VertexType.values()[i];
            allProperties.add(NumberDistributionProperty.createNewProperty("portGroups/" + vertexType, graph -> {
                        NumberDistribution<Integer> distribution = new NumberDistribution<>();
                        for (Vertex vertex : graph.getVertices()) {
                            if (ImplicitCharacteristics.isOfType(vertexType, vertex, graph)) {
                                LinkedHashSet<PortGroup> portGroups = new LinkedHashSet<>();
                                addAllContainedPortGroupsRecursively(vertex.getPortCompositions(), portGroups);
                                distribution.add(portGroups.size());
                            }
                        }
                        return distribution;
                    })
            );
        }

        return allProperties;
    }

    private static List<Property> createCountVertexGroupTypeProperties() {
        List<Property> allProperties = new ArrayList<>(VertexGroupType.values().length);

        for (int i = 0; i < VertexGroupType.values().length; i++) {
            VertexGroupType vertexGroupType = VertexGroupType.values()[i];
            allProperties.add(NumericalProperty.createNewProperty(vertexGroupType + "Count", graph -> {
                        int count = 0;
                        for (VertexGroup vertexGroup : graph.getAllRecursivelyContainedVertexGroups()) {
                            if (ImplicitCharacteristics.isOfType(vertexGroupType, vertexGroup, graph)) {
                                ++count;
                            }
                        }
                        return count;
                    })
            );
        }

        return allProperties;
    }

    private static List<Property> createVerticesPerVertexGroupTypeProperties() {
        List<Property> allProperties = new ArrayList<>(VertexGroupType.values().length);

        for (int i = 0; i < VertexGroupType.values().length; i++) {
            VertexGroupType vertexGroupType = VertexGroupType.values()[i];
            allProperties.add(NumberDistributionProperty.createNewProperty("vertices/" + vertexGroupType, graph -> {
                        NumberDistribution<Integer> distribution = new NumberDistribution<>();
                        for (VertexGroup vertexGroup : graph.getAllRecursivelyContainedVertexGroups()) {
                            if (ImplicitCharacteristics.isOfType(vertexGroupType, vertexGroup, graph)) {
                                distribution.add(vertexGroup.getContainedVertices().size());
                            }
                        }
                        return distribution;
                    })
            );
        }

        return allProperties;
    }

    private static List<Property> createPortsPerVertexGroupTypeProperties() {
        List<Property> allProperties = new ArrayList<>(VertexGroupType.values().length);

        for (int i = 0; i < VertexGroupType.values().length; i++) {
            VertexGroupType vertexGroupType = VertexGroupType.values()[i];
            allProperties.add(NumberDistributionProperty.createNewProperty("ports/" + vertexGroupType, graph -> {
                        NumberDistribution<Integer> distribution = new NumberDistribution<>();
                        for (VertexGroup vertexGroup : graph.getAllRecursivelyContainedVertexGroups()) {
                            if (ImplicitCharacteristics.isOfType(vertexGroupType, vertexGroup, graph)) {
                                distribution.add(getPortsOfVertexGroup(vertexGroup).size());
                            }
                        }
                        return distribution;
                    })
            );
        }

        return allProperties;
    }

    private static List<Property> createPortPairingsPerVertexTypeProperties() {
        List<Property> allProperties = new ArrayList<>(VertexType.values().length);

        for (int i = 0; i < VertexType.values().length; i++) {
            VertexType vertexType = VertexType.values()[i];
            allProperties.add(NumberDistributionProperty.createNewProperty("portPairings/" + vertexType, graph -> {
                        NumberDistribution<Integer> distribution = new NumberDistribution<>();
                        for (Vertex vertex : graph.getVertices()) {
                            if (ImplicitCharacteristics.isOfType(vertexType, vertex, graph)) {
                                int numberOfPortPairings = 0;
                                VertexGroup vertexGroup = vertex.getVertexGroup();
                                if (vertexGroup != null) {
                                    //find all port pairings of its group where vertex has a port of
                                    for (PortPairing portPairing : vertexGroup.getPortPairings()) {
                                        if (vertex.getPorts().contains(portPairing.getPort0())
                                                || vertex.getPorts().contains(portPairing.getPort1())) {
                                            ++numberOfPortPairings;
                                        }
                                    }
                                }
                                distribution.add(numberOfPortPairings);
                            }
                        }
                        return distribution;
                    })
            );
        }

        return allProperties;
    }

    private static List<Property> createPortPairingsPerVertexGroupTypeProperties() {
        List<Property> allProperties = new ArrayList<>(VertexGroupType.values().length);

        for (int i = 0; i < VertexGroupType.values().length; i++) {
            VertexGroupType vertexGroupType = VertexGroupType.values()[i];
            allProperties.add(NumberDistributionProperty.createNewProperty("portPairings/" + vertexGroupType, graph -> {
                        NumberDistribution<Integer> distribution = new NumberDistribution<>();
                        for (VertexGroup vertexGroup : graph.getAllRecursivelyContainedVertexGroups()) {
                            if (ImplicitCharacteristics.isOfType(vertexGroupType, vertexGroup, graph)) {
                                distribution.add(vertexGroup.getPortPairings().size());
                            }
                        }
                        return distribution;
                    })
            );
        }

        return allProperties;
    }

    private static List<Property> createUnpairedPortsPerVertexGroupTypeProperties() {
        List<Property> allProperties = new ArrayList<>(VertexGroupType.values().length);

        for (int i = 0; i < VertexGroupType.values().length; i++) {
            VertexGroupType vertexGroupType = VertexGroupType.values()[i];
            allProperties.add(NumberDistributionProperty.createNewProperty("unpairedPorts/" + vertexGroupType,
                    graph -> {
                        NumberDistribution<Integer> distribution = new NumberDistribution<>();
                        for (VertexGroup vertexGroup : graph.getAllRecursivelyContainedVertexGroups()) {
                            if (ImplicitCharacteristics.isOfType(vertexGroupType, vertexGroup, graph)) {
                                Collection<Port> portsOfVertexGroup = getPortsOfVertexGroup(vertexGroup);
                                for (PortPairing portPairing : vertexGroup.getPortPairings()) {
                                    for (Port port : portPairing.getPorts()) {
                                        portsOfVertexGroup.remove(port);
                                    }
                                }
                                distribution.add(portsOfVertexGroup.size());
                            }
                        }
                        return distribution;
                    })
            );
        }

        return allProperties;
    }

    private static List<Property> createEdgesPerRegularPortProperties() {
        int maxOwn = 4;
        List<Property> allProperties = new ArrayList<>(maxOwn + 2);

        for (int i = 0; i <= maxOwn + 1; i++) {
            String propertyName = i + "edges/regularPorts";
            if (i > maxOwn) {
                propertyName = ">" + maxOwn + "edges/regularPorts";
            }
            int finalI = i;
            allProperties.add(NumericalProperty.createNewProperty(propertyName, graph -> {
                int countRegularPortEdgeIncidences = 0;
                for (Vertex vertex : graph.getVertices()) {
                    for (Port port : vertex.getPorts()) {
                        if (ImplicitCharacteristics.isRegularPort(port, graph)) {
                            if (port.getEdges().size() == finalI
                                    || (finalI > maxOwn && port.getEdges().size() > finalI)) {
                                ++countRegularPortEdgeIncidences;
                            }
                        }
                    }
                }
                return (double) countRegularPortEdgeIncidences / (double) countRegularPorts(graph);
            }));
        }
        return allProperties;
    }

    private static List<Property> createDegreeOfHyperedgeProperties() {
        int maxOwn = 15;
        List<Property> allProperties = new ArrayList<>(maxOwn + 2);

        for (int i = 0; i <= maxOwn + 1; i++) {
            String propertyName = "hyperedgesOfDegree" + i;
            if (i > maxOwn) {
                propertyName = "hyperedgesOfDegree>" + maxOwn;
            }
            int finalI = i;
            allProperties.add(NumericalProperty.createNewProperty(propertyName, graph -> {
                int countHyperedgesOfDegreeI = 0;
                for (Edge edge : graph.getEdges()) {
                    if (edge.getPorts().size() == finalI
                            || (finalI > maxOwn && edge.getPorts().size() > finalI)) {
                        ++countHyperedgesOfDegreeI;
                    }
                }
                //make it absolute instead of relative
                return countHyperedgesOfDegreeI; // / (double) graph.getEdges().size();
            }));
        }
        return allProperties;
    }

    private static Collection<Port> getPortsOfVertexGroup(VertexGroup vertexGroup) {
        LinkedHashSet<Port> ports = new LinkedHashSet<>();
        for (Vertex vertex : vertexGroup.getAllRecursivelyContainedVertices()) {
            ports.addAll(vertex.getPorts());
        }
        return ports;
    }

    private static int countRegularPorts(Graph graph) {
        int count = 0;
        for (Vertex vertex : graph.getVertices()) {
            for (Port port : vertex.getPorts()) {
                count += ImplicitCharacteristics.isRegularPort(port, graph) ? 1 : 0;
            }
        }
        return count;
    }
}
