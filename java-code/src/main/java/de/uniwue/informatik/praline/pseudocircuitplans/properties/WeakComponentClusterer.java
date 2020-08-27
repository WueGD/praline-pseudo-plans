package de.uniwue.informatik.praline.pseudocircuitplans.properties;

/**
 * class copied from Jung, but replaced {@link java.util.HashSet} by {@link java.util.LinkedHashSet}
 */
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//
import com.google.common.base.Function;
import edu.uci.ics.jung.graph.Graph;

import java.util.*;

public class WeakComponentClusterer<V, E> implements Function<Graph<V, E>, Set<Set<V>>> {
    public WeakComponentClusterer() {
    }

    public Set<Set<V>> apply(Graph<V, E> graph) {
        Set<Set<V>> clusterSet = new LinkedHashSet<>();
        LinkedHashSet<V> unvisitedVertices = new LinkedHashSet<>(graph.getVertices());

        while(!unvisitedVertices.isEmpty()) {
            Set<V> cluster = new LinkedHashSet<>();
            V root = unvisitedVertices.iterator().next();
            unvisitedVertices.remove(root);
            cluster.add(root);
            Queue<V> queue = new LinkedList<>();
            queue.add(root);

            while(!queue.isEmpty()) {
                V currentVertex = queue.remove();
                Collection<V> neighbors = graph.getNeighbors(currentVertex);
                Iterator<V> var9 = neighbors.iterator();

                while(var9.hasNext()) {
                    V neighbor = var9.next();
                    if (unvisitedVertices.contains(neighbor)) {
                        queue.add(neighbor);
                        unvisitedVertices.remove(neighbor);
                        cluster.add(neighbor);
                    }
                }
            }

            clusterSet.add(cluster);
        }

        return clusterSet;
    }
}
