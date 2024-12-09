package de.uniwue.informatik.praline.pseudocircuitplans.generatingnewplans;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.labels.LabeledObject;
import de.uniwue.informatik.praline.datastructure.labels.TextLabel;
import de.uniwue.informatik.praline.datastructure.utils.Serialization;
import de.uniwue.informatik.praline.pseudocircuitplans.properties.*;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.javatuples.Pair;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MainGeneratePlans {

    private final static String PATH_ORIGINAL_PLANS =
//            "data/denkbares_08_06_2021/praline";
//            "data" + File.separator + "praline-readable-2020-09-04";
//            "data" + File.separator + "largest-comp-praline-package-2020-05-18";
//            "data" + File.separator + "praline-package-2020-05-18";
//            "data" + File.separator + "denkbares_08_06_2021-praline";
//            "data" + File.separator + "example-for-visualization-very-small";
//            "data" + File.separator + "example-for-visualization-others2";
            "data" + File.separator + "example-cgta";

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    private final static String PATH_GENERATED_PLANS =
            "data" + File.separator + "generated_" + DATE_FORMAT.format(new Date());

    private final static int NUMBER_OF_GENERATED_PLANS_PER_ORIGINAL = 3;

    private final static int SEED = 945395365;

    private final static Random RANDOM = new Random(SEED);

    /**
     * Portion of original elements that have to be removed.
     * E.g. if this is 0.1, then at least 10 % of the vertices, 10 % of the edges, and so on
     * have to be removed and replaced by new elements (not necessarily the exact the number of elements but
     * something around this)
     */
    private final static double q = 0.05; //0.1;

    private final static int NUMBER_OF_CANDIDATES_FOR_EDGE_INSERTION = 1000;

    private static int generatedPlansCounter = 0;

    public static void main(String[] args) throws IOException {
        File sourceDir = new File(PATH_ORIGINAL_PLANS);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new IOException(PATH_ORIGINAL_PLANS + " is no directory or does not exist. Abort.");
        }
        //go through all files and if it is a json, it should be a circuit plan and we read it
        //by that we collect our data and compute our statistics at first
        DataSetProperties originalPlansProperties = new DataSetProperties();
        for (File file : sourceDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".json") &&
                    (!PATH_ORIGINAL_PLANS.contains("readable") || file.getName().endsWith("-praline.json"))) {
                originalPlansProperties.add(gatherDataOfPlan(file));
            }
        }

        //now compute for each original plan the specified number of artificial new plans and save them
        new File(PATH_GENERATED_PLANS).mkdirs();
        for (PropertySheet originalPlanSheet : originalPlansProperties) {
            for (int i = 0; i < NUMBER_OF_GENERATED_PLANS_PER_ORIGINAL; i++) {
                generateNewPlans(originalPlanSheet, originalPlansProperties);
            }
        }
    }

    private static PropertySheet gatherDataOfPlan(File jsonFile) throws IOException {
        Graph plan = Serialization.read(jsonFile, Graph.class);

        System.out.println("read plan " + jsonFile.getName());

        PropertySheet propertySheet = new PropertySheet(plan);
        for (Property property : PropertyManager.getAllProperties()) {
            propertySheet.addValue(new PropertyValue<>(property, plan));
        }

        return propertySheet;
    }


    /**
     *
     * @param originalPlanSheet
     * @param originalPlansProperties
     * @return
     *      success
     * @throws IOException
     */
    private static boolean generateNewPlans(PropertySheet originalPlanSheet, DataSetProperties originalPlansProperties)
            throws IOException {

        //clone original graph by serializing it and directly deserializing it again
        // TODO: do it more clever without using files
        File dummyFile = new File("target" + File.separator + "dummyFileToBeDeleted.json");
        if (!dummyFile.exists()) {
            dummyFile.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(dummyFile, false);
        BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fos));
        br.write(Serialization.write(originalPlanSheet.getGraph()));
        br.flush();
        br.close();
        fos.flush();
        fos.close();
        Graph newPlan = Serialization.read(dummyFile, Graph.class);
        dummyFile.delete();


        ////////////
        // Phase 0: save current labels
        ////////////
        List<String> vertexLabelTexts =
                newPlan.getVertices().stream().filter(v -> !ImplicitCharacteristics.isSplice(v, newPlan)).map(v ->
                        ((TextLabel) v.getLabelManager().getMainLabel()).getInputText()).collect(Collectors.toList());
        LinkedList<String> vertexLabelTextsUnused = new LinkedList<>(vertexLabelTexts);
        Collections.shuffle(vertexLabelTextsUnused, RANDOM); //create a random order in which the label texts will be re-assigned
        List<String> edgeLabelTexts =
                newPlan.getEdges().stream().map(e -> ((TextLabel) e.getLabelManager().getMainLabel()).getInputText())
                        .collect(Collectors.toList());
        LinkedList<String> edgeLabelTextsUnused = new LinkedList<>(edgeLabelTexts);
        Collections.shuffle(edgeLabelTextsUnused, RANDOM); //create a random order in which the label texts will be re-assigned


        ////////////
        // Phase 1: compute target values of generated plans
        ////////////

        //Compute a random value for all properties. Some make absolutely no sense -- in particular for
        // some StatisticParameters for a NumberDistributionProperty. We will just ignore them.
        Pair<Map<NumericalProperty, Number>, Map<NumberDistributionProperty, Map<StatisticParameter, Number>>>
                targetValueMaps = determineTargetValues(originalPlanSheet, originalPlansProperties);
        Map<NumericalProperty, Number> numericalTargetValues = targetValueMaps.getValue0();
        Map<NumberDistributionProperty, Map<StatisticParameter, Number>> numberDistributionTargetValues =
                targetValueMaps.getValue1();

        //specific target values

        //TODO: modified it -- our target value is now always 1 component and we read from files with only 1 component

        int targetValueConnectedComponents = 1;
//                Math.max(1, numericalTargetValues.get(PropertyManager.getProperty("componentCount")).intValue());

        int targetValueSplices = numericalTargetValues.get(PropertyManager.getProperty("SPLICECount")).intValue();

        int targetValueSoloVertices =
                numericalTargetValues.get(PropertyManager.getProperty("SOLO_VERTEXCount")).intValue();
        int targetValuePortsSoloVertices =
                targetValueSoloVertices == 0 ? 0 : numberDistributionTargetValues.get(PropertyManager.getProperty(
                        "ports/SOLO_VERTEX")).get(StatisticParameter.SUM).intValue();
        double targetValueStandardDeviationPortsPerSoloVertex = numberDistributionTargetValues.get(
                PropertyManager.getProperty("ports/SOLO_VERTEX")).get(StatisticParameter.STANDARD_DEVIATION)
                .doubleValue();

        int targetValueConnectors = numericalTargetValues.get(PropertyManager.getProperty("CONNECTORCount")).intValue();
        int targetValuePortPairingsConnectors =
                targetValueConnectors == 0 ? 0 : numberDistributionTargetValues.get(PropertyManager.getProperty(
                "portPairings/CONNECTOR")).get(StatisticParameter.SUM).intValue();
        double targetValueStandardDeviationPortPairingsPerConnector = numberDistributionTargetValues.get(
                PropertyManager.getProperty("portPairings/CONNECTOR")).get(StatisticParameter.STANDARD_DEVIATION)
                .doubleValue();
        int targetValueUnpairedPortsConnectors =
                targetValueConnectors == 0 ? 0 : numberDistributionTargetValues.get(PropertyManager.getProperty(
                "unpairedPorts/CONNECTOR")).get(StatisticParameter.SUM).intValue();

        int targetValueDeviceConnectors =
                numericalTargetValues.get(PropertyManager.getProperty("DEVICE_CONNECTORCount")).intValue();
        /**
         * That's the sum of device vertices + device connector vertices.
         * So do not confuse it with targetValueDeviceConnectorVertices
         */
        int targetValueVerticesDeviceConnectors = numberDistributionTargetValues.get(PropertyManager.getProperty(
                "vertices/DEVICE_CONNECTOR")).get(StatisticParameter.SUM).intValue();
        int targetValueDeviceVertices = targetValueDeviceConnectors;
        /**
         * do not confuse with targetValueVerticesDeviceConnectors (see there for more)
         */
        int targetValueDeviceConnectorVertices = targetValueVerticesDeviceConnectors - targetValueDeviceVertices;
        double targetValueStandardDeviationVerticesPerDeviceConnector = numberDistributionTargetValues.get(
                PropertyManager.getProperty("vertices/DEVICE_CONNECTOR")).get(StatisticParameter.STANDARD_DEVIATION)
                .doubleValue();
        int targetValuePortPairingsDeviceConnectors =
                targetValueDeviceVertices == 0 ? 0 : numberDistributionTargetValues.get(PropertyManager.getProperty(
                "portPairings/DEVICE_CONNECTOR")).get(StatisticParameter.SUM).intValue();
        double targetValueStandardDeviationPortPairingsPerDeviceConnectorVertex = numberDistributionTargetValues.get(
                PropertyManager.getProperty("portPairings/DEVICE_CONNECTOR_VERTEX"))
                .get(StatisticParameter.STANDARD_DEVIATION).doubleValue();
        int targetValueUnpairedPortsDeviceConnectors =
                targetValueDeviceVertices == 0 ? 0 : numberDistributionTargetValues.get(PropertyManager.getProperty(
                "unpairedPorts/DEVICE_CONNECTOR")).get(StatisticParameter.SUM).intValue();


        int targetValueRegularPorts = targetValuePortsSoloVertices +
                2 * targetValuePortPairingsConnectors + targetValueUnpairedPortsConnectors +
                targetValuePortPairingsDeviceConnectors + targetValueUnpairedPortsDeviceConnectors;

        int portEdgeIncidences = 0;
        int targetValueIEdgesPerRegularPort[] = new int[5];
        int assignedRegularPorts = 0;
        for (int i = 0; i < targetValueIEdgesPerRegularPort.length; i++) {
            if (i != 1) {
                targetValueIEdgesPerRegularPort[i] = (int) Math.round((double) targetValueRegularPorts *
                        numericalTargetValues.get(PropertyManager.getProperty(i + "edges/regularPorts")).doubleValue());
                portEdgeIncidences += i * targetValueIEdgesPerRegularPort[i];
                assignedRegularPorts += targetValueIEdgesPerRegularPort[i];
            }
        }
        //the rest of the regular ports will have 1 edge -- this should be the overwhelming majority
        targetValueIEdgesPerRegularPort[1] = targetValueRegularPorts - assignedRegularPorts;
        portEdgeIncidences += targetValueIEdgesPerRegularPort[1];

        int targetValueSpliceEdgeIncidences =
                (int) Math.round((double) targetValueSplices * numberDistributionTargetValues.get(
                PropertyManager.getProperty("edges/splice")).get(StatisticParameter.MEAN).doubleValue());
        portEdgeIncidences += targetValueSpliceEdgeIncidences;

        int targetValueHyperedgesOfDegreeI[] = new int[20];
        targetValueHyperedgesOfDegreeI[0] = 0;
        targetValueHyperedgesOfDegreeI[1] = 0;
        int targetValueEdges = 0;
        for (int i = 3; i < targetValueHyperedgesOfDegreeI.length; i++) {
            targetValueHyperedgesOfDegreeI[i] =
                    numericalTargetValues.get(PropertyManager.getProperty("hyperedgesOfDegree" + i)).intValue();
            portEdgeIncidences -= i * targetValueHyperedgesOfDegreeI[i];
            targetValueEdges += targetValueHyperedgesOfDegreeI[i];
        }
        //we may add an additional degree-3-edge if there is an odd number of port--edge incidences remaining
        if (portEdgeIncidences % 2 == 1) {
            ++targetValueHyperedgesOfDegreeI[3];
            portEdgeIncidences -= 3;
            targetValueEdges++;
        }
        // all other edges will have degree 2 -- this should be the overwhelming majority
        targetValueHyperedgesOfDegreeI[2] = Math.max(0, portEdgeIncidences / 2);
        targetValueEdges += targetValueHyperedgesOfDegreeI[2];

        double targetValueMeanParallelEdges = numberDistributionTargetValues.get(
                PropertyManager.getProperty("parallelEdges")).get(StatisticParameter.MEAN).doubleValue();

        int targetValueSelfLoops = numericalTargetValues.get(PropertyManager.getProperty("selfLoopEdges")).intValue();

