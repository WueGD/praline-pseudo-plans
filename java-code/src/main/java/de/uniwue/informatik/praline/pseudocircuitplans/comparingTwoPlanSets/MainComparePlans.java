package de.uniwue.informatik.praline.pseudocircuitplans.comparingTwoPlanSets;

import de.uniwue.informatik.praline.pseudocircuitplans.analyzingoriginalplans.MainReadPlans;
import de.uniwue.informatik.praline.pseudocircuitplans.properties.DataSetProperties;

import java.io.File;
import java.io.IOException;

public class MainComparePlans {

    private final static String PATH_DATA_SET_0 = "data" + File.separator + "largest-comp-praline-package-2020-05-18";
    private final static String PATH_DATA_SET_1 = "data" + File.separator + "generated_2020-08-20_04-42-39";
//    private final static String PATH_DATA_SET_0 = "data" + File.separator + "example-for-visualization-very-small";
//    private final static String PATH_DATA_SET_1 = "data" + File.separator + "generated_2020-06-04_21-01-37";
//    private final static String PATH_DATA_SET_0 = "data" + File.separator + "example-for-visualization-others2";
//    private final static String PATH_DATA_SET_1 = "data" + File.separator + "generated_2020-07-08_02-38-32";

//    private final static String PATH_DATA_SET_0 = "data" + File.separator + "praline-package-2020-05-18";
//    private final static String PATH_DATA_SET_1 = "data" + File.separator + "generated_2020-06-02_05-34-09";

    public static void main(String[] args) throws IOException {
        DataSetProperties properties0 = MainReadPlans.getDataSetProperties(PATH_DATA_SET_0, false);
        DataSetProperties properties1 = MainReadPlans.getDataSetProperties(PATH_DATA_SET_1, false);

        //text output for the complete statistics
        System.out.println();
        System.out.println("Evaluated " + PATH_DATA_SET_0 + "(" + properties0.size() + " plans) | " + PATH_DATA_SET_1
                + "(" + properties1.size() + " plans).");
        System.out.println();
        System.out.println("Statistics:");
        System.out.println();
        MainReadPlans.textOutputStatistics(properties0, properties1);
    }
}
