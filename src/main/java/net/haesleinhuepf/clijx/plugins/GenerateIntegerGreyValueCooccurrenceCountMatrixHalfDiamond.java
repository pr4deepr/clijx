package net.haesleinhuepf.clijx.plugins;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.CLIJOpenCLProcessor;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import net.haesleinhuepf.clij2.AbstractCLIJ2Plugin;
import net.haesleinhuepf.clij2.CLIJ2;
import org.scijava.plugin.Plugin;

import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Author: @haesleinhuepf
 *         August 2020
 */
@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_generateIntegerGreyValueCooccurrenceCountMatrixHalfDiamond")
public class GenerateIntegerGreyValueCooccurrenceCountMatrixHalfDiamond extends AbstractCLIJ2Plugin implements CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation {

    @Override
    public boolean executeCL() {
        boolean result = generateIntegerGreyValueCooccurrenceCountMatrixHalfDiamond(getCLIJ2(), (ClearCLBuffer)( args[0]), (ClearCLBuffer)(args[1]));
        return result;
    }

    public static boolean generateIntegerGreyValueCooccurrenceCountMatrixHalfDiamond(CLIJ2 clij2, ClearCLBuffer src_label_map1, ClearCLBuffer dst_touch_count_matrix) {
        int num_threads = (int) src_label_map1.getDepth();

        long[][][] counts = new long[num_threads][(int)dst_touch_count_matrix.getWidth()][(int)dst_touch_count_matrix.getHeight()];

        Thread[] threads = new Thread[num_threads];
        Statistician[] statisticians = new Statistician[num_threads];

        ArrayList<float[]> buffers = new ArrayList<>();

        for (int i = 0; i < num_threads; i++) {
            float[] labels_1;
            if (i == 0) {
                ClearCLBuffer label_map_1_slice = clij2.create(src_label_map1.getWidth(), src_label_map1.getHeight());
                clij2.copySlice(src_label_map1, label_map_1_slice, i);
                labels_1 = new float[(int) (label_map_1_slice.getWidth() * label_map_1_slice.getHeight())];
                label_map_1_slice.writeTo(FloatBuffer.wrap(labels_1), true);

                buffers.add(labels_1);
            } else {
                labels_1 = buffers.get(i);
            }

            float[] labels_2;
            if (i < num_threads - 1) {
                ClearCLBuffer label_map_2_slice = clij2.create(src_label_map1.getWidth(), src_label_map1.getHeight());
                clij2.copySlice(src_label_map1, label_map_2_slice, i + 1);
                labels_2 = new float[(int) (label_map_2_slice.getWidth() * label_map_2_slice.getHeight())];
                label_map_2_slice.writeTo(FloatBuffer.wrap(labels_2), true);

                buffers.add(labels_2);
            } else {
                labels_2 = null;
            }


            statisticians[i] = new Statistician(counts[i], clij2, labels_1, labels_2, (int)src_label_map1.getWidth(), (int)src_label_map1.getHeight(), i);
            threads[i] = new Thread(statisticians[i]);
            threads[i].start();
        }
        for (int i = 0; i < num_threads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        buffers.clear();

        float[][] matrix = new float[(int)dst_touch_count_matrix.getWidth()][(int)dst_touch_count_matrix.getHeight()];

        for (int t = 0; t < num_threads; t++) {
            for (int j = 0; j < counts[0].length; j++) {
                for (int k = 0; k < counts[0][0].length; k++) {
                    matrix[j][k] += counts[t][j][k];
                }
            }
        }

        ClearCLBuffer countMatrix = clij2.pushMat(matrix);
        clij2.copy(countMatrix, dst_touch_count_matrix);
        countMatrix.close();

        return true;
    }

    @Override
    public String getParameterHelpText() {
        return "Image integer_image, ByRef Image grey_value_cooccurrence_matrix_destination";
    }


    private static class Statistician implements Runnable{


        private ClearCLBuffer label_map_1_slice;
        private ClearCLBuffer label_map_2_slice;
        private final int width;
        private final int height;
        private final int zPlane;
        private final CLIJ2 clij2;

        long[][] tps;

        private float[] labels_1;
        private float[] labels_2;

        Statistician(long[][] tps, CLIJ2 clij2, float[] labels_1, float[] labels_2, int width, int height, int zPlane) {
            this.tps = tps;
            this.labels_1 = labels_1;
            this.labels_2 = labels_2;
            this.width = width;
            this.height = height;

            this.zPlane = zPlane;
            this.clij2 = clij2;
        }

        @Override
        public void run() {

            int x = 0;
            int y = 0;
            for (int i = 0; i < labels_1.length; i++) {
                int label_1;
                int label_2;

                // right
                if (x < width - 1) {
                    label_1 = (int) labels_1[i];
                    label_2 = (int) labels_1[i + 1];
                    if (label_1 >= label_2) {
                        tps[label_1][label_2]++;
                    } else if (label_1 < label_2) {
                        tps[label_2][label_1]++;
                    }
                }
                // bottom
                if (y < height - 1) {
                    label_1 = (int) labels_1[i];
                    label_2 = (int) labels_1[i + width];
                    if (label_1 >= label_2) {
                        tps[label_1][label_2]++;
                    } else if (label_1 < label_2) {
                        tps[label_2][label_1]++;
                    }
                }

                // next plane
                if (label_map_2_slice != null) {
                    label_1 = (int) labels_1[i];
                    label_2 = (int) labels_2[i];
                    if (label_1 >= label_2) {
                        tps[label_1][label_2]++;
                    } else if (label_1 < label_2) {
                        tps[label_2][label_1]++;
                    }
                }

                x++;
                if (x >= width) {
                    x = 0;
                    y++;
                }
            }
        }
    }


    @Override
    public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input)
    {
        double maxValue = getCLIJ2().maximumOfAllPixels((ClearCLBuffer) args[0]) + 1;
        ClearCLBuffer output = clij.createCLBuffer(new long[]{(long)maxValue, (long)maxValue}, NativeTypeEnum.Float);
        return output;
    }

    @Override
    public String getDescription() {
        return "Takes an image and assumes its grey values are integers. It builds up a grey-level co-occurence matrix of neigboring (left, bottom, back) pixel intensities. \n\n"+
                "Major parts of this operation run on the CPU.";
    }

    @Override
    public String getAvailableForDimensions() {
        return "2D, 3D";
    }

    public static void main(String... args) {
        CLIJ2 clij2 = CLIJ2.getInstance();

        ClearCLBuffer buffer = clij2.pushString(
                "1 1 1\n" +
                      "2 2 2\n" +
                      "3 3 4");

        ClearCLBuffer matrix = clij2.create(5, 5);

        GenerateIntegerGreyValueCooccurrenceCountMatrixHalfDiamond.generateIntegerGreyValueCooccurrenceCountMatrixHalfDiamond(clij2, buffer, matrix);

        clij2.print(matrix);

    }

}
