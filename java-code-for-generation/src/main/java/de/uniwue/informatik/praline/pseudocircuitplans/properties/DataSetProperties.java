package de.uniwue.informatik.praline.pseudocircuitplans.properties;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;

import java.util.ArrayList;

public class DataSetProperties extends ArrayList<PropertySheet> {

    public DataSetProperties() {
        super();
    }

    public DataSetProperties(int initialCapacity) {
        super(initialCapacity);
    }

    public <N extends Number & Comparable> double get(NumericalProperty<N> property,
                                                      StatisticParameter statisticParameter) {
        return this.getDistribution(property).get(statisticParameter);
    }

    public PropertySheet get(Graph plan) {
        for (PropertySheet propertySheet : this) {
            if (propertySheet.getGraph().equals(plan)) {
                return propertySheet;
            }
        }
        return null;
    }

    public double get(NumberDistributionProperty<?> property,
                      StatisticParameter useThisParameterForInternalDataOfEachGraph,
                      StatisticParameter statisticParameter) {
        NumberDistribution<Double> allPropertyValues = new NumberDistribution<>(this.size());
        for (PropertySheet propertySheet : this) {
            allPropertyValues.add(propertySheet.getPropertyValue(property)
                    .get(useThisParameterForInternalDataOfEachGraph));
        }
        return allPropertyValues.get(statisticParameter);
    }

    public <N extends Number & Comparable> NumberDistribution<N> getDistribution(NumericalProperty<N> property) {
        NumberDistribution<N> allPropertyValues = new NumberDistribution<>(this.size());
        for (PropertySheet propertySheet : this) {
            allPropertyValues.add(propertySheet.getPropertyValue(property));
        }
        return allPropertyValues;
    }

    public NumberDistribution<Double> getDistribution(NumberDistributionProperty<?> property,
                                                      StatisticParameter statisticParameter) {
        NumberDistribution<Double> allPropertyValues = new NumberDistribution<>(this.size());
        for (PropertySheet propertySheet : this) {
            allPropertyValues.add(propertySheet.getPropertyValue(property).get(statisticParameter));
        }
        return allPropertyValues;
    }
}
