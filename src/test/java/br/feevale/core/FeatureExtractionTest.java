package br.feevale.core;

import br.feevale.utils.ImageUtils;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FeatureExtractionTest {

    @Test
    public void testSobelDetectsVerticalEdge() {
        BufferedImage image = grayImage(new int[][]{
                {0, 0, 255, 255, 255},
                {0, 0, 255, 255, 255},
                {0, 0, 255, 255, 255},
                {0, 0, 255, 255, 255},
                {0, 0, 255, 255, 255}
        });

        BufferedImage result = FeatureExtraction.detectEdgesSobel(image, 80);

        int edgePixels = countForeground(result, 128);
        assertTrue(edgePixels >= 5);
    }

    @Test
    public void testCannyDetectsSquareContour() {
        BufferedImage image = grayImage(new int[][]{
                {0, 0, 0, 0, 0, 0, 0},
                {0, 255, 255, 255, 255, 255, 0},
                {0, 255, 255, 255, 255, 255, 0},
                {0, 255, 255, 255, 255, 255, 0},
                {0, 255, 255, 255, 255, 255, 0},
                {0, 255, 255, 255, 255, 255, 0},
                {0, 0, 0, 0, 0, 0, 0}
        });

        BufferedImage result = FeatureExtraction.detectEdgesCanny(image, 20, 60);

        int edgePixels = countForeground(result, 128);
        assertTrue(edgePixels > 0);
    }

    @Test
    public void testCannyOnFlatImageWithZeroThresholdsProducesNoEdges() {
        BufferedImage image = grayImage(new int[][]{
                {120, 120, 120, 120, 120},
                {120, 120, 120, 120, 120},
                {120, 120, 120, 120, 120},
                {120, 120, 120, 120, 120},
                {120, 120, 120, 120, 120}
        });

        BufferedImage result = FeatureExtraction.detectEdgesCanny(image, 0, 0);

        int edgePixels = countForeground(result, 128);
        assertEquals(0, edgePixels);
    }

    @Test
    public void testObjectCountWithConnectivity() {
        BufferedImage image = grayImage(new int[][]{
                {255, 0, 0},
                {0, 255, 0},
                {0, 0, 255}
        });

        int four = FeatureExtraction.countObjects(image, 128, FeatureExtraction.Connectivity.FOUR, 1);
        int eight = FeatureExtraction.countObjects(image, 128, FeatureExtraction.Connectivity.EIGHT, 1);

        assertEquals(3, four);
        assertEquals(1, eight);
    }

    @Test
    public void testHistogramCountsPixelsPerIntensity() {
        BufferedImage image = grayImage(new int[][]{
                {0, 0},
                {255, 255}
        });

        int[] histogram = FeatureExtraction.intensityHistogram(image);

        assertEquals(2, histogram[0]);
        assertEquals(2, histogram[255]);
    }

    @Test
    public void testHistogramRenderingHasExpectedSize() {
        int[] histogram = new int[256];
        histogram[100] = 50;
        histogram[200] = 100;

        BufferedImage image = FeatureExtraction.renderIntensityHistogram(histogram, 400, 200);

        assertEquals(400, image.getWidth());
        assertEquals(200, image.getHeight());
    }

    @Test
    public void testHarrisCornersFindsAtLeastOneCorner() {
        BufferedImage image = grayImage(new int[][]{
                {0, 0, 0, 0, 0, 0, 0},
                {0, 255, 255, 255, 255, 255, 0},
                {0, 255, 255, 255, 255, 255, 0},
                {0, 255, 255, 255, 255, 255, 0},
                {0, 255, 255, 255, 255, 255, 0},
                {0, 255, 255, 255, 255, 255, 0},
                {0, 0, 0, 0, 0, 0, 0}
        });

        FeatureExtraction.CornerDetectionResult result = FeatureExtraction.detectCornersHarris(image, 0.05, 1);

        assertTrue(result.cornerCount() > 0);
    }

    private static BufferedImage grayImage(final int[][] matrix) {
        final int height = matrix.length;
        final int width = matrix[0].length;
        final BufferedImage image = ImageUtils.createImage(width, height);
        final int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int value = matrix[y][x];
                pixels[(y * width) + x] = ImageUtils.toARGB(255, value, value, value);
            }
        }

        ImageUtils.setPixels(image, pixels);
        return image;
    }

    private static int countForeground(final BufferedImage image, final int threshold) {
        int count = 0;
        final int[] pixels = ImageUtils.getPixels(image);

        for (int pixel : pixels) {
            if (ImageUtils.getRed(pixel) >= threshold) {
                count++;
            }
        }

        return count;
    }
}

