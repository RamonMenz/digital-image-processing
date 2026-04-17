package br.feevale.core;

import br.feevale.utils.ConvolutionUtils;
import br.feevale.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Filtros (processamento pontual) implementados manualmente, operando diretamente nos pixels.
 *
 * <p>Todos os metodos retornam uma nova imagem e preservam o canal alpha,
 * exceto quando explicitado de outra forma.</p>
 */
public final class Filters {

    private Filters() {
        throw new IllegalStateException("Utility class.");
    }

    /**
     * Brilho e contraste via: D(x,y) = C * f(x,y) + B
     *
     * <ul>
     *   <li>C: contraste (ganho). Ex.: 1.0 mantém, &gt;1 aumenta, entre 0 e 1 reduz.</li>
     *   <li>B: brilho (offset). Ex.: 0 mantém, &gt;0 clareia, &lt;0 escurece.</li>
     * </ul>
     *
     * <p>O cálculo é aplicado em cada canal (R,G,B) e o resultado é clampeado para [0,255].
     * O alpha é preservado.</p>
     *
     * @param image imagem de entrada
     * @param contrast fator de contraste (ganho)
     * @param brightness offset de brilho aplicado apos o ganho
     * @return nova imagem com brilho e contraste ajustados
     */
    public static BufferedImage adjustBrightnessContrast(
            final BufferedImage image,
            final double contrast, final int brightness
    ) {
        final BufferedImage result = ImageUtils.createImage(image.getWidth(), image.getHeight());
        final int[] srcPixels = ImageUtils.getPixels(image);
        final int[] dstPixels = new int[srcPixels.length];

        for (int i = 0; i < srcPixels.length; i++) {
            final int argb = srcPixels[i];
            final int a = ImageUtils.getAlpha(argb);

            final int r = ImageUtils.getRed(argb);
            final int g = ImageUtils.getGreen(argb);
            final int b = ImageUtils.getBlue(argb);

            final int nr = ImageUtils.clamp((int) Math.round(contrast * r + brightness));
            final int ng = ImageUtils.clamp((int) Math.round(contrast * g + brightness));
            final int nb = ImageUtils.clamp((int) Math.round(contrast * b + brightness));

            dstPixels[i] = ImageUtils.toARGB(a, nr, ng, nb);
        }

        ImageUtils.setPixels(result, dstPixels);
        return result;
    }

    /**
     * Tipos de conversao para tons de cinza.
     */
    public enum GrayscaleType {
        /**
         * Media aritmetica simples: (R + G + B) / 3.
         */
        AVERAGE,
        /**
         * Media ponderada: (R*0,2125 + G*0,7154 + B*0,0721).
         */
        WEIGHTED_2125_7154_0721_DIV3,
        /**
         * Media ponderada: (R*0,50 + G*0,419 + B*0,081).
         */
        WEIGHTED_0500_0419_0081_DIV3
    }

    /**
     * Converte para tons de cinza usando o tipo informado. Preserva alpha.
     *
     * @param image imagem de entrada
     * @param type estrategia de conversao para tons de cinza
     * @return nova imagem em escala de cinza
     */
    public static BufferedImage grayscale(final BufferedImage image, final GrayscaleType type) {
        final BufferedImage result = ImageUtils.createImage(image.getWidth(), image.getHeight());
        final int[] srcPixels = ImageUtils.getPixels(image);
        final int[] dstPixels = new int[srcPixels.length];

        for (int i = 0; i < srcPixels.length; i++) {
            final int argb = srcPixels[i];
            final int a = ImageUtils.getAlpha(argb);

            final int r = ImageUtils.getRed(argb);
            final int g = ImageUtils.getGreen(argb);
            final int b = ImageUtils.getBlue(argb);

            final double gray;
            switch (type) {
                 case WEIGHTED_2125_7154_0721_DIV3 -> gray = (0.2125 * r + 0.7154 * g + 0.0721 * b);
                 case WEIGHTED_0500_0419_0081_DIV3 -> gray = (0.50 * r + 0.419 * g + 0.081 * b);
                default -> gray = (r + g + b) / 3.0;
            }

            final int clampedGray = ImageUtils.clamp((int) Math.round(gray));
            dstPixels[i] = ImageUtils.toARGB(a, clampedGray, clampedGray, clampedGray);
        }

        ImageUtils.setPixels(result, dstPixels);
        return result;
    }

