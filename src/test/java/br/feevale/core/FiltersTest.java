package br.feevale.core;

import br.feevale.utils.ImageUtils;
import org.junit.Test;

import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;

public class FiltersTest {

    @Test
    public void testWeightedGrayscaleKeepsExpectedLuminanceScale() {
        BufferedImage image = ImageUtils.createImage(1, 1);
        image.setRGB(0, 0, ImageUtils.toARGB(255, 255, 255, 255));

        BufferedImage result = Filters.grayscale(image, Filters.GrayscaleType.WEIGHTED_2125_7154_0721_DIV3);
        int gray = ImageUtils.getRed(result.getRGB(0, 0));

        assertEquals(255, gray);
    }
}

