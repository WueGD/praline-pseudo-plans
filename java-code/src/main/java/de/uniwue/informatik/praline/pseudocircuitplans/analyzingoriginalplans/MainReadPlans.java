package de.uniwue.informatik.praline.pseudocircuitplans.analyzingoriginalplans;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.utils.Serialization;
import de.uniwue.informatik.praline.pseudocircuitplans.properties.*;

import java.io.File;
import java.io.IOException;

public class MainReadPlans {

    private final static String SOURCE_PATH = "data" + File.separator + "largest-comp-praline-package-2020-05-18";
//    "../drawings-original-generated/very-small";

    public static void main(String[] args) throws IOException {
        DataSetProperties originalPlansProperties = getDataSetProperties(SOURCE_PATH, true);

        //text output for the complete statistics
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("=======================================================================");
        System.out.println();
        System.out.println("Evaluated " + originalPlansProperties.size() + " original plans. Statistics:");
        System.out.println();
        textOutputStatistics(originalPlansProperties, null);
    }

    /**
     *
     * @param props0
     * @param props1
     *      might be null
     */
    public static void textOutputStatistics(DataSetProperties props0, DataSetProperties props1) {
        for (Property property : PropertyManager.getAllProperties()) {
            if (property instanceof NumericalProperty) {
                for (StatisticParameter statisticParameter : StatisticParameter.values()) {
                    System.out.print(statisticParameter.name() + " of " + property.getPropertyName() + ": " +
                            props0.get((NumericalProperty<?>) property, statisticParameter));
                    if (props1 != null) {
                        System.out.print(" | " + props1.get((NumericalProperty<?>) property, statisticParameter));
                    }
                    System.out.println();
                }
                System.out.println();
            }
            else if (property instanceof NumberDistributionProperty) {
                for (StatisticParameter useThisParameterForInternalDataOfEachGraph : StatisticParameter.values()) {
                    for (StatisticParameter statisticParameter : StatisticParameter.values()) {
                        System.out.print(statisticParameter.name() + " of " + property.getPropertyName() + " (" +
                                useThisParameterForInternalDataOfEachGraph.name() + "): " +
                                props0.get((NumberDistributionProperty<?>) property,
                                        useThisParameterForInternalDataOfEachGraph, statisticParameter));
                        if (props1 != null) {
                            System.out.print(" | " + props1.get((NumberDistributionProperty<?>) property,
                                    useThisParameterForInternalDataOfEachGraph, statisticParameter));
                        }
                        System.out.println();
                    }
                    System.out.println();
                }
            }
            System.out.println("---");
            System.out.println();
        }
    }

    public static DataSetProperties getDataSetProperties(String path, boolean textOutput) throws IOException {
        File sourceDir = new File(path);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new IOException(path + " is no directory or does not exist. Abort.");
        }
        //go through all files and if it is a json, it should be a circuit plan and we read it
        DataSetProperties plansProperties = new DataSetProperties();
        for (File file : sourceDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                plansProperties.add(analyzePlan(file, textOutput));
            }
        }
        return plansProperties;
    }

    private static PropertySheet analyzePlan(File jsonFile, boolean textOutput) throws IOException {
        Graph plan = Serialization.read(jsonFile, Graph.class);

        if (textOutput) {
            System.out.println("read plan " + jsonFile.getName());
        }

        PropertySheet propertySheet = new PropertySheet(plan);
        for (Property property : PropertyManager.getAllProperties()) {
            propertySheet.addValue(new PropertyValue<>(property, plan));
            if (textOutput) {
                System.out.println(property.getPropertyName() + ": " + propertySheet.getPropertyValue(property));
            }
        }
        if (textOutput) {
            System.out.println();
        }

        return propertySheet;
    }
}
