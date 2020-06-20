package de.uniwue.informatik.praline.pseudocircuitplans.generatingnewplans;

import de.uniwue.informatik.praline.datastructure.graphs.*;
import de.uniwue.informatik.praline.datastructure.utils.Serialization;
import de.uniwue.informatik.praline.pseudocircuitplans.properties.JungUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

public class MainExtractLargestComponent {

    private final static String SOURCE_PATH = "data" + File.separator + "praline-package-2020-05-18";

    private final static String TARGET_PATH = "data" + File.separator + "largest-comp-praline-package-2020-05-18";

    private final static String FILE_PREFIX = "lc-";

    public static void main(String[] args) throws IOException {
        File sourceDir = new File(SOURCE_PATH);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new IOException(SOURCE_PATH + " is no directory or does not exist. Abort.");
        }
        File targetDir = new File(TARGET_PATH);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        //go through all files and if it is a json, it should be a circuit plan and we read it
        int counter = 0;
        File[] files = sourceDir.listFiles();
        for (File file : files) {
            System.out.println("Read file " + file.getName() + " (" + ++counter + "/" + files.length + ")");
            if (file.isFile() && file.getName().endsWith(".json")) {
                extractAndSaveGraphOfLargestComponent(file);
            }
        }
    }

    private static void extractAndSaveGraphOfLargestComponent(File jsonFile) throws IOException {
        Graph graph = Serialization.read(jsonFile, Graph.class);

        Set<Set<JungUtils.PseudoVertex>> connectedComponents = JungUtils.getConnectedComponents(graph);
        //remove largest component from our collection. We will keep it.
        connectedComponents.remove(JungUtils.getLargestConnectedComponent(connectedComponents));
        //remove all elements of the other components
        for (Set<JungUtils.PseudoVertex> connectedComponent : connectedComponents) {
            for (JungUtils.PseudoVertex pseudoVertex : connectedComponent) {
                if (pseudoVertex.getVertexObject() instanceof Vertex) {
                    removeVertexCleanly(graph, (Vertex) pseudoVertex.getVertexObject());
                }
                else if (pseudoVertex.getVertexObject() instanceof VertexGroup) {
                    VertexGroup vertexGroup = (VertexGroup) pseudoVertex.getVertexObject();
                    for (Vertex containedVertex :
                            new ArrayList<>(vertexGroup.getAllRecursivelyContainedVertices())) {
                        removeVertexCleanly(graph, containedVertex);
                    }
                    graph.removeVertexGroup(vertexGroup);
                }
            }
        }

        String targetFilePath = TARGET_PATH + File.separator + FILE_PREFIX + jsonFile.getName();
        IOUtils.saveGraphAsJson(graph, targetFilePath);
        System.out.println("Saved " + targetFilePath);
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
}
