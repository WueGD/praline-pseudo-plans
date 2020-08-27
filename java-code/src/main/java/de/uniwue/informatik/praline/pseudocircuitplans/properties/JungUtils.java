package de.uniwue.informatik.praline.pseudocircuitplans.properties;


import com.google.common.base.Function;
import de.uniwue.informatik.praline.datastructure.graphs.*;
import edu.uci.ics.jung.algorithms.shortestpath.DistanceStatistics;
import edu.uci.ics.jung.graph.UndirectedGraph;

import java.util.*;

public class JungUtils {

    public static class PseudoVertex {
        private Vertex vertex;
        private VertexGroup vertexGroup;

        public PseudoVertex(Vertex vertex) {
            this.vertex = vertex;
        }

        public PseudoVertex(VertexGroup vertexGroup) {
            this.vertexGroup = vertexGroup;
        }

        public Object getVertexObject() {
            if (getVertex() != null) {
                return getVertex();
            }
            return getVertexGroup();
        }
        public Vertex getVertex() {
            return vertex;
        }
        public VertexGroup getVertexGroup() {
            return vertexGroup;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PseudoVertex that = (PseudoVertex) o;
            return Objects.equals(vertex, that.vertex) && Objects.equals(vertexGroup, that.vertexGroup);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vertex, vertexGroup);
        }
    }

    public static UndirectedGraph<PseudoVertex, Integer> transformToJungGraph(Graph pralineGraph) {
        UndirectedSparseGraph<PseudoVertex, Integer> jungGraph = new UndirectedSparseGraph<>();

        //add vertices
        LinkedHashMap<Vertex, PseudoVertex> vertex2pseudoVertex = new LinkedHashMap<>();
        for (VertexGroup vertexGroup : pralineGraph.getVertexGroups()) {
            if (!ImplicitCharacteristics.isOfType(VertexGroupType.UNDEFINED, vertexGroup, pralineGraph)) {
                PseudoVertex pseudoVertex = new PseudoVertex(vertexGroup);
                jungGraph.addVertex(pseudoVertex);
                for (Vertex vertex : vertexGroup.getAllRecursivelyContainedVertices()) {
                    vertex2pseudoVertex.put(vertex, pseudoVertex);
                }
            }
        }
        for (Vertex vertex : pralineGraph.getVertices()) {
            if (!vertex2pseudoVertex.containsKey(vertex)) {
                PseudoVertex pseudoVertex = new PseudoVertex(vertex);
                jungGraph.addVertex(pseudoVertex);
                vertex2pseudoVertex.put(vertex, pseudoVertex);
            }
        }

        //add edges
        int edgeCounter = 0;
        for (Edge edge : pralineGraph.getEdges()) {
            //decompose hyperedges to many deg-2-edges
            List<Port> ports = edge.getPorts();
            for (int i = 0; i < ports.size() - 1; i++) {
                PseudoVertex vi = vertex2pseudoVertex.get(ports.get(i).getVertex());
                for (int j = i + 1; j < ports.size(); j++) {
                    PseudoVertex vj = vertex2pseudoVertex.get(ports.get(j).getVertex());
                    if (!jungGraph.isNeighbor(vi, vj)) {
                        jungGraph.addEdge(edgeCounter++, vi, vj);
                    }
                }
            }
        }

        return jungGraph;
    }

    public static double getDiameterOfLargestComponent(Graph pralineGraph) {
        UndirectedGraph<PseudoVertex, Integer> jungGraph = transformToJungGraph(pralineGraph);
        removeEverythingButLargestComponent(jungGraph);

        return DistanceStatistics.diameter(jungGraph);
    }

    public static <V> Function<V, Double> getAverageDistances(UndirectedGraph<V, ?> jungGraph) {
        return DistanceStatistics.averageDistances(jungGraph);
    }

    public static <V> Set<V> getLargestConnectedComponent(Set<Set<V>> connectedComponents) {
        int maxSize = 0;
        Set<V> largestComponent = null;
        for (Set<V> connectedComponent : connectedComponents) {
            if (connectedComponent.size() > maxSize) {
                maxSize = connectedComponent.size();
                largestComponent = connectedComponent;
            }
        }
        return largestComponent;
    }
    public static <V> void removeEverythingButLargestComponent(UndirectedGraph<V, Integer> jungGraph) {
        Set<V> largestConnectedComponent = getLargestConnectedComponent(getConnectedComponents(jungGraph));

        //remove all vertices not in the largest component
        for (V vertex : new ArrayList<>(jungGraph.getVertices())) {
            if (!largestConnectedComponent.contains(vertex)) {
                jungGraph.removeVertex(vertex);
            }
        }
    }

    public static Set<Set<PseudoVertex>> getConnectedComponents(Graph pralineGraph) {
        return getConnectedComponents(transformToJungGraph(pralineGraph));
    }

    public static <V> Set<Set<V>> getConnectedComponents(UndirectedGraph<V, Integer> jungGraph) {
        WeakComponentClusterer<V, Integer> weakComponentClusterer = new WeakComponentClusterer<>();
        Set<Set<V>> components = weakComponentClusterer.apply(jungGraph);

        return components;
    }
}
