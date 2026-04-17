package br.feevale.core;

import br.feevale.utils.ImageUtils;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GeometricTransformationsTest {

    private BufferedImage testImage;

    @Before
    public void setUp() {
        testImage = ImageUtils.createImage(100, 100);
        int[] pixels = new int[100 * 100];

        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                int gray = (x + y) % 256;
                pixels[(y * 100) + x] = ImageUtils.toARGB(255, gray, gray, gray);
            }
        }

        ImageUtils.setPixels(testImage, pixels);
    }

    @Test
    public void testMirrorHorizontal() {
        BufferedImage result = GeometricTransformations.mirrorHorizontal(testImage);

        assertNotNull(result);
        assertEquals(testImage.getWidth(), result.getWidth());
        assertEquals(testImage.getHeight(), result.getHeight());

        int originalPixel = testImage.getRGB(99, 0);
        int mirroredPixel = result.getRGB(0, 0);
        assertEquals(originalPixel, mirroredPixel);
    }

    @Test
    public void testMirrorVertical() {
        BufferedImage result = GeometricTransformations.mirrorVertical(testImage);

        assertNotNull(result);
        assertEquals(testImage.getWidth(), result.getWidth());
        assertEquals(testImage.getHeight(), result.getHeight());

        int originalPixel = testImage.getRGB(0, 99);
        int mirroredPixel = result.getRGB(0, 0);
        assertEquals(originalPixel, mirroredPixel);
    }

    @Test
    public void testScaleUp() {
        BufferedImage result = GeometricTransformations.scale(testImage, 2.0, 2.0);

        assertNotNull(result);
        assertEquals(200, result.getWidth());
        assertEquals(200, result.getHeight());
    }

    @Test
    public void testScaleUpNonUniform() {
        BufferedImage result = GeometricTransformations.scale(testImage, 2.0, 1.5);

        assertNotNull(result);
        assertEquals(200, result.getWidth());
        assertEquals(150, result.getHeight());
    }

    @Test
    public void testScaleDown() {
        BufferedImage result = GeometricTransformations.scale(testImage, 1 / 2.0, 1 / 2.0);

        assertNotNull(result);
        assertEquals(50, result.getWidth());
        assertEquals(50, result.getHeight());
    }

    @Test
    public void testScaleDownNonUniform() {
        BufferedImage result = GeometricTransformations.scale(testImage, 1 / 2.0, 1 / 4.0);

        assertNotNull(result);
        assertEquals(50, result.getWidth());
        assertEquals(25, result.getHeight());
    }

    @Test
    public void testTranslate() {
        BufferedImage result = GeometricTransformations.translate(testImage, 10, 10);

        assertNotNull(result);
        assertEquals(testImage.getWidth(), result.getWidth());
        assertEquals(testImage.getHeight(), result.getHeight());
    }

    @Test
    public void testRotate() {
        BufferedImage result = GeometricTransformations.rotateKeepSize(testImage, 90);

        assertNotNull(result);
        assertEquals(testImage.getWidth(), result.getWidth());
        assertEquals(testImage.getHeight(), result.getHeight());
    }

    @Test
    public void testRotateDirection90Degrees() {
        BufferedImage img = ImageUtils.createImage(2, 2);
        int a = ImageUtils.toARGB(255, 255, 0, 0);
        int b = ImageUtils.toARGB(255, 0, 255, 0);
        int c = ImageUtils.toARGB(255, 0, 0, 255);
        int d = ImageUtils.toARGB(255, 255, 255, 0);
        img.setRGB(0, 0, a);
        img.setRGB(1, 0, b);
        img.setRGB(0, 1, c);
        img.setRGB(1, 1, d);

        BufferedImage r90 = GeometricTransformations.rotateKeepSize(img, 90);
        assertNotNull(r90);
        assertEquals(2, r90.getWidth());
        assertEquals(2, r90.getHeight());
        assertEquals(c, r90.getRGB(0, 0));
        assertEquals(a, r90.getRGB(1, 0));
        assertEquals(d, r90.getRGB(0, 1));
        assertEquals(b, r90.getRGB(1, 1));

        BufferedImage rNeg90 = GeometricTransformations.rotateKeepSize(img, -90);
        assertNotNull(rNeg90);
        assertEquals(2, rNeg90.getWidth());
        assertEquals(2, rNeg90.getHeight());
        assertEquals(b, rNeg90.getRGB(0, 0));
        assertEquals(d, rNeg90.getRGB(1, 0));
        assertEquals(a, rNeg90.getRGB(0, 1));
        assertEquals(c, rNeg90.getRGB(1, 1));
    }
}

