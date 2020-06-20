package de.uniwue.informatik.praline.pseudocircuitplans.properties;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;

import java.util.function.Function;

/**
 * Every property can exist only once (unique by name).
 * Instead of calling the constructor, you should call {@link NumericalProperty#createNewProperty(String, Function)}
 * or {@link NumberDistributionProperty#createNewProperty(String, Function)}.
 * {@link NumericalProperty} and {@link NumberDistributionProperty} are the subclasses of {@link Property}.
 * Access all created properties by {@link PropertyManager#getProperty(String)}!
 * <br/>
 * <br/>
 * Property of a {@link Graph} and how it is computed.
 * For example, this may be number of vertices in the graph, so
 * {@link Property#getPropertyName()} = "vertexCount",
 * generic parameter E is {@link Integer}
 * and
 * {@link Property#getComputingFunctionProperty()} provides a function
 * that counts the number of vertices in a graph and returns that number.
 * The same {@link Property} may be used for multiple {@link Graph}s
 * to compute a {@link PropertyValue} for each {@link Graph}.
 *
 * @param <E>
 */
public abstract class Property<E> {

    private String propertyName;

    private Function<Graph, E> computingFunctionProperty;

    protected Property(String propertyName, Function<Graph, E> computingFunctionProperty) {
        this.propertyName = propertyName;
        this.computingFunctionProperty = computingFunctionProperty;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Function<Graph, E> getComputingFunctionProperty() {
        return computingFunctionProperty;
    }
}
