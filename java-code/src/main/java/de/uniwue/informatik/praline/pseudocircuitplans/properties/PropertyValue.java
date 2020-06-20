package de.uniwue.informatik.praline.pseudocircuitplans.properties;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;

/**
 * Stores one concrete value of one {@link Property} for one {@link Graph}.
 * All {@link PropertyValue}s of a {@link Graph} may be stored in a {@link PropertySheet}.
 *
 * @param <E>
 */
public class PropertyValue<E> {

    private Property<E> property;

    private E value;

    public PropertyValue(Property<E> property, Graph graph) {
        this.property = property;
        this.value = property.getComputingFunctionProperty().apply(graph);
    }

    public Property<E> getProperty() {
        return property;
    }

    public E getValue() {
        return value;
    }
}
