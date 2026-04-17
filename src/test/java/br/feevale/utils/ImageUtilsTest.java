package br.feevale.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImageUtilsTest {

    @Test
    public void testGetRed() {
        int argb = ImageUtils.toARGB(255, 100, 150, 200);
        assertEquals(100, ImageUtils.getRed(argb));
    }

    @Test
    public void testGetGreen() {
        int argb = ImageUtils.toARGB(255, 100, 150, 200);
        assertEquals(150, ImageUtils.getGreen(argb));
    }

    @Test
    public void testGetBlue() {
        int argb = ImageUtils.toARGB(255, 100, 150, 200);
        assertEquals(200, ImageUtils.getBlue(argb));
    }

    @Test
    public void testGetAlpha() {
        int argb = ImageUtils.toARGB(128, 100, 150, 200);
        assertEquals(128, ImageUtils.getAlpha(argb));
    }

    @Test
    public void testClamp() {
        assertEquals(0, ImageUtils.clamp(-50));
        assertEquals(255, ImageUtils.clamp(300));
        assertEquals(128, ImageUtils.clamp(128));
    }
}

