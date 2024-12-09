package de.uniwue.informatik.praline.pseudocircuitplans.properties;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;

import java.util.function.Function;

public class NumericalProperty<N extends Number & Comparable> extends Property<N> {


    private NumericalProperty(String propertyName, Function<Graph, N> computingFunctionProperty) {
        super(propertyName, computingFunctionProperty);
    }

    /**
     * Find existing properties via {@link PropertyManager#getProperty(String)}!
     *
     * @param propertyName
     * @param computingFunctionProperty
     * @param <E>
     * @return
     */
    public static <N extends Number & Comparable> NumericalProperty<N> createNewProperty(String propertyName,
                                                                                         Function<Graph, N>
                                                                                         computingFunctionProperty) {
        if (PropertyManager.getProperty(propertyName) != null) {
            return null;
        }
        NumericalProperty<N> property = new NumericalProperty<>(propertyName, computingFunctionProperty);
        PropertyManager.addProperty(property);
        return property;
    }
}
