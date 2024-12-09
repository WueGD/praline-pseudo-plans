package de.uniwue.informatik.praline.pseudocircuitplans.properties;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;

import java.util.function.Function;

public class NumberDistributionProperty<N extends Number & Comparable> extends Property<NumberDistribution<N>> {


    private NumberDistributionProperty(String propertyName,
                                       Function<Graph, NumberDistribution<N>> computingFunctionProperty) {
        super(propertyName, computingFunctionProperty);
    }

    /**
     * Find existing properties via {@link PropertyManager#getProperty(String)}!
     *
     * @param propertyName
     * @param computingFunctionProperty
     * @param <N>
     * @return
     */
    public static <N extends Number & Comparable> NumberDistributionProperty<N> createNewProperty(String propertyName
            , Function<Graph, NumberDistribution<N>> computingFunctionProperty) {
        if (PropertyManager.getProperty(propertyName) != null) {
            return null;
        }
        NumberDistributionProperty<N> property = new NumberDistributionProperty<N>(propertyName,
                computingFunctionProperty);
        PropertyManager.addProperty(property);
        return property;
    }
}