//        //check for degenerate case and fix it TODO check degree of splice and potentially guarantee deg >= 2?
//        if (targetValueSpliceEdgeIncidences < targetValueSplices) {
//            //make sure the new incidences are an even number
//            int missingIncidences =
//                    (int) Math.ceil((double) (targetValueSplices - targetValueSpliceEdgeIncidences) / 2.0) * 2;
//            targetValueSpliceEdgeIncidences += missingIncidences;
//            targetValuePortEdgeIncidences += missingIncidences;
//            targetValueEdges += missingIncidences / 2;
//        }

        ////////////
        // Phase 2: remove a portion of q of all elements
        ////////////

        // 0. remove all connected components of size 1, i.e., isolated vertices. Why they are there at all?
        Set<Set<JungUtils.PseudoVertex>> connectedComponents = JungUtils.getConnectedComponents(newPlan);
        for (Set<JungUtils.PseudoVertex> connectedComponent : connectedComponents) {
            if (connectedComponent.size() == 1) {
                for (JungUtils.PseudoVertex pseudoVertex : connectedComponent) {
                    if (pseudoVertex.getVertexObject() instanceof Vertex) {
                        removeVertexCleanly(newPlan, (Vertex) pseudoVertex.getVertexObject());
                    }
                    else if (pseudoVertex.getVertexObject() instanceof VertexGroup) {
                        VertexGroup vertexGroup = (VertexGroup) pseudoVertex.getVertexObject();
                        for (Vertex containedVertex :
                                new ArrayList<>(vertexGroup.getAllRecursivelyContainedVertices())) {
                            removeVertexCleanly(newPlan, containedVertex);
                        }
                        newPlan.removeVertexGroup(vertexGroup);
                    }
                }
                //subtract this component from the target value
                targetValueConnectedComponents = Math.max(1, targetValueConnectedComponents - 1);
            }
        }

        //specify vertices and vertex groups

        Map<VertexType, List<Vertex>> verticesByType = separateVertices(newPlan);
        List<Vertex> splices = verticesByType.get(VertexType.SPLICE);
        List<Vertex> soloVertices = verticesByType.get(VertexType.SOLO_VERTEX);

        Map<VertexGroupType, List<VertexGroup>> vertexGroupsByType = separateVertexGroups(newPlan);
        List<VertexGroup> connectors = vertexGroupsByType.get(VertexGroupType.CONNECTOR);
        List<VertexGroup> deviceConnectors = vertexGroupsByType.get(VertexGroupType.DEVICE_CONNECTOR);

        // A. splices

        //all splices set empty text for port labels
        for (Vertex splice : splices) {
            for (Port port : splice.getPorts()) {
                ((TextLabel) port.getLabelManager().getMainLabel()).setInputText("");
            }
        }
        //remove q splices
        int numberSplicesRemoved = Math.max((int) ((double) splices.size() * q + 1.0),
                splices.size() - targetValueSplices);
        List<Vertex> splicesToBeRemoved = selectRandomly(splices, numberSplicesRemoved);
        for (Vertex splice : splicesToBeRemoved) {
            removeVertexCleanly(newPlan, splice);
            splices.remove(splice);
        }

        // B. solo vertices

        int numberSoloVerticesRemoved = Math.max((int) ((double) soloVertices.size() * q + 1.0),
                soloVertices.size() - targetValueSoloVertices);
        List<Vertex> soloVerticesToBeRemoved = selectRandomly(soloVertices, numberSoloVerticesRemoved);
        for (Vertex vertex : soloVerticesToBeRemoved) {
            removeVertexCleanly(newPlan, vertex);
            soloVertices.remove(vertex);
        }
        //remove q ports from solo vertices
        //first find all ports of solo vertices
        ArrayList<Port> portsSoloVertices = new ArrayList<>();
        //for every solo vertex keep randomly 1 port -- we must not have vertices without ports
        ArrayList<Port> soloPortsForRemoval = new ArrayList<>();
        for (Vertex v : soloVertices) {
            ArrayList<Port> portsOfV = new ArrayList<>(v.getPorts());
            portsSoloVertices.addAll(portsOfV);
            soloPortsForRemoval.addAll(selectRandomly(portsOfV, Math.max(0, portsOfV.size() - 1)));
        }
        int numberPortsSoloVerticesRemoved = Math.max((int) ((double) portsSoloVertices.size() * q + 1.0),
                soloPortsForRemoval.size() - targetValuePortsSoloVertices);
        List<Port> portsToBeRemoved = selectRandomly(soloPortsForRemoval, numberPortsSoloVerticesRemoved);
        for (Port p : portsToBeRemoved) {
            portsSoloVertices.remove(p);
            removePortCleanly(newPlan, p);
        }

        // C. connectors

        int numberConnectorsRemoved = Math.max((int) ((double) connectors.size() * q + 1.0),
                connectors.size() - targetValueConnectors);
        List<VertexGroup> connectorsToBeRemoved = selectRandomly(connectors, numberConnectorsRemoved);
        for (VertexGroup connector : connectorsToBeRemoved) {
            newPlan.removeVertexGroup(connector);
            for (Vertex containedVertex : connector.getContainedVertices()) {
                removeVertexCleanly(newPlan, containedVertex);
            }
            connectors.remove(connector);
        }
        //remove q port pairings from connectors
        //first find all port pairings of connectors
        ArrayList<PortPairing> portPairingsConnectors = new ArrayList<>();
        LinkedHashSet<Port> pairedPorts = new LinkedHashSet<>();
        //for every connector keep randomly 1 port pairing -- we must not have connectors without port pairings
        ArrayList<PortPairing> portPairingsConnectorsForRemoval = new ArrayList<>();
        for (VertexGroup connector : connectors) {
            ArrayList<PortPairing> portPairingsOfThisConnector = new ArrayList<>(connector.getPortPairings());
            portPairingsConnectors.addAll(portPairingsOfThisConnector);
            for (PortPairing portPairing : portPairingsOfThisConnector) {
                pairedPorts.addAll(portPairing.getPorts());
            }
            portPairingsConnectorsForRemoval.addAll(selectRandomly(portPairingsOfThisConnector, Math.max(0,
                    portPairingsOfThisConnector.size() - 1)));
        }
        int numberPortPairingsConnectorsRemoved = Math.max((int) ((double) portPairingsConnectors.size() * q + 1.0),
                portPairingsConnectors.size() - targetValuePortPairingsConnectors);
        List<PortPairing> connectorPortPairingsToBeRemoved = selectRandomly(portPairingsConnectorsForRemoval,
                numberPortPairingsConnectorsRemoved);
        for (PortPairing pp : connectorPortPairingsToBeRemoved) {
            portPairingsConnectors.remove(pp);
            //find port pairing within all vertex groups
            for (VertexGroup vertexGroup : newPlan.getVertexGroups()) {
                if (vertexGroup.getPortPairings().contains(pp)) {
                    //remove reference to port pairing
                    vertexGroup.removePortPairing(pp);
                    //and remove both individual ports
                    for (Port port : pp.getPorts()) {
                        removePortCleanly(newPlan, port);
                        pairedPorts.remove(port);
                    }
                    break;
                }
            }
        }
        //remove q unpaired ports from connectors
        //first find all unpaired port of connectors
        ArrayList<Port> unpairedPortsConnectors = new ArrayList<>();
        for (VertexGroup connector : connectors) {
            for (Vertex connectorVertex : connector.getContainedVertices()) {
                for (Port port : connectorVertex.getPorts()) {
                    if (!pairedPorts.contains(port)) {
                        unpairedPortsConnectors.add(port);
                    }
                }
            }
        }
        int numberUnpairedPortsConnectorsRemoved = Math.max((int) ((double) unpairedPortsConnectors.size() * q + 1.0),
                unpairedPortsConnectors.size() - targetValueUnpairedPortsConnectors);
        List<Port> unpairedPortsToBeRemoved = selectRandomly(unpairedPortsConnectors,
                numberUnpairedPortsConnectorsRemoved);
        for (Port p : unpairedPortsToBeRemoved) {
            unpairedPortsConnectors.remove(p);
            removePortCleanly(newPlan, p);
        }

        // D. device connectors

        int numberDeviceConnectorsRemoved = Math.max((int) ((double) deviceConnectors.size() * q + 1.0),
                deviceConnectors.size() - targetValueDeviceConnectors);
        List<VertexGroup> deviceConnectorsToBeRemoved = selectRandomly(deviceConnectors, numberDeviceConnectorsRemoved);
        for (VertexGroup deviceConnector : deviceConnectorsToBeRemoved) {
            newPlan.removeVertexGroup(deviceConnector);
            for (Vertex containedVertex : deviceConnector.getContainedVertices()) {
                removeVertexCleanly(newPlan, containedVertex);
            }
            deviceConnectors.remove(deviceConnector);
        }
        //remove q device connector vertices from device connectors
        //first find all device connector vertices
        ArrayList<Vertex> allDeviceConnectorVertices = new ArrayList<>();
        ArrayList<Vertex> allDeviceVertices = new ArrayList<>();
        ArrayList<Vertex> allDeviceConnectorVerticesForRemoval = new ArrayList<>();
        for (VertexGroup deviceConnector : deviceConnectors) {
            ArrayList<Vertex> deviceConnectorVerticesOfThisDeviceConnector = new ArrayList<>();
            for (Vertex containedVertex : deviceConnector.getContainedVertices()) {
                if (ImplicitCharacteristics.isOfType(VertexType.DEVICE_CONNECTOR_VERTEX, containedVertex, newPlan)) {
                    deviceConnectorVerticesOfThisDeviceConnector.add(containedVertex);
                }
                else if (ImplicitCharacteristics.isOfType(VertexType.DEVICE_VERTEX, containedVertex, newPlan)) {
                    allDeviceVertices.add(containedVertex);
                }
            }
            allDeviceConnectorVertices.addAll(deviceConnectorVerticesOfThisDeviceConnector);
            allDeviceConnectorVerticesForRemoval.addAll(deviceConnectorVerticesOfThisDeviceConnector);
            allDeviceConnectorVerticesForRemoval.remove(selectRandomly(deviceConnectorVerticesOfThisDeviceConnector,
                    1).get(0));
        }
        int numberDeviceConnectorVerticesRemoved =
                Math.max((int) ((double) allDeviceConnectorVertices.size() * q + 1.0),
                        allDeviceConnectorVertices.size() - targetValueDeviceConnectors);
        List<Vertex> deviceConnectorVerticesToBeRemoved = selectRandomly(allDeviceConnectorVerticesForRemoval,
                numberDeviceConnectorVerticesRemoved);
        for (Vertex deviceConnectorToBeRemoved : deviceConnectorVerticesToBeRemoved) {
            allDeviceConnectorVertices.remove(deviceConnectorToBeRemoved);
            removeVertexCleanly(newPlan, deviceConnectorToBeRemoved);
        }
        //remove q port pairings from device connectors
        //first find all port pairings of device connectors
        ArrayList<PortPairing> portPairingsDeviceConnectors = new ArrayList<>();
        ArrayList<PortPairing> portPairingsDeviceConnectorsForRemoval = new ArrayList<>();
        for (VertexGroup deviceConnector : deviceConnectors) {
            ArrayList<PortPairing> portPairingsOfThisDeviceConnector =
                    new ArrayList<>(deviceConnector.getPortPairings());
            portPairingsDeviceConnectors.addAll(portPairingsOfThisDeviceConnector);
            for (PortPairing portPairing : portPairingsOfThisDeviceConnector) {
                pairedPorts.addAll(portPairing.getPorts());
            }
            //for each device connector vertex, we must keep it connected to its device vertex -- so exclude one port
            // pairing for each device connector vertex
            portPairingsDeviceConnectorsForRemoval.addAll(portPairingsOfThisDeviceConnector);
            for (Vertex containedVertex : deviceConnector.getContainedVertices()) {
                if (ImplicitCharacteristics.isOfType(VertexType.DEVICE_CONNECTOR_VERTEX, containedVertex, newPlan)) {
                    ArrayList<Port> pairedPortsOfThisVertex = new ArrayList<>();
                    for (Port port : containedVertex.getPorts()) {
                        if (pairedPorts.contains(port)) {
                            pairedPortsOfThisVertex.add(port);
                        }
                    }
                    PortPairing toBeKept = getPortPairing(selectRandomly(pairedPortsOfThisVertex, 1).get(0),
                            deviceConnector);
                    portPairingsDeviceConnectorsForRemoval.remove(toBeKept);
                }
            }
        }
        int numberPortPairingsDeviceConnectorsRemoved =
                Math.max((int) ((double) portPairingsDeviceConnectors.size() * q + 1.0),
                        portPairingsDeviceConnectors.size() - targetValuePortPairingsDeviceConnectors);
        List<PortPairing> deviceConnectorPortPairingsToBeRemoved = selectRandomly(portPairingsDeviceConnectorsForRemoval,
                numberPortPairingsDeviceConnectorsRemoved);
        for (PortPairing pp : deviceConnectorPortPairingsToBeRemoved) {
            portPairingsDeviceConnectors.remove(pp);
            //find port pairing within all vertex groups
            for (VertexGroup vertexGroup : newPlan.getVertexGroups()) {
                if (vertexGroup.getPortPairings().contains(pp)) {
                    //remove reference to port pairing
                    vertexGroup.removePortPairing(pp);
                    //and remove both individual ports
                    for (Port port : pp.getPorts()) {
                        removePortCleanly(newPlan, port);
                        pairedPorts.remove(port);
                    }
                    break;
                }
            }
        }
        //remove q unpaired ports from device connectors
        //first find all unpaired port of device connectors
        ArrayList<Port> unpairedPortsDeviceConnectors = new ArrayList<>();
        for (VertexGroup deviceConnector : deviceConnectors) {
            for (Vertex vertex : deviceConnector.getContainedVertices()) {
                for (Port port : vertex.getPorts()) {
                    if (!pairedPorts.contains(port)) {
                        unpairedPortsDeviceConnectors.add(port);
                    }
                }
            }
        }
        int numberUnpairedPortsDeviceConnectorsRemoved =
                Math.max((int) ((double) unpairedPortsDeviceConnectors.size() * q + 1.0),
                unpairedPortsDeviceConnectors.size() - targetValueUnpairedPortsDeviceConnectors);
        unpairedPortsToBeRemoved = selectRandomly(unpairedPortsDeviceConnectors,
                numberUnpairedPortsDeviceConnectorsRemoved);
        for (Port p : unpairedPortsToBeRemoved) {
            unpairedPortsDeviceConnectors.remove(p);
            removePortCleanly(newPlan, p);
        }

        // E. remove q edges

        //remove all edges of degree at most 1 (they should not even be there! what are they actually?!)
        for (Edge edge : new ArrayList<>(newPlan.getEdges())) {
            if (edge.getPorts().size() <= 1) {
                newPlan.removeEdge(edge);
            }
        }
        //select q of all edges for removal
        LinkedList<Edge> edges = new LinkedList<>(newPlan.getEdges());
        int numberEdgesRemoved = Math.max((int) ((double) edges.size() * q + 1.0),
                edges.size() - targetValueEdges);
        List<Edge> edgesToBeRemoved = selectRandomly(newPlan.getEdges(), numberEdgesRemoved);

        //check for all degrees of hyperedges that we do not have too many
        LinkedHashMap<Integer, List<Edge>> edgesOfDegI = new LinkedHashMap<>();
        for (int i = 0; i < targetValueHyperedgesOfDegreeI.length; i++) {
            edgesOfDegI.put(i, new ArrayList<>());
        }
        for (Edge edge : edges) {
            int degree = edge.getPorts().size();
            if (degree < targetValueHyperedgesOfDegreeI.length) {
                edgesOfDegI.get(degree).add(edge);
            }
            else {
                System.out.println("Warning! Skipped edge " + edge + " of degree " + degree + ".");
            }
        }
        for (int i = 0; i < targetValueHyperedgesOfDegreeI.length; i++) {
            List<Edge> edgesOfThisDeg = edgesOfDegI.get(i);
            if (edgesOfThisDeg.size() > targetValueHyperedgesOfDegreeI[i]) {
                edgesToBeRemoved.addAll(selectRandomly(edgesOfThisDeg,
                        edgesOfThisDeg.size() - targetValueHyperedgesOfDegreeI[i]));
            }
        }

        //remove edges
        for (Edge e : edgesToBeRemoved) {
            newPlan.removeEdge(e);
            edges.remove(e);
        }


        // F. dissolve q edge bundles

        //TODO: for now we just remove all edge bundles. This step has to be added later!
        for (EdgeBundle edgeBundle : new ArrayList<>(newPlan.getEdgeBundles())) {
            newPlan.removeEdgeBundle(edgeBundle); //change this for-loop!
        }

        // G. change labels

        changeLabelsRandomly(newPlan.getVertices(), splices, vertexLabelTextsUnused, vertexLabelTexts);
        changeLabelsRandomly(newPlan.getEdges(), null, edgeLabelTextsUnused, edgeLabelTexts);

        ////////////
        // Phase 3: insert new elements to reach the target values (precisely or approximately)
        ////////////

        // A. splices

        //add splices to reach the target value
        while (splices.size() < targetValueSplices) {
            //init every splice with exactly 1 port
            TextLabel portLabel = new TextLabel(""); //splices have empty label text
            Port splicePort = new Port(Collections.emptyList(), Collections.singleton(portLabel));
            TextLabel spliceLabel = new TextLabel(ImplicitCharacteristics.SPLICE_LABEL);
            Vertex newSplice = new Vertex(Collections.singleton(splicePort), Collections.singleton(spliceLabel));
            splices.add(newSplice);
            newPlan.addVertex(newSplice);
        }

        // B. solo vertices

        while (soloVertices.size() < targetValueSoloVertices) {
            TextLabel vertexLabel = new TextLabel(getNextLabelText(vertexLabelTextsUnused, vertexLabelTexts));
            Vertex newSoloVertex = new Vertex(null, Collections.singleton(vertexLabel));
            //init every vertex with at least one port
            int numberOfNewPorts = determineNumberOfNewElements(targetValuePortsSoloVertices, targetValueSoloVertices,
                    portsSoloVertices.size(), soloVertices.size(), targetValueStandardDeviationPortsPerSoloVertex);
            for (int i = 0; i < numberOfNewPorts; i++) {
                addNewPort(newSoloVertex, portsSoloVertices);
            }

            soloVertices.add(newSoloVertex);
            newPlan.addVertex(newSoloVertex);
        }
        //add ports randomly to solo vertices
        //TODO do something more clever than random, so strive for a specific distribution that is similar to an
        // original distribution
        while (portsSoloVertices.size() < targetValuePortsSoloVertices && !soloVertices.isEmpty()) {
            Vertex soloVertex = selectRandomly(soloVertices, 1).get(0);
            addNewPort(soloVertex, portsSoloVertices);
        }
        //reset port label text of solo vertices
        for (Vertex soloVertex : soloVertices) {
            resetPortLabelText(soloVertex, 0);
        }

        // C. connectors

        while (connectors.size() < targetValueConnectors) {
            ArrayList<Vertex> verticesOfNewConnector = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                TextLabel vertexLabel = new TextLabel(getNextLabelText(vertexLabelTextsUnused, vertexLabelTexts));
                Vertex newConnectorVertex = new Vertex(null, Collections.singleton(vertexLabel));
                verticesOfNewConnector.add(newConnectorVertex);
                newPlan.addVertex(newConnectorVertex);
            }
            VertexGroup newConnector = new VertexGroup(verticesOfNewConnector);
            newConnector.addTouchingPair(new TouchingPair(verticesOfNewConnector.get(0),
                    verticesOfNewConnector.get(1)));
            //init every connector with at least one port pairing
            int numberOfNewPortPairings = determineNumberOfNewElements(targetValuePortPairingsConnectors,
                    targetValueConnectors, portPairingsConnectors.size(), connectors.size(),
                    targetValueStandardDeviationPortPairingsPerConnector);
            for (int i = 0; i < numberOfNewPortPairings; i++) {
                portPairingsConnectors.add(addNewPortPairing(newConnector, verticesOfNewConnector, pairedPorts,
                        newPlan));
            }

            connectors.add(newConnector);
            newPlan.addVertexGroup(newConnector);
        }
        //add port pairings randomly to connectors
        //TODO do something more clever than random, so strive for a specific distribution that is similar to an
        // original distribution
        while (portPairingsConnectors.size() < targetValuePortPairingsConnectors && !connectors.isEmpty()) {
            VertexGroup connector = selectRandomly(connectors, 1).get(0);
            portPairingsConnectors.add(
                    addNewPortPairing(connector, connector.getContainedVertices(), pairedPorts, newPlan));
        }
        //add unpaired ports randomly to connector vertices
        //TODO do something more clever than random, so strive for a specific distribution that is similar to an
        // original distribution
        while (unpairedPortsConnectors.size() < targetValueUnpairedPortsConnectors && !connectors.isEmpty()) {
            VertexGroup connector = selectRandomly(connectors, 1).get(0);
            Vertex connectorVertex = selectRandomly(connector.getContainedVertices(), 1).get(0);
            addNewPort(connectorVertex, unpairedPortsConnectors);
        }
        //reset port label text of connectors
        for (VertexGroup connector : connectors) {
            resetPortLabelText(connector, 0, unpairedPortsConnectors);
        }

        // D. device connectors

        while (deviceConnectors.size() < targetValueDeviceConnectors) {
            //init every device connector with at least one device connector vertex
            int numberOfNewVerticesOfDeviceConnector =
                    determineNumberOfNewElements(targetValueVerticesDeviceConnectors, targetValueDeviceConnectors,
                            allDeviceVertices.size() + allDeviceConnectorVertices.size(),
                            deviceConnectors.size(), targetValueStandardDeviationVerticesPerDeviceConnector);

            //create device vertex of new device connector
            TextLabel deviceLabel = new TextLabel(getNextLabelText(vertexLabelTextsUnused, vertexLabelTexts));
            Vertex newDeviceVertex = new Vertex(null, Collections.singleton(deviceLabel));
            newPlan.addVertex(newDeviceVertex);
            allDeviceVertices.add(newDeviceVertex);

            VertexGroup newDeviceConnector = new VertexGroup(Collections.singleton(newDeviceVertex));
            deviceConnectors.add(newDeviceConnector);
            newPlan.addVertexGroup(newDeviceConnector);

            //create device connector vertices of new device connector
            for (int i = 0; i < Math.max(1, numberOfNewVerticesOfDeviceConnector - 1); i++) {
                TextLabel deviceConnectorVertexLabel = new TextLabel(
                        getNextLabelText(vertexLabelTextsUnused, vertexLabelTexts));
                Vertex newDeviceConnectorVertex = new Vertex(null, Collections.singleton(deviceConnectorVertexLabel));
                newPlan.addVertex(newDeviceConnectorVertex);

                newDeviceConnector.addVertex(newDeviceConnectorVertex);
                newDeviceConnector.addTouchingPair(new TouchingPair(newDeviceVertex, newDeviceConnectorVertex));


                //init every device connector vertex with at least one port pairing
                int numberOfNewPortPairings = determineNumberOfNewElements(targetValuePortPairingsDeviceConnectors,
                        targetValueDeviceConnectorVertices, portPairingsDeviceConnectors.size(),
                        allDeviceConnectorVertices.size(), targetValueStandardDeviationPortPairingsPerDeviceConnectorVertex);
                for (int j = 0; j < numberOfNewPortPairings; j++) {
                    portPairingsDeviceConnectors.add(addNewPortPairing(newDeviceConnector,
                            Arrays.asList(newDeviceVertex, newDeviceConnectorVertex), pairedPorts, newPlan));
                }

                allDeviceConnectorVertices.add(newDeviceConnectorVertex);
            }
        }
        //add new device connector vertices to device connectors
        while (allDeviceConnectorVertices.size() < targetValueDeviceConnectorVertices && !allDeviceVertices.isEmpty()) {
            //find device vertex where we add the device connector vertex to
            Vertex deviceVertex = selectRandomly(allDeviceVertices, 1).get(0);
            VertexGroup deviceConnector = deviceVertex.getVertexGroup();
            //create new device connector vertex
            Vertex newDeviceConnectorVertex = new Vertex(null,
                    Collections.singleton(new TextLabel(getNextLabelText(vertexLabelTextsUnused, vertexLabelTexts))));
            newPlan.addVertex(newDeviceConnectorVertex);
            allDeviceConnectorVertices.add(newDeviceConnectorVertex);
            deviceConnector.addVertex(newDeviceConnectorVertex);
            deviceConnector.addTouchingPair(new TouchingPair(deviceVertex, newDeviceConnectorVertex));
            //add a port pairing between them
            portPairingsDeviceConnectors.add(addNewPortPairing(deviceConnector, Arrays.asList(deviceVertex,
                    newDeviceConnectorVertex), pairedPorts, newPlan));
        }
        //add port pairings randomly to device connectors
        //TODO do something more clever than random, so strive for a specific distribution that is similar to an
        // original distribution
        while (portPairingsDeviceConnectors.size() < targetValuePortPairingsDeviceConnectors
                && !deviceConnectors.isEmpty()) {
            //find device vertex where we add the device connector vertex to
            Vertex deviceVertex = selectRandomly(allDeviceVertices, 1).get(0);
            VertexGroup deviceConnector = deviceVertex.getVertexGroup();
            Vertex deviceConnectorVertex = null;
            while (deviceConnectorVertex == null || deviceConnectorVertex.equals(deviceVertex)) {
                deviceConnectorVertex = selectRandomly(deviceConnector.getContainedVertices(), 1).get(0);
            }
            portPairingsDeviceConnectors.add(addNewPortPairing(deviceConnector,  Arrays.asList(deviceVertex,
                    deviceConnectorVertex), pairedPorts, newPlan));
        }
        //add unpaired ports randomly to vertices of device connector
        //TODO do something more clever than random, so strive for a specific distribution that is similar to an
        // original distribution
        while (unpairedPortsDeviceConnectors.size() < targetValueUnpairedPortsDeviceConnectors
                && !deviceConnectors.isEmpty()) {
            VertexGroup deviceConnector = selectRandomly(deviceConnectors, 1).get(0);
            Vertex vertexOfDeviceConnector = selectRandomly(deviceConnector.getContainedVertices(), 1).get(0);
            addNewPort(vertexOfDeviceConnector, unpairedPortsDeviceConnectors);
        }
        //reset port label text of connectors
        for (VertexGroup deviceConnector : deviceConnectors) {
            resetPortLabelText(deviceConnector, 0, unpairedPortsDeviceConnectors);
        }

        // E. insert edges

        int missingEdges = targetValueEdges - edges.size();
        //find ports without an edge, they are candidates to get new edges
        List<Port> regularPortsWithoutEdgeUnassigned = new ArrayList<>();
        Map<Integer, List<Port>> numberOfEdges2RegularPort = new LinkedHashMap<>();
        for (int i = 1; i < targetValueIEdgesPerRegularPort.length; i++) {
            numberOfEdges2RegularPort.put(i, new ArrayList<>());
        }
        //add ports of solo vertices
        for (Port port : portsSoloVertices) {
            if (port.getEdges().isEmpty()) {
                regularPortsWithoutEdgeUnassigned.add(port);
            }
            else {
                numberOfEdges2RegularPort.get(port.getEdges().size()).add(port);
            }
        }
        //add paired and unpaired ports of connectors
        for (VertexGroup connector : connectors) {
            for (Vertex connectorVertex : connector.getContainedVertices()) {
                for (Port port : connectorVertex.getPorts()) {
                    if (port.getEdges().isEmpty()) {
                        regularPortsWithoutEdgeUnassigned.add(port);
                    }
                    else {
                        numberOfEdges2RegularPort.get(port.getEdges().size()).add(port);
                    }
                }
            }
        }
        //add paired and unpaired ports of device connector vertices
        for (Vertex deviceConnectorVertex : allDeviceConnectorVertices) {
            for (Port port : deviceConnectorVertex.getPorts()) {
                if (port.getEdges().isEmpty()) {
                    regularPortsWithoutEdgeUnassigned.add(port);
                }
                else {
                    numberOfEdges2RegularPort.get(port.getEdges().size()).add(port);
                }
            }
        }
        //add unpaired ports of device vertices
        for (Port port : unpairedPortsDeviceConnectors) {
            if (allDeviceVertices.contains(port.getVertex())) {
                if (port.getEdges().isEmpty()) {
                    regularPortsWithoutEdgeUnassigned.add(port);
                } else {
                    numberOfEdges2RegularPort.get(port.getEdges().size()).add(port);
                }
            }
        }
        //splice ports
        LinkedList<Port> splicePorts = new LinkedList<>();
        for (Vertex splice : splices) {
            splicePorts.add(splice.getPorts().iterator().next());
        }

        //first connect different connected components to reach the target value for connected components
        connectedComponents = JungUtils.getConnectedComponents(newPlan);
        while (connectedComponents.size() > targetValueConnectedComponents) {
            List<Set<JungUtils.PseudoVertex>> componentsToBeConnected =
                    selectRandomly(new ArrayList<>(connectedComponents), 2);
            //find ports of component
            List<List<Port>> portsComponents = new ArrayList<>(2);
            for (int i = 0; i < 2; i++) {
                List<Port> portsOfComponent = new LinkedList<>();
                for (Port port : regularPortsWithoutEdgeUnassigned) {
                    JungUtils.PseudoVertex pseudoVertexForRealVertex = new JungUtils.PseudoVertex(port.getVertex());
                    JungUtils.PseudoVertex pseudoVertexForRealVertexGroup = null;
                    if (port.getVertex().getVertexGroup() != null) {
                        pseudoVertexForRealVertexGroup = new JungUtils.PseudoVertex(port.getVertex().getVertexGroup());
                    }
                    if (componentsToBeConnected.get(i).contains(pseudoVertexForRealVertex)
                            || (pseudoVertexForRealVertexGroup != null
                                && componentsToBeConnected.get(i).contains(pseudoVertexForRealVertexGroup))) {
                        portsOfComponent.add(port);
                    }
                }
                //use splices only if no regular vertices available
                if (portsOfComponent.isEmpty()) {
                    for (Port splicePort : splicePorts) {
                        if (componentsToBeConnected.get(i).contains(
                                new JungUtils.PseudoVertex(splicePort.getVertex()))) {
                            portsOfComponent.add(splicePort);
                        }
                    }
                }
                portsComponents.add(portsOfComponent);
            }
            //select a port from both selected components
            Port port0 = portsComponents.get(0).isEmpty() ? getPortThatCanHaveEdges(componentsToBeConnected.get(0),
                    newPlan) : selectRandomly(portsComponents.get(0), 1).get(0);
            Port port1 = portsComponents.get(1).isEmpty() ? getPortThatCanHaveEdges(componentsToBeConnected.get(1),
                    newPlan) : selectRandomly(portsComponents.get(1), 1).get(0);
            addNewEdge(newPlan, Arrays.asList(port0, port1), edges, edgeLabelTextsUnused, edgeLabelTexts);
            --missingEdges;
            updatePortLists(port0, regularPortsWithoutEdgeUnassigned, numberOfEdges2RegularPort, newPlan);
            updatePortLists(port1, regularPortsWithoutEdgeUnassigned, numberOfEdges2RegularPort, newPlan);

            //unify components
            connectedComponents.remove(componentsToBeConnected.get(0));
            connectedComponents.remove(componentsToBeConnected.get(1));
            Set<JungUtils.PseudoVertex> unificationComponent = componentsToBeConnected.get(0);
            unificationComponent.addAll(componentsToBeConnected.get(1));
            connectedComponents.add(unificationComponent);
        }

        //determine for each such regular port without edges how many edges it should get in the end
        List<Port> portsToGetAnEdge = new ArrayList<>();
        List<Port> portsToGetMoreEdges = new ArrayList<>();
        List<Port> regularPortsWithoutEdge = selectRandomly(regularPortsWithoutEdgeUnassigned,
                targetValueIEdgesPerRegularPort[0]);
        regularPortsWithoutEdgeUnassigned.removeAll(regularPortsWithoutEdge);
        Map<Integer, List<Port>> currNumberOfEdges2ports = new LinkedHashMap<>();
        Map<Integer, List<Port>> addedNumberOfEdges2ports = new LinkedHashMap<>();
        currNumberOfEdges2ports.put(0, regularPortsWithoutEdge);
        for (int i = 1; i < targetValueIEdgesPerRegularPort.length; i++) {
            List<Port> currRegularPortsWithIEdges = numberOfEdges2RegularPort.get(i);
            currNumberOfEdges2ports.put(i, new ArrayList<>(currRegularPortsWithIEdges));
            addedNumberOfEdges2ports.put(i, new ArrayList<>());
            int missingPortsWithIEdges = targetValueIEdgesPerRegularPort[i] - currRegularPortsWithIEdges.size();
            if (missingPortsWithIEdges > 0) {
                List<Port> candidates = new ArrayList<>(regularPortsWithoutEdgeUnassigned);
                candidates.addAll(portsToGetMoreEdges);
                List<Port> portsToGetIEdges = selectRandomly(candidates, missingPortsWithIEdges);
                for (Port portToGetIEdges : portsToGetIEdges) {
                    //add the selected ports up to i times -> each occurrence will get an edge later
                    for (int j = portToGetIEdges.getEdges().size()
                            + countOccurrences(portsToGetAnEdge, portToGetIEdges); j < i; j++) {
                        portsToGetAnEdge.add(portToGetIEdges);
                    }
                    regularPortsWithoutEdgeUnassigned.remove(portToGetIEdges);
                    portsToGetMoreEdges.remove(portToGetIEdges);
                    --missingPortsWithIEdges;
                }
                addedNumberOfEdges2ports.get(i).addAll(portsToGetIEdges);
                if (missingPortsWithIEdges > 0) {
                    //there are no more ports available; maybe adapt target values instead (currently commented out)
//                    if (i < targetValueIEdgesPerRegularPort.length - 1) {
//                        targetValueIEdgesPerRegularPort[i] -= missingPortsWithIEdges;
//                        targetValueIEdgesPerRegularPort[i + 1] += missingPortsWithIEdges;
//                    }
                }
            }
            else if (missingPortsWithIEdges < 0) {
                portsToGetMoreEdges.addAll(selectRandomly(currRegularPortsWithIEdges, -missingPortsWithIEdges));
                for (Port port : portsToGetMoreEdges) {
                    currNumberOfEdges2ports.get(i).remove(port);
                    addedNumberOfEdges2ports.get(i).remove(port);
                }
            }
        }
        if (portsToGetMoreEdges.size() > 0) {
            //that case should ideally not or occcur. If it (rarely) does, maybe add a port--edge incidence (next line)
//            portsToGetAnEdge.addAll(portsToGetMoreEdges);
        }
        //and for all splices
        int currSpliceEdgeIncidences = countSpliceEdgeIncidences(newPlan);
        int addedSpliceEdgeIncidences = 0;
        while (currSpliceEdgeIncidences + addedSpliceEdgeIncidences < targetValueSpliceEdgeIncidences
                && !splicePorts.isEmpty()) {
            portsToGetAnEdge.add(selectRandomly(splicePorts, 1).get(0));
            ++addedSpliceEdgeIncidences;
        }

        ////////////////////
        //this block is just for checking correctness and is not used later in the code
        int availablePortIncidences = portsToGetAnEdge.size();
        int missingEdgeIncidences = 0;
        LinkedHashMap<Integer, Integer> existingEdgesOfDegI = new LinkedHashMap<>();
        LinkedHashMap<Integer, Integer> missingEdgesOfDegI = new LinkedHashMap<>();
        for (int i = 0; i < targetValueHyperedgesOfDegreeI.length; i++) {
            missingEdgeIncidences += i * Math.max(0,
                    targetValueHyperedgesOfDegreeI[i] - countHyperedgesOfDegreeI(newPlan, i));
            existingEdgesOfDegI.put(i, countHyperedgesOfDegreeI(newPlan, i));
            missingEdgesOfDegI.put(i, targetValueHyperedgesOfDegreeI[i] - countHyperedgesOfDegreeI(newPlan, i));
        }
        if (availablePortIncidences != missingEdgeIncidences) {
            //TODO: bad case, occurs sometimes (maybe 1 of 20 times a plan is generated)
        }
        ////////////////////


        //randomly connect the remaining ports of the list -- however if both endpoints are the same port choose a new
        // random port instead.
        //We add the remaining hyperedges
        //TODO: care about more the structural properties (currently only multi-edges) then also connectedness, ...
        //add hyperedges with deg >= 3
        for (int i = targetValueHyperedgesOfDegreeI.length - 1; i >= 3; i--) {
            int currEdgesOfDegI = countHyperedgesOfDegreeI(newPlan, i);
            while (targetValueHyperedgesOfDegreeI[i] > currEdgesOfDegI) {
                boolean success = findAndInsertNewEdge(newPlan, portsToGetAnEdge, i, edges,
                        targetValueMeanParallelEdges, targetValueSelfLoops, edgeLabelTextsUnused, edgeLabelTexts);
                if (!success) {
                    break;
                }
                --missingEdges;
                ++currEdgesOfDegI;
            }
        }
        //add edges of deg 2
        while (missingEdges > 0) {
            boolean success = findAndInsertNewEdge(newPlan, portsToGetAnEdge, 2, edges,
                    targetValueMeanParallelEdges, targetValueSelfLoops, edgeLabelTextsUnused, edgeLabelTexts);
            if (!success) {
                break;
            }
            --missingEdges;
        }

        // F. insert edge bundles

        //TODO

        ////////////
        // Save plan
        ////////////

        if (newPlan.getVertices().isEmpty()) {
            //no vertices -> empty graph -> do not save
            return false;
        }

        String fileId;
        String fileName;
        String filePath;
        do {
            fileId = getRandom16DigitsHexadecimalString();
            fileName = "praline-pseudo-plan-" + fileId + ".json";
            filePath = PATH_GENERATED_PLANS + File.separator + fileName;
        }
        while (new File(filePath).exists());
        IOUtils.saveGraphAsJson(newPlan, filePath);

        System.out.println("generated new plan " + (++generatedPlansCounter));

        Set<Set<JungUtils.PseudoVertex>> connectedComponentsFinally = JungUtils.getConnectedComponents(newPlan);
        if (connectedComponentsFinally.size() > 1) {
            System.out.println("Warning! " + filePath + " has more than 1 connected component.");
        }

        return true;
    }

    private static <E> int countOccurrences(List<E> list, E element) {
        int counter = 0;
        for (E e : list) {
            if (e.equals(element)) {
                ++counter;
            }
        }
        return counter;
    }

    private static void updatePortLists(Port port, List<Port> regularPortsWithoutEdgeUnassigned,
                                        Map<Integer, List<Port>> numberOfEdges2RegularPort, Graph graph) {
        if (ImplicitCharacteristics.isSplice(port.getVertex(), graph)) {
            return;
        }
        if (regularPortsWithoutEdgeUnassigned.contains(port)) {
            regularPortsWithoutEdgeUnassigned.remove(port);
            numberOfEdges2RegularPort.get(1).add(port);
        }
        else {
            if (port.getEdges().size() > 1) {
                numberOfEdges2RegularPort.get(port.getEdges().size() - 1).remove(port);
            }
            numberOfEdges2RegularPort.putIfAbsent(port.getEdges().size(), new ArrayList<>());
            numberOfEdges2RegularPort.get(port.getEdges().size()).add(port);
        }
    }

    private static Port getPortThatCanHaveEdges(Set<JungUtils.PseudoVertex> pseudoVertices, Graph graph) {
        for (JungUtils.PseudoVertex pseudoVertex : pseudoVertices) {
            Object vertexObject = pseudoVertex.getVertexObject();
            if (vertexObject instanceof Vertex) {
                Port somePortWithEdges = getPortThatCanHaveEdges((Vertex) vertexObject, graph);
                if (somePortWithEdges != null) {
                    return somePortWithEdges;
                }
            }
            else if (vertexObject instanceof VertexGroup) {
                for (Vertex containedVertex : ((VertexGroup) vertexObject).getContainedVertices()) {
                    Port somePortWithEdges = getPortThatCanHaveEdges(containedVertex, graph);
                    if (somePortWithEdges != null) {
                        return somePortWithEdges;
                    }
                }
            }
        }
        return null;
    }

    private static Port getPortThatCanHaveEdges(Vertex vertex, Graph graph) {
        if (ImplicitCharacteristics.isDeviceVertex(vertex, graph) || vertex.getPorts().isEmpty()) {
            return null;
        }
        return vertex.getPorts().iterator().next();
    }

    /**
     *
     * @param graph
     * @param portsToGetAnEdge
     * @param numberOfPorts
     * @param setOfAlreadyExistingEdges
     * @param targetValueMeanParallelEdges
     * @param edgeLabelTextsUnused
     * @param edgeLabelTexts
     * @return
     *      success
     */
    private static boolean findAndInsertNewEdge(Graph graph, List<Port> portsToGetAnEdge, int numberOfPorts,
                                                Collection<Edge> setOfAlreadyExistingEdges,
                                                double targetValueMeanParallelEdges, int targetValueSelfLoops,
                                                LinkedList<String> edgeLabelTextsUnused,
                                                Collection<String> edgeLabelTexts) {
        List<Collection<Port>> candidatesForNewEdge = generateCandidatesForNewEdge(portsToGetAnEdge, numberOfPorts);
        //if no edges available return fail
        if (candidatesForNewEdge == null) {
            return false;
        }
        //otherwise compute badness of candidates
        double bestBadness = Double.POSITIVE_INFINITY;
        Collection<Port> bestCandidate = null;
        for (Collection<Port> candidate : candidatesForNewEdge) {
            double badnessCandidate = evaluateInsertionOfEdge(graph, candidate, targetValueMeanParallelEdges,
                    targetValueSelfLoops, edgeLabelTextsUnused, edgeLabelTexts);
            if (badnessCandidate < bestBadness) {
                bestBadness = badnessCandidate;
                bestCandidate = candidate;
            }
        }

        addNewEdge(graph, bestCandidate, setOfAlreadyExistingEdges, edgeLabelTextsUnused, edgeLabelTexts);
        for (Port port : bestCandidate) {
            portsToGetAnEdge.remove(port);
        }
        return true;
    }

    /**
     *
     * @param graph
     * @param portsOfNewEdge
     * @param targetValueMeanParallelEdges
     * @param edgeLabelTextsUnused
     * @param edgeLabelTexts
     * @return
     *      badness of insertion (0 is best possible insertion)
     */
    private static double evaluateInsertionOfEdge(Graph graph, Collection<Port> portsOfNewEdge,
                                                  double targetValueMeanParallelEdges, int targetValueSelfLoops,
                                                  LinkedList<String> edgeLabelTextsUnused,
                                                  Collection<String> edgeLabelTexts) {
        //add edge just to compute the badness
        Edge newEdge = addNewEdge(graph, portsOfNewEdge, null, edgeLabelTextsUnused, edgeLabelTexts);
        //compute badness
        NumberDistributionProperty<Integer> parallelEdgesProperty =
                (NumberDistributionProperty<Integer>) PropertyManager.getProperty("parallelEdges");
        NumberDistribution<Integer> parallelEdgesDistribution =
                parallelEdgesProperty.getComputingFunctionProperty().apply(graph);
        double currentMean = parallelEdgesDistribution.get(StatisticParameter.MEAN);
//        double currentStandardDeviation = parallelEdgesDistribution.get(StatisticParameter.STANDARD_DEVIATION);
        NumericalProperty<Integer> selfLoopProperty =
                (NumericalProperty<Integer>) PropertyManager.getProperty("selfLoopEdges");
        int selfLoops = selfLoopProperty.getComputingFunctionProperty().apply(graph);
        int numberOfSurplusSelfLoops = Math.max(0, selfLoops - targetValueSelfLoops);
        double badness = Math.pow(targetValueMeanParallelEdges / currentMean - 1.0, 2.0) + numberOfSurplusSelfLoops;
        //remove the new edge after evaluation
        graph.removeEdge(newEdge);

        return badness;
    }

    /**
     *
     * @param portsToGetAnEdge
     * @param numberOfPorts
     * @return
     *      null if no candidate available
     */
    private static List<Collection<Port>> generateCandidatesForNewEdge(List<Port> portsToGetAnEdge, int numberOfPorts) {
        List<Collection<Port>> candidates = new ArrayList<>(NUMBER_OF_CANDIDATES_FOR_EDGE_INSERTION);
        for (int i = 0; i < NUMBER_OF_CANDIDATES_FOR_EDGE_INSERTION; i++) {
            Collection<Port> candidateI = generateOneCandidateForNewEdge(portsToGetAnEdge, numberOfPorts);
            if (candidateI == null) {
                return null;
            }
            candidates.add(candidateI);
        }
        return candidates;
    }

    /**
     *
     * @param portsToGetAnEdge
     * @param numberOfPorts
     * @return
     *      null if no candidate
     */
    private static Collection<Port> generateOneCandidateForNewEdge(List<Port> portsToGetAnEdge, int numberOfPorts) {

        List<Port> ports = selectRandomlyWithoutDoublets(portsToGetAnEdge, numberOfPorts);
        if (ports.size() < numberOfPorts) {
            return null;
        }
        return ports;
    }

    private static int countHyperedgesOfDegreeI(Graph graph, int i) {
        int count = 0;
        for (Edge edge : graph.getEdges()) {
            if (edge.getPorts().size() == i) {
                ++count;
            }
        }
        return count;
    }

    private static int countSpliceEdgeIncidences(Graph graph) {
        int sum = 0;
        for (Vertex vertex : graph.getVertices()) {
            if (ImplicitCharacteristics.isSplice(vertex, graph)) {
                for (Port port : vertex.getPorts()) {
                    sum += port.getEdges().size();
                }
            }
        }
        return sum;
    }

    private static Edge addNewEdge(Graph graph, Collection<Port> ports, Collection<Edge> setOfAlreadyExistingEdges,
                                   LinkedList<String> edgeLabelTextsUnused, Collection<String> edgeLabelTexts) {
        TextLabel edgeLabel = new TextLabel(getNextLabelText(edgeLabelTextsUnused, edgeLabelTexts));
        Edge newEdge = new Edge(ports, Collections.singleton(edgeLabel), null);
        if (setOfAlreadyExistingEdges != null) {
            setOfAlreadyExistingEdges.add(newEdge);
        }
        graph.addEdge(newEdge);
        return newEdge;
    }

    /**
     * With this method, I tried to come closer to the original variance/standard deviation of elements per object
     * by trying to initialize with a (already good) number of initial elements per objects.
     * However, that did not work out.
     * In any case, the variance was greater for the pseudo plans.
     * This gap could be shrunk best if we just initialize all new objects with just 1 one elment.
     * Hence, this method will always return 1.
     *
     * @param targetValueTotalSum
     * @param targetValueNumberOfObjects
     * @param currentTotalSum
     * @param currentNumberOfObjects
     * @param standardDeviationElementsPerObject
     * @return
     *          1
     */
    private static int determineNumberOfNewElements(int targetValueTotalSum, int targetValueNumberOfObjects,
                                                    int currentTotalSum, int currentNumberOfObjects,
                                                    double standardDeviationElementsPerObject) {
        return 1;

//        if (targetValueTotalSum <= 1) {
//            return 1;
//        }
//        //all later objects should get at least 1 element, so we have only limited amount of available objects
//        int objectsRemainingAfterThis = targetValueNumberOfObjects - currentNumberOfObjects - 1;
//        int availableObjects = targetValueTotalSum - currentTotalSum - objectsRemainingAfterThis;
//        //determine number of elements for this object by a normal distribution
//        double meanRatio = (double) targetValueTotalSum / (double) targetValueNumberOfObjects;
//        double targetRatio;
//        try {
//            targetRatio =
//                    (double) computeTargetValueByNormalDistribution(meanRatio, standardDeviationElementsPerObject);
//        } catch (org.apache.commons.math3.exception.NumberIsTooLargeException e) {
//            return 1;
//        }
//        double targetObjects = targetRatio * (double) targetValueNumberOfObjects;
//        //reduce by factor q to have this object similar to the other objects that were also reduced by factor q
//        targetObjects = targetObjects * (1.0 - q);
//        return Math.max(1, Math.min((int) Math.round(targetObjects), availableObjects));
    }

    private static void changeLabelsRandomly(Collection<? extends LabeledObject> toBeRenamed,
                                             Collection<? extends LabeledObject> toBeExcluded,
                                             LinkedList<String> labelTextsUnused,
                                             Collection<String> labelTexts) {
        if (toBeExcluded == null) {
            toBeExcluded = Collections.emptyList();
        }
        for (LabeledObject labeledObject : toBeRenamed) {
            if (!toBeExcluded.contains(labeledObject)) {
                ((TextLabel) labeledObject.getLabelManager().getMainLabel())
                        .setInputText(getNextLabelText(labelTextsUnused, labelTexts));
            }
        }
    }

    private static Port addNewPort(Vertex vertex, Collection<Port> newPortToBeAddedTo) {
        TextLabel portLabel = new TextLabel(""); //start with empty label -- port labels will be reset later
        Port newPort = new Port(Collections.emptyList(), Collections.singleton(portLabel));
        newPortToBeAddedTo.add(newPort);

        //if there is no port group yet create one
        if (vertex.getPortCompositions().isEmpty()) {
            vertex.addPortComposition(new PortGroup(null, false));
        }
        //select one of the port groups on the top level randomly
        PortGroup portGroup = (PortGroup) selectRandomly(vertex.getPortCompositions(), 1).get(0);
        portGroup.addPortComposition(newPort);

        return newPort;
    }

    private static PortPairing addNewPortPairing(VertexGroup vertexGroup, List<Vertex> twoVerticesToGetThePortPairing,
                                                 Collection<Port> newPortsToBeAddedTo, Graph graph) {
        ArrayList<Port> portsOfNewPortPairing = new ArrayList<>(2);
        for (Vertex vertex : twoVerticesToGetThePortPairing) {
            TextLabel portLabel = new TextLabel(""); //start with empty label -- port labels will be reset later
            Port port = new Port(Collections.emptyList(), Collections.singleton(portLabel));
            if (newPortsToBeAddedTo != null) {
                newPortsToBeAddedTo.add(port);
            }
            portsOfNewPortPairing.add(port);
            PortGroup portGroup = findOrCreateCorrectPortGroup(vertex, vertexGroup, graph);
            portGroup.addPortComposition(port);
        }
        PortPairing newPortPairing = new PortPairing(portsOfNewPortPairing.get(0), portsOfNewPortPairing.get(1));
        vertexGroup.addPortPairing(newPortPairing);

        return newPortPairing;
    }

    private static PortGroup findOrCreateCorrectPortGroup(Vertex vertex, VertexGroup vertexGroup, Graph graph) {
        if (ImplicitCharacteristics.isOfType(VertexType.DEVICE_VERTEX, vertex, graph)) {
            ArrayList<PortGroup> allPortGroups = new ArrayList<>();
            for (PortComposition portGroup : vertex.getPortCompositions()) {
                allPortGroups.add((PortGroup) portGroup);
            }
            //go through all port groups and if there is a port pairing in this port group connecting to the other
            // vertex then use this port group
            for (PortGroup portGroup : allPortGroups) {
                for (PortPairing portPairing : vertexGroup.getPortPairings()) {
                    if (portGroup.getPortCompositions().contains(portPairing.getPort0())
                            || portGroup.getPortCompositions().contains(portPairing.getPort1())) {
                        return portGroup;
                    }
                }
            }
            //if we did not find such a port group then we create a new one
            PortGroup newPortGroup = new PortGroup(null, false);
            vertex.addPortComposition(newPortGroup);
            return newPortGroup;
        }
        if (vertex.getPortCompositions().isEmpty()) {
            vertex.addPortComposition(new PortGroup(null, false));
        }
        return (PortGroup) vertex.getPortCompositions().get(0);
    }

    /**
     * removes also incident edges
     */
    private static void removePortCleanly(Graph graph, Port port) {
        for (Edge edge : new ArrayList<>(port.getEdges())) {
            graph.removeEdge(edge);
        }
        port.getVertex().removePortComposition(port);
    }

    /**
     * removes also incident edges
     */
    private static void removeVertexCleanly(Graph graph, Vertex vertex) {
        for (Port port : vertex.getPorts()) {
            for (Edge edge : new ArrayList<>(port.getEdges())) {
                graph.removeEdge(edge);
            }
        }
        graph.removeVertex(vertex);
    }

    private static PortPairing getPortPairing(Port port, VertexGroup vertexGroup) {
        for (PortPairing portPairing : vertexGroup.getPortPairings()) {
            if (portPairing.getPorts().contains(port)) {
                return portPairing;
            }
        }
        return null;
    }

    /**
     * sets new labels to the ports of this vertex. For the one vertex, the labels will be integers starting with A.1,
     * A.2, ... in random order, for the next vertex B.1, B.2, ...
     * For one vertex the assignment is random and for the others it is such that the paired ports get the same
     * number. You may skip numbers with some probability.
     *
     * @param vertexGroup
     * @param skipNumberProbability
     *      value in range [0,1) -- set this value to 0 to skip nothing
     */
    private static void resetPortLabelText(VertexGroup vertexGroup, double skipNumberProbability,
                                           Collection<Port> unpairedPorts) {
        List<Vertex> vertices = vertexGroup.getContainedVertices();
        //first find vertex with the most ports
        int maxPorts = Integer.MIN_VALUE;
        Vertex vertexWithMostPorts = null;
        for (Vertex vertex : vertices) {
            if (vertex.getPorts().size() > maxPorts) {
                maxPorts = vertex.getPorts().size();
                vertexWithMostPorts = vertex;
            }
        }

        int portNumber = resetPortLabelText(vertexWithMostPorts, skipNumberProbability, ""); //"A.");

        //name the ports of the other vertices
        //1. determine prefix
        //TODO: for now: no use of prefix any more
//        Map<Vertex, String> vertex2prefix = new LinkedHashMap<>();
//        vertex2prefix.put(vertexWithMostPorts, "A.");
//        Iterator<String> alphabet =
//                Arrays.asList("B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
//                        "T", "U", "V", "W", "X", "Y", "Z", "Aa", "Ab", "Ac", "Ad", "Ae", "Af", "Ag", "Ah").iterator();
//        for (Vertex vertex : vertices) {
//            if (!vertex.equals(vertexWithMostPorts)) {
//                vertex2prefix.put(vertex, alphabet.next() + ".");
//            }
//        }
        //2. find pairings and name them accordingly
        for (PortPairing portPairing : vertexGroup.getPortPairings()) {
            Port port0 = portPairing.getPort0();
            Port port1 = portPairing.getPort1();
            Port basePort = port0.getVertex().equals(vertexWithMostPorts) ? port0 : port1;
            Port otherPort = port0.getVertex().equals(vertexWithMostPorts) ? port1 : port0;

            //if the base vertex was not involved, we have to give a name first
            if (!basePort.getVertex().equals(vertexWithMostPorts)) {
                if (skipNumberProbability > 0) {
                    while (RANDOM.nextDouble() < skipNumberProbability) {
                        ++portNumber;
                    }
                }
                ((TextLabel) basePort.getLabelManager().getMainLabel()).setInputText(
//                        vertex2prefix.get(basePort.getVertex()) +
                        (portNumber++) + "");
            }
            //name the other by the base port
            ((TextLabel) otherPort.getLabelManager().getMainLabel()).setInputText(
//                    vertex2prefix.get(otherPort.getVertex()) +
                    ((TextLabel) basePort.getLabelManager().getMainLabel()).getInputText()); //.substring(2));
        }
        //3. find and name unpaired ports
        for (Vertex vertex : vertices) {
            for (Port port : vertex.getPorts()) {
                if (unpairedPorts.contains(port)) {
                    if (skipNumberProbability > 0) {
                        while (RANDOM.nextDouble() < skipNumberProbability) {
                            ++portNumber;
                        }
                    }
                    ((TextLabel) port.getLabelManager().getMainLabel()).setInputText(
//                            vertex2prefix.get(port.getVertex()) +
                            (portNumber++) + "");
                }
            }
        }
    }

    /**
     * sets new labels to the ports of this vertex. The labels will be integers starting with 1 and the increasing
     * numbers in random order. You may skip numbers with some probability.
     *
     * @param vertex
     * @param skipNumberProbability
     *      value in range [0,1) -- set this value to 0 to skip nothing
     * @return
     *      greatest port number + 1 that was used
     */
    private static int resetPortLabelText(Vertex vertex, double skipNumberProbability) {
        return resetPortLabelText(vertex, skipNumberProbability, "");
    }

    private static int resetPortLabelText(Vertex vertex, double skipNumberProbability, String portNamePrefix) {
        if (portNamePrefix == null) {
            portNamePrefix = "";
        }

        LinkedList<Port> ports = new LinkedList<>(vertex.getPorts());
        Collections.shuffle(ports, RANDOM);

        int portNumber = 1;
        for (Port port : ports) {
            if (skipNumberProbability > 0) {
                while (RANDOM.nextDouble() < skipNumberProbability) {
                    ++portNumber;
                }
            }
            ((TextLabel) port.getLabelManager().getMainLabel()).setInputText(portNamePrefix + (portNumber++));
        }
        return portNumber;
    }

    private static Map<VertexType, List<Vertex>> separateVertices(Graph plan) {
        Map<VertexType, List<Vertex>> vertexTypeMap = new LinkedHashMap<>(VertexType.values().length);

        for (VertexType vertexType : VertexType.values()) {
            vertexTypeMap.put(vertexType, new ArrayList<>());
        }
        for (Vertex vertex : plan.getVertices()) {
            vertexTypeMap.get(ImplicitCharacteristics.getVertexType(vertex, plan)).add(vertex);
        }
        return vertexTypeMap;
    }

    private static Map<VertexGroupType, List<VertexGroup>> separateVertexGroups(Graph plan) {
        Map<VertexGroupType, List<VertexGroup>> vertexGroupTypeMap = new LinkedHashMap<>(VertexGroupType.values().length);

        for (VertexGroupType vertexGroupType : VertexGroupType.values()) {
            vertexGroupTypeMap.put(vertexGroupType, new ArrayList<>());
        }
        for (VertexGroup vertexGroup : plan.getVertexGroups()) {
            vertexGroupTypeMap.get(ImplicitCharacteristics.getVertexGroupType(vertexGroup, plan)).add(vertexGroup);
        }
        return vertexGroupTypeMap;
    }

    private static String getNextLabelText(LinkedList<String> labelTextsUnused,
                                           Collection<String> labelTexts) {
        if (!labelTextsUnused.isEmpty()) {
            return labelTextsUnused.poll();
        }

        //we have used all available label texts once -> re-start
        labelTextsUnused.addAll(labelTexts);
        Collections.shuffle(labelTextsUnused, RANDOM);
        return getNextLabelText(labelTextsUnused, labelTexts);
    }

    private static String getRandom16DigitsHexadecimalString() {
        String hexString = "";
        for (int i = 0; i < 16; i++) {
            hexString += Integer.toHexString(RANDOM.nextInt(16));
        }
        return hexString;
    }

    private static Pair<Map<NumericalProperty,Number>, Map<NumberDistributionProperty,Map<StatisticParameter,Number>>>
    determineTargetValues(PropertySheet originalPlanSheet, DataSetProperties originalPlansProperties) {

        Map<NumericalProperty,Number> numericalPropertyMap = new LinkedHashMap<>();
        Map<NumberDistributionProperty,Map<StatisticParameter,Number>> numberDistributionPropertyMap = new LinkedHashMap<>();

        for (PropertyValue propertyValue : originalPlanSheet.getAllValues()) {
            Property property = propertyValue.getProperty();
            if (property instanceof NumericalProperty) {
                Number n = (Number) propertyValue.getValue();
                double standardDeviation = findStandardDeviation((NumericalProperty) property, originalPlansProperties);
                Number correctTypeNewValue = computeTargetValueByNormalDistribution(n, standardDeviation);
                numericalPropertyMap.put((NumericalProperty) property, correctTypeNewValue);
            }
            else if (property instanceof NumberDistributionProperty) {
                NumberDistribution<?> numberDistribution = (NumberDistribution<?>) propertyValue.getValue();
                numberDistributionPropertyMap.put((NumberDistributionProperty) property, new LinkedHashMap<>());
                for (StatisticParameter statisticParameter : StatisticParameter.values()) {
                    Number n = numberDistribution.get(statisticParameter);
                    double standardDeviation = findStandardDeviation((NumberDistributionProperty) property,
                            statisticParameter, originalPlansProperties);
                    Number correctTypeNewValue = computeTargetValueByNormalDistribution(n, standardDeviation);
                    numberDistributionPropertyMap.get(property).put(statisticParameter, correctTypeNewValue);
                }
            }
        }

        return new Pair<>(numericalPropertyMap, numberDistributionPropertyMap);
    }

    private static Number computeTargetValueByNormalDistribution(Number n, double standardDeviation) {
        //catch special cases
        if (standardDeviation <= 0 || Double.isNaN(standardDeviation)) {
            return n;
        }
        if (Double.isNaN(n.doubleValue())) {
            return n;
        }

        NormalDistribution normalDistribution = new NormalDistribution(n.doubleValue(), standardDeviation);
        double newValue = normalDistribution.inverseCumulativeProbability(RANDOM.nextDouble());
        Number correctTypeNewValue = null;
        if (n instanceof Double) {
            correctTypeNewValue = newValue;
            if ((double) correctTypeNewValue < 0) {
                correctTypeNewValue = 0.0;
            }
        }
        else if (n instanceof Integer) {
            correctTypeNewValue = (int) Math.round(newValue);
            if ((int) correctTypeNewValue < 0) {
                correctTypeNewValue = 0;
            }
        }
        return correctTypeNewValue;
    }

    private static double findStandardDeviation(NumericalProperty property, DataSetProperties originalPlansProperties) {
        return originalPlansProperties.get(property, StatisticParameter.STANDARD_DEVIATION)
                / originalPlansProperties.get(property, StatisticParameter.COUNT) * 40.0; //100.0;
    }

    private static double findStandardDeviation(NumberDistributionProperty property,
                                                StatisticParameter statisticParameter,
                                                DataSetProperties originalPlansProperties) {
        return originalPlansProperties.get(property, statisticParameter, StatisticParameter.STANDARD_DEVIATION)
                / originalPlansProperties.get(property, statisticParameter, StatisticParameter.COUNT); // * 2.0;
    }

    private static <E> List<E> selectRandomly(List<E> baseList, int elementsToBeSelected) {
        return selectRandomly(baseList, elementsToBeSelected, true);
    }

    private static <E> List<E> selectRandomlyWithoutDoublets(List<E> baseList, int elementsToBeSelected) {
        return selectRandomly(baseList, elementsToBeSelected, false);
    }

    private static <E> List<E> selectRandomly(List<E> baseList, int elementsToBeSelected, boolean allowDoublets) {
        LinkedList<E> remainingElements = new LinkedList<>(baseList);
        ArrayList<E> selectedElements = new ArrayList<>(elementsToBeSelected);

        while (selectedElements.size() < elementsToBeSelected && !remainingElements.isEmpty()) {
            int selectionIndex = RANDOM.nextInt(remainingElements.size());
            E selectedElement = remainingElements.remove(selectionIndex);
            if (allowDoublets || !selectedElements.contains(selectedElement)) {
                selectedElements.add(selectedElement);
            }
        }
        return selectedElements;
    }
}
