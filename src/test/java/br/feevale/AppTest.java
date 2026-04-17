package br.feevale;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.junit.Test;

import br.feevale.core.GeometricTransformations;
import br.feevale.core.FeatureExtraction;
import br.feevale.utils.ImageUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testes de fumaça de alto nível da suíte.
 *
 * <p>Os testes detalhados ficam em classes dedicadas por domínio
 * (core e utils).</p>
 */
public class AppTest {

    @Test
    public void testCorePipelineSmoke() {
        BufferedImage image = ImageUtils.createImage(5, 5);
        int[] pixels = new int[25];
        Arrays.fill(pixels, ImageUtils.toARGB(255, 120, 120, 120));
        ImageUtils.setPixels(image, pixels);

        BufferedImage rotated = GeometricTransformations.rotateKeepSize(image, 15);
        BufferedImage edges = FeatureExtraction.detectEdgesSobel(rotated, 90);

        assertNotNull(rotated);
        assertNotNull(edges);
        assertEquals(5, rotated.getWidth());
        assertEquals(5, rotated.getHeight());
        assertEquals(5, edges.getWidth());
        assertEquals(5, edges.getHeight());
    }
}