    /**
     * Negativo: inverte RGB (255 - valor). Preserva alpha.
     *
     * @param image imagem de entrada
     * @return nova imagem com cores invertidas
     */
    public static BufferedImage negative(final BufferedImage image) {
        final BufferedImage result = ImageUtils.createImage(image.getWidth(), image.getHeight());
        final int[] srcPixels = ImageUtils.getPixels(image);
        final int[] dstPixels = new int[srcPixels.length];

        for (int i = 0; i < srcPixels.length; i++) {
            final int argb = srcPixels[i];
            final int a = ImageUtils.getAlpha(argb);

            final int r = 255 - ImageUtils.getRed(argb);
            final int g = 255 - ImageUtils.getGreen(argb);
            final int b = 255 - ImageUtils.getBlue(argb);

            dstPixels[i] = ImageUtils.toARGB(a, r, g, b);
        }

        ImageUtils.setPixels(result, dstPixels);
        return result;
    }

    /**
     * Tipos de kernel de suavizacao usados em passa-baixa e unsharp mask.
     */
    public enum BlurType {
        /**
         * Kernel uniforme (box blur).
         */
        BOX,
        /**
         * Kernel gaussiano com pesos concentrados no centro.
         */
        GAUSSIAN
    }

    /**
     * Passa-baixa (suavização) via convolução por canal (RGB) com borda replicada.
     *
     * @param image imagem de entrada
     * @param type tipo de kernel de suavizacao
     * @param size tamanho do kernel (ímpar: 3,5,7,...)
     * @return nova imagem suavizada
     */
    public static BufferedImage lowPass(final BufferedImage image, final BlurType type, final int size) {
        final int kernelSize = ConvolutionUtils.normalizeOddKernelSize(size);
        final double[][] kernel = ConvolutionUtils.lowPassKernel(type, kernelSize);
        return ConvolutionUtils.convolveRGB(image, kernel);
    }

    /**
     * Passa-alta (aguçamento) via convolução com kernel de Unsharp Mask.
     *
     * @param image imagem de entrada
     * @param blurType tipo de blur usado para montar a mascara
     * @param blurSize tamanho do kernel de blur (par sera normalizado para impar)
     * @param amount intensidade do aguçamento (valores negativos sao tratados como 0)
     * @return nova imagem com realce de detalhes
     */
    public static BufferedImage highPass(
            final BufferedImage image, final BlurType blurType,
            final int blurSize, final double amount
    ) {
        final double a = Math.max(0.0, amount);
        final int kernelSize = ConvolutionUtils.normalizeOddKernelSize(blurSize);
        final double[][] blurKernel = ConvolutionUtils.lowPassKernel(blurType, kernelSize);
        final double[][] sharpenKernel = ConvolutionUtils.unsharpMaskKernel(blurKernel, a);
        return ConvolutionUtils.convolveRGB(image, sharpenKernel);
    }


    /**
     * Threshold (binarização) com base na luminância.
     * Pixels com Y &gt;= threshold viram branco, senão preto. Preserva alpha.
     *
     * @param image imagem de entrada
     * @param threshold limiar em [0,255]
     * @return nova imagem binarizada
     */
    public static BufferedImage threshold(final BufferedImage image, final int threshold) {
        final BufferedImage out = ImageUtils.createImage(image.getWidth(), image.getHeight());
        final int[] srcPixels = ImageUtils.getPixels(image);
        final int[] dstPixels = new int[srcPixels.length];

        for (int i = 0; i < srcPixels.length; i++) {
            final int argb = srcPixels[i];
            final int a = ImageUtils.getAlpha(argb);

            final int r = ImageUtils.getRed(argb);
            final int g = ImageUtils.getGreen(argb);
            final int b = ImageUtils.getBlue(argb);

            final int intensity = (int) Math.round((r + g + b) / 3.0);
            final int value = (intensity >= ImageUtils.clamp(threshold)) ? 255 : 0;
            dstPixels[i] = ImageUtils.toARGB(a, value, value, value);
        }

        ImageUtils.setPixels(out, dstPixels);
        return out;
    }

}



