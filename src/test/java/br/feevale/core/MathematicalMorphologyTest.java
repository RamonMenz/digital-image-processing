package br.feevale.core;

import br.feevale.utils.ImageUtils;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MathematicalMorphologyTest {

    @Test
    public void testDilationExpandsSinglePixel() {
        BufferedImage image = binaryImage(new int[][]{
                {0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0},
                {0, 0, 1, 0, 0},
                {0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0}
        });

        BufferedImage result = MathematicalMorphology.dilate(image, 3, 1, 128);

        assertMatrixEquals(new int[][]{
                {0, 0, 0, 0, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 0, 0, 0, 0}
        }, toBinaryMatrix(result));
    }

    @Test
    public void testErosionShrinksSolidBlock() {
        BufferedImage image = binaryImage(new int[][]{
                {0, 0, 0, 0, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 0, 0, 0, 0}
        });

        BufferedImage result = MathematicalMorphology.erode(image, 3, 1, 128);

        assertMatrixEquals(new int[][]{
                {0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0},
                {0, 0, 1, 0, 0},
                {0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0}
        }, toBinaryMatrix(result));
    }

    @Test
    public void testOpeningRemovesIsolatedNoise() {
        BufferedImage image = binaryImage(new int[][]{
                {1, 0, 0, 0, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 0, 0, 0, 0}
        });

        BufferedImage result = MathematicalMorphology.opening(image, 3, 1, 128);

        assertMatrixEquals(new int[][]{
                {0, 0, 0, 0, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 0, 0, 0, 0}
        }, toBinaryMatrix(result));
    }

    @Test
    public void testClosingFillsSmallHole() {
        BufferedImage image = binaryImage(new int[][]{
                {0, 0, 0, 0, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 0, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 0, 0, 0, 0}
        });

        BufferedImage result = MathematicalMorphology.closing(image, 3, 1, 128);

        assertMatrixEquals(new int[][]{
                {0, 0, 0, 0, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 0, 0, 0, 0}
        }, toBinaryMatrix(result));
    }

    @Test
    public void testThinningReducesThickness() {
        BufferedImage image = binaryImage(new int[][]{
                {0, 0, 0, 0, 0, 0, 0},
                {0, 0, 1, 1, 1, 0, 0},
                {0, 0, 1, 1, 1, 0, 0},
                {0, 0, 1, 1, 1, 0, 0},
                {0, 0, 1, 1, 1, 0, 0},
                {0, 0, 1, 1, 1, 0, 0},
                {0, 0, 0, 0, 0, 0, 0}
        });

        BufferedImage result = MathematicalMorphology.thinning(image, 128, 0);

        int foregroundBefore = countForeground(image);
        int foregroundAfter = countForeground(result);
        assertTrue(foregroundAfter < foregroundBefore);
        assertEquals(1, toBinaryMatrix(result)[3][3]);
    }

    @Test
    public void testContourExtractionReturnsBorderOnly() {
        BufferedImage image = binaryImage(new int[][]{
                {0, 0, 0, 0, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 0, 0, 0, 0}
        });

        BufferedImage result = MathematicalMorphology.extractContour(image, 3, 128);

        assertMatrixEquals(new int[][]{
                {0, 0, 0, 0, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 0, 1, 0},
                {0, 1, 1, 1, 0},
                {0, 0, 0, 0, 0}
        }, toBinaryMatrix(result));
    }

    @Test
    public void testThinningWorksForNonSquareImage() {
        BufferedImage image = binaryImage(new int[][]{
                {0, 0, 1, 1, 1, 0, 0, 0},
                {0, 0, 1, 1, 1, 0, 0, 0},
                {0, 0, 1, 1, 1, 0, 0, 0},
                {0, 0, 1, 1, 1, 0, 0, 0},
                {0, 0, 1, 1, 1, 0, 0, 0}
        });

        BufferedImage result = MathematicalMorphology.thinning(image, 128, 0);

        assertEquals(8, result.getWidth());
        assertEquals(5, result.getHeight());
        assertTrue(countForeground(result) > 0);
    }

    private static BufferedImage binaryImage(final int[][] matrix) {
        final int height = matrix.length;
        final int width = matrix[0].length;
        BufferedImage image = ImageUtils.createImage(width, height);
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = matrix[y][x] == 1 ? 255 : 0;
                pixels[(y * width) + x] = ImageUtils.toARGB(255, value, value, value);
            }
        }

        ImageUtils.setPixels(image, pixels);
        return image;
    }

    private static int[][] toBinaryMatrix(final BufferedImage image) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int[] pixels = ImageUtils.getPixels(image);
        final int[][] matrix = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[(y * width) + x];
                matrix[y][x] = ImageUtils.getRed(pixel) >= 128 ? 1 : 0;
            }
        }

        return matrix;
    }

    private static int countForeground(final BufferedImage image) {
        int count = 0;
        int[] pixels = ImageUtils.getPixels(image);
        for (int pixel : pixels) {
            if (ImageUtils.getRed(pixel) >= 128) {
                count++;
            }
        }
        return count;
    }

    private static void assertMatrixEquals(final int[][] expected, final int[][] actual) {
        assertEquals(expected.length, actual.length);
        assertEquals(expected[0].length, actual[0].length);

        for (int y = 0; y < expected.length; y++) {
            for (int x = 0; x < expected[0].length; x++) {
                assertEquals("Falha em (x=" + x + ", y=" + y + ")", expected[y][x], actual[y][x]);
            }
        }
    }
}

