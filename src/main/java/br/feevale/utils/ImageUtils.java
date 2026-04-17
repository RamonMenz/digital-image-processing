package br.feevale.utils;

import java.awt.image.BufferedImage;

/**
 * Utilitários para manipulação de imagens.
 * Contém métodos auxiliares para extração e manipulação de componentes de cor.
 */
public class ImageUtils {

    /**
     * Extrai o componente vermelho (R) de um valor ARGB.
     */
    public static int getRed(int argb) {
        return (argb >> 16) & 0xFF;
    }

    /**
     * Extrai o componente verde (G) de um valor ARGB.
     */
    public static int getGreen(int argb) {
        return (argb >> 8) & 0xFF;
    }

    /**
     * Extrai o componente azul (B) de um valor ARGB.
     */
    public static int getBlue(int argb) {
        return argb & 0xFF;
    }

    /**
     * Extrai o componente alpha (A) de um valor ARGB.
     */
    public static int getAlpha(int argb) {
        return (argb >> 24) & 0xFF;
    }

    /**
     * Combina componentes ARGB em um único valor inteiro.
     */
    public static int toARGB(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * Limita um valor ao intervalo [0, 255].
     */
    public static int clamp(int value) {
        return Math.clamp(value, 0, 255);
    }

    /**
     * Extrai os pixels de uma imagem como array de inteiros ARGB.
     * Mais eficiente para processamento em lote.
     */
    public static int[] getPixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    /**
     * Define os pixels de uma imagem a partir de um array de inteiros ARGB.
     */
    public static void setPixels(BufferedImage image, int[] pixels) {
        image.setRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
    }

    /**
     * Cria uma nova BufferedImage com as dimensões especificadas.
     */
    public static BufferedImage createImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Cria uma cópia de uma imagem.
     */
    public static BufferedImage copyImage(BufferedImage source) {
        if (source == null) return null;

        BufferedImage copy = createImage(source.getWidth(), source.getHeight());
        int[] pixels = getPixels(source);
        setPixels(copy, pixels);

        return copy;
    }

    /**
     * Verifica se as coordenadas estão dentro dos limites da imagem.
     */
    public static boolean isValidCoordinate(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

}
