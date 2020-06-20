package de.uniwue.informatik.praline.pseudocircuitplans.generatingnewplans;

import de.uniwue.informatik.praline.datastructure.graphs.Graph;
import de.uniwue.informatik.praline.datastructure.utils.Serialization;

import java.io.FileWriter;
import java.io.IOException;

public class IOUtils {
    public static void saveGraphAsJson(Graph graph, String targetFilePath) throws IOException {
        FileWriter fileWriter = new FileWriter(targetFilePath);
        fileWriter.write(Serialization.write(graph));
        fileWriter.close();
    }
}
