package net.haesleinhuepf.clijx.plugins;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.CLIJOpenCLProcessor;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import net.haesleinhuepf.clij2.AbstractCLIJ2Plugin;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.utilities.IsCategorized;
import org.scijava.plugin.Plugin;

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_localMaximumAverageDistanceOfNClosestNeighborsMap")
public class LocalMaximumAverageDistanceOfNClosestNeighborsMap extends AbstractCLIJ2Plugin implements CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation, IsCategorized {

    @Override
    public String getParameterHelpText() {
        return "Image input, ByRef Image destination, Number n";
    }

    @Override
    public boolean executeCL() {
        return localMaximumAverageDistanceOfNClosestNeighborsMap(getCLIJ2(), (ClearCLBuffer) args[0], (ClearCLBuffer) args[1], asInteger(args[2]));
    }

    public static boolean localMaximumAverageDistanceOfNClosestNeighborsMap(CLIJ2 clij2, ClearCLBuffer pushed, ClearCLBuffer result, int n) {
        int number_of_labels = (int)clij2.maximumOfAllPixels(pushed);
        ClearCLBuffer touch_matrix = clij2.create(number_of_labels + 1, number_of_labels + 1);
        clij2.generateTouchMatrix(pushed, touch_matrix);


        ClearCLBuffer pointlist = clij2.create(number_of_labels, pushed.getDimension());
        clij2.centroidsOfLabels(pushed, pointlist);

        ClearCLBuffer distance_matrix = clij2.create(number_of_labels + 1, number_of_labels + 1);
        clij2.generateDistanceMatrix(pointlist, pointlist, distance_matrix);

        ClearCLBuffer distance_vector = clij2.create(number_of_labels + 1, 1, 1);
        clij2.averageDistanceOfNClosestPoints(distance_matrix, distance_vector, n);
        distance_matrix.close();
        pointlist.close();


        ClearCLBuffer maximum_vector = clij2.create(number_of_labels, 1, 1);
        clij2.maximumOfTouchingNeighbors(distance_vector, touch_matrix, maximum_vector);
        distance_vector.close();
        touch_matrix.close();


        clij2.replaceIntensities(pushed, maximum_vector, result);
        maximum_vector.close();

        return true;
    }

    @Override
    public String getDescription() {
        return "Takes a label map, determines distances between all centroids, the mean distance of the n closest points for every point\n" +
                " and replaces every label with the maximum distance of touching labels.";
    }

    @Override
    public String getAvailableForDimensions() {
        return "2D, 3D";
    }

    @Override
    public String getCategories() {
        return "Measurements, Neighbors";
    }
}
