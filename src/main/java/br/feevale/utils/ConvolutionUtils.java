package br.feevale.utils;

import br.feevale.core.Filters;

import java.awt.image.BufferedImage;

/**
 * Utilitarios para operacoes de convolucao em imagens ARGB.
 */
public final class ConvolutionUtils {

    private ConvolutionUtils() {
        throw new IllegalStateException("Utility class.");
    }

    public static int normalizeOddKernelSize(final int size) {
        int k = Math.max(1, size);
        if (k % 2 == 0) k++;
        return k;
    }

    public static double[][] lowPassKernel(final Filters.BlurType type, final int kernelSize) {
        return (type == Filters.BlurType.GAUSSIAN)
                ? ConvolutionUtils.gaussianKernel(kernelSize)
                : ConvolutionUtils.boxKernel(kernelSize);
    }

    public static double[][] boxKernel(final int size) {
        final double[][] k = new double[size][size];
        final double v = 1.0 / (size * (double) size);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                k[x][y] = v;
            }
        }
        return k;
    }

    public static double[][] gaussianKernel(final int size) {
        final int radius = size / 2;
        final double sigma = Math.max(0.1, radius / 2.0);
        final double twoSigma2 = 2.0 * sigma * sigma;

        final double[][] k = new double[size][size];
        double sum = 0.0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                final double val = Math.exp(-(x * x + y * y) / twoSigma2);
                k[x + radius][y + radius] = val;
                sum += val;
            }
        }

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                k[x][y] /= sum;
            }
        }
        return k;
    }

    /**
     * Constroi um kernel de unsharp a partir de um kernel de blur.
     * Formula linear equivalente: (1 + amount) * I - amount * blur.
     */
    public static double[][] unsharpMaskKernel(final double[][] blurKernel, final double amount) {
        final int h = blurKernel.length;
        final int w = blurKernel[0].length;
        final double a = Math.max(0.0, amount);
        final double[][] k = new double[h][w];

        for (int x = 0; x < h; x++) {
            for (int y = 0; y < w; y++) {
                k[x][y] = -a * blurKernel[x][y];
            }
        }

        k[h / 2][w / 2] += (1.0 + a);
        return k;
    }

    /**
     * Convolucao por canal (RGB), preservando alpha e usando borda replicada.
     */
    public static BufferedImage convolveRGB(final BufferedImage source, final double[][] kernel) {
        final int w = source.getWidth();
        final int h = source.getHeight();
        final int[] src = ImageUtils.getPixels(source);
        final int[] dst = new int[src.length];
        final BufferedImage out = ImageUtils.createImage(w, h);

        final int kh = kernel.length;
        final int kw = kernel[0].length;
        final int ry = kh / 2;
        final int rx = kw / 2;

        for (int x = 0; x < h; x++) {
            for (int y = 0; y < w; y++) {
                double accR = 0.0;
                double accG = 0.0;
                double accB = 0.0;

                final int center = src[x * w + y];
                final int a = ImageUtils.getAlpha(center);

                for (int kx = 0; kx < kh; kx++) {
                    final int sx = clampCoord(x + (kx - ry), 0, h - 1);
                    for (int ky = 0; ky < kw; ky++) {
                        final int sy = clampCoord(y + (ky - rx), 0, w - 1);
                        final int p = src[sx * w + sy];
                        final double kv = kernel[kx][ky];
                        accR += ImageUtils.getRed(p) * kv;
                        accG += ImageUtils.getGreen(p) * kv;
                        accB += ImageUtils.getBlue(p) * kv;
                    }
                }

                final int nr = ImageUtils.clamp((int) Math.round(accR));
                final int ng = ImageUtils.clamp((int) Math.round(accG));
                final int nb = ImageUtils.clamp((int) Math.round(accB));
                dst[x * w + y] = ImageUtils.toARGB(a, nr, ng, nb);
            }
        }

        ImageUtils.setPixels(out, dst);
        return out;
    }

    private static int clampCoord(final int v, final int min, final int max) {
        return Math.max(min, Math.min(max, v));
    }
}


