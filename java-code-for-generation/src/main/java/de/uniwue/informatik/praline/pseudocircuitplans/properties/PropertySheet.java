package de.uniwue.informatik.praline.pseudocircuitplans.properties;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * Object for gathering {@link PropertyValue}s for one graph
 */
public class PropertySheet {

    private Graph graph;

    private LinkedHashMap<String, PropertyValue> allValues = new LinkedHashMap<>();

    public PropertySheet(Graph graph) {
        this.graph = graph;
    }

    /**
     *
     * @param propertyValue
     * @param <E>
     * @return
     *      false if the value was not added because there is already a value of this property (remove it first from
     *      this sheet!)
     *      or true if the value was added
     */
    public <E> boolean addValue(PropertyValue<E> propertyValue) {
        String propertyName = propertyValue.getProperty().getPropertyName();
        if (allValues.containsKey(propertyName)) {
            return false;
        }
        allValues.put(propertyName, propertyValue);
        return true;
    }

    public <E> PropertyValue<E> removeValue(PropertyValue<E> propertyValue) {
        String propertyName = propertyValue.getProperty().getPropertyName();
        if (!allValues.containsKey(propertyName)) {
            return null;
        }
        return allValues.remove(propertyName);
    }

    public Collection<PropertyValue> getAllValues() {
        return new ArrayList<>(allValues.values());
    }

    public void removeAllValues() {
        allValues.clear();
    }

    public <E> E getPropertyValue(String propertyName) {
        return (E) allValues.get(propertyName).getValue();
    }

    public <E> E getPropertyValue(Property<E> property) {
        return (E) allValues.get(property.getPropertyName()).getValue();
    }

    public <N extends Number & Comparable> NumberDistribution<N> getPropertyValue(NumberDistributionProperty<N>
                                                                                          property) {
        return (NumberDistribution<N>) allValues.get(property.getPropertyName()).getValue();
    }

    public Graph getGraph() {
        return graph;
    }
}
