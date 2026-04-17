package br.feevale.core;

import br.feevale.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

/**
 * Operacoes de extracao de caracteristicas em imagens.
 */
public final class FeatureExtraction {

    private static final int[][] SOBEL_X = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
    };
    private static final int[][] SOBEL_Y = {
            {-1, -2, -1},
            {0, 0, 0},
            {1, 2, 1}
    };

    private FeatureExtraction() {
        throw new IllegalStateException("Utility class.");
    }

    public enum Connectivity {
        FOUR,
        EIGHT
    }

    public record CornerDetectionResult(BufferedImage image, int cornerCount) {
    }

    /**
     * Detecta bordas com Sobel e aplica threshold no modulo do gradiente.
     */
    public static BufferedImage detectEdgesSobel(final BufferedImage image, final int threshold) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int[] src = ImageUtils.getPixels(image);
        final int[] out = new int[src.length];
        final int thresholdValue = ImageUtils.clamp(threshold);
        final int[][] gray = toGrayscale(image);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int gx = sobelAt(gray, x, y, SOBEL_X);
                final int gy = sobelAt(gray, x, y, SOBEL_Y);
                final int magnitude = (int) Math.round(Math.min(255.0, Math.hypot(gx, gy)));
                final int value = magnitude >= thresholdValue ? 255 : 0;
                final int alpha = ImageUtils.getAlpha(src[(y * width) + x]);
                out[(y * width) + x] = ImageUtils.toARGB(alpha, value, value, value);
            }
        }

        final BufferedImage result = ImageUtils.createImage(width, height);
        ImageUtils.setPixels(result, out);
        return result;
    }

    /**
     * Detecta bordas com Canny (suavizacao gaussiana, NMS, histerese).
     */
    public static BufferedImage detectEdgesCanny(final BufferedImage image, final int lowThreshold, final int highThreshold) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int[] src = ImageUtils.getPixels(image);
        final int[] out = new int[src.length];

        final int low = ImageUtils.clamp(Math.min(lowThreshold, highThreshold));
        final int high = ImageUtils.clamp(Math.max(lowThreshold, highThreshold));

        final int[][] gray = toGrayscale(image);
        final double[][] smoothed = gaussianBlur5x5(gray);
        final double[][] gradX = new double[height][width];
        final double[][] gradY = new double[height][width];
        final double[][] magnitude = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final double gx = sobelAt(smoothed, x, y, SOBEL_X);
                final double gy = sobelAt(smoothed, x, y, SOBEL_Y);
                gradX[y][x] = gx;
                gradY[y][x] = gy;
                magnitude[y][x] = Math.hypot(gx, gy);
            }
        }

        final double[][] nms = nonMaximumSuppression(magnitude, gradX, gradY);
        final boolean[][] strong = new boolean[height][width];
        final boolean[][] weak = new boolean[height][width];

        double maxNms = 0.0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (nms[y][x] > maxNms) {
                    maxNms = nms[y][x];
                }
            }
        }

        // Sem energia de borda apos NMS: retorna resultado vazio para evitar falso-positivo global.
        if (maxNms <= 0.0) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final int alpha = ImageUtils.getAlpha(src[(y * width) + x]);
                    out[(y * width) + x] = ImageUtils.toARGB(alpha, 0, 0, 0);
                }
            }

            final BufferedImage result = ImageUtils.createImage(width, height);
            ImageUtils.setPixels(result, out);
            return result;
        }

        final double nmsScale = maxNms > 0.0 ? (255.0 / maxNms) : 0.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final double normalized = nms[y][x] * nmsScale;
                if (normalized >= high) {
                    strong[y][x] = true;
                } else if (normalized >= low) {
                    weak[y][x] = true;
                }
            }
        }

        final boolean[][] edges = hysteresis(strong, weak);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int value = edges[y][x] ? 255 : 0;
                final int alpha = ImageUtils.getAlpha(src[(y * width) + x]);
                out[(y * width) + x] = ImageUtils.toARGB(alpha, value, value, value);
            }
        }

        final BufferedImage result = ImageUtils.createImage(width, height);
        ImageUtils.setPixels(result, out);
        return result;
    }

    /**
     * Conta componentes conexos no foreground binarizado por threshold.
     */
    public static int countObjects(final BufferedImage image, final int threshold,
                                   final Connectivity connectivity, final int minArea) {
        final boolean[][] binary = toBinaryMatrix(image, threshold);
        final int height = binary.length;
        final int width = binary[0].length;
        final boolean[][] visited = new boolean[height][width];
        final int safeMinArea = Math.max(1, minArea);
        int count = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!binary[y][x] || visited[y][x]) {
                    continue;
                }

                final int area = floodFillArea(binary, visited, x, y, connectivity);
                if (area >= safeMinArea) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Gera histograma de intensidade (0..255) baseado em luminancia.
     */
    public static int[] intensityHistogram(final BufferedImage image) {
        final int[] histogram = new int[256];
        final int[] pixels = ImageUtils.getPixels(image);

        for (int argb : pixels) {
            final int intensity = intensity(argb);
            histogram[intensity]++;
        }

        return histogram;
    }

    /**
     * Renderiza uma imagem de histograma para exibicao no painel transformado.
     */
    public static BufferedImage renderIntensityHistogram(final int[] histogram, final int width, final int height) {
        final int safeWidth = Math.max(256, width);
        final int safeHeight = Math.max(120, height);
        final BufferedImage image = ImageUtils.createImage(safeWidth, safeHeight);
        final int[] pixels = new int[safeWidth * safeHeight];

        Arrays.fill(pixels, ImageUtils.toARGB(255, 20, 20, 20));

        int max = 0;
        for (int value : histogram) {
            max = Math.max(max, value);
        }
        if (max == 0) {
            ImageUtils.setPixels(image, pixels);
            return image;
        }

        final int topPadding = 12;
        final int bottomPadding = 20;
        final int leftPadding = 12;
        final int rightPadding = 12;
        final int chartWidth = safeWidth - leftPadding - rightPadding;
        final int chartHeight = safeHeight - topPadding - bottomPadding;
        final int baseY = topPadding + chartHeight;

        for (int x = 0; x < chartWidth; x++) {
            final int bin = (int) Math.round((x / (double) Math.max(1, chartWidth - 1)) * 255.0);
            final int count = histogram[Math.clamp(bin, 0, 255)];
            final int barHeight = (int) Math.round((count / (double) max) * chartHeight);

            for (int y = 0; y < barHeight; y++) {
                final int py = baseY - y;
                if (py >= 0 && py < safeHeight) {
                    pixels[(py * safeWidth) + (x + leftPadding)] = ImageUtils.toARGB(255, 230, 230, 230);
                }
            }
        }

        for (int x = leftPadding; x < safeWidth - rightPadding; x++) {
            pixels[(baseY * safeWidth) + x] = ImageUtils.toARGB(255, 170, 170, 170);
        }

        ImageUtils.setPixels(image, pixels);
        return image;
    }

    /**
     * Detecta cantos com Harris e desenha marcacoes vermelhas sobre a imagem.
     */
    public static CornerDetectionResult detectCornersHarris(final BufferedImage image, final double thresholdRatio,
                                                            final int suppressionRadius) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int[] src = ImageUtils.getPixels(image);
        final int[] out = src.clone();

        final double safeThresholdRatio = Math.clamp(thresholdRatio, 0.0001, 1.0);
        final int safeRadius = Math.max(1, suppressionRadius);

        final int[][] gray = toGrayscale(image);
        final double[][] ix = new double[height][width];
        final double[][] iy = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ix[y][x] = sobelAt(gray, x, y, SOBEL_X);
                iy[y][x] = sobelAt(gray, x, y, SOBEL_Y);
            }
        }

        final double[][] ixx = new double[height][width];
        final double[][] iyy = new double[height][width];
        final double[][] ixy = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ixx[y][x] = ix[y][x] * ix[y][x];
                iyy[y][x] = iy[y][x] * iy[y][x];
                ixy[y][x] = ix[y][x] * iy[y][x];
            }
        }

        final double[][] sxx = gaussianBlur3x3(ixx);
        final double[][] syy = gaussianBlur3x3(iyy);
        final double[][] sxy = gaussianBlur3x3(ixy);
        final double[][] response = new double[height][width];

        double maxResponse = Double.NEGATIVE_INFINITY;
        final double k = 0.04;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final double det = (sxx[y][x] * syy[y][x]) - (sxy[y][x] * sxy[y][x]);
                final double trace = sxx[y][x] + syy[y][x];
                final double r = det - (k * trace * trace);
                response[y][x] = r;
                if (r > maxResponse) {
                    maxResponse = r;
                }
            }
        }

        if (maxResponse <= 0) {
            return new CornerDetectionResult(image, 0);
        }

        final double thresholdValue = maxResponse * safeThresholdRatio;
        int corners = 0;

        for (int y = safeRadius; y < height - safeRadius; y++) {
            for (int x = safeRadius; x < width - safeRadius; x++) {
                final double candidate = response[y][x];
                if (candidate < thresholdValue) {
                    continue;
                }

                if (!isLocalMaximum(response, x, y, safeRadius, candidate)) {
                    continue;
                }

                corners++;
                paintCross(out, width, height, x, y, 2);
            }
        }

        final BufferedImage marked = ImageUtils.createImage(width, height);
        ImageUtils.setPixels(marked, out);
        return new CornerDetectionResult(marked, corners);
    }

    private static int[][] toGrayscale(final BufferedImage image) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int[] pixels = ImageUtils.getPixels(image);
        final int[][] gray = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                gray[y][x] = intensity(pixels[(y * width) + x]);
            }
        }

        return gray;
    }

    private static int intensity(final int argb) {
        final int r = ImageUtils.getRed(argb);
        final int g = ImageUtils.getGreen(argb);
        final int b = ImageUtils.getBlue(argb);
        return ImageUtils.clamp((int) Math.round(0.299 * r + 0.587 * g + 0.114 * b));
    }

    private static int sobelAt(final int[][] gray, final int x, final int y, final int[][] kernel) {
        int value = 0;
        for (int ky = -1; ky <= 1; ky++) {
            for (int kx = -1; kx <= 1; kx++) {
                final int sy = clampIndex(y + ky, gray.length);
                final int sx = clampIndex(x + kx, gray[0].length);
                value += gray[sy][sx] * kernel[ky + 1][kx + 1];
            }
        }
        return value;
    }

    private static double sobelAt(final double[][] gray, final int x, final int y, final int[][] kernel) {
        double value = 0.0;
        for (int ky = -1; ky <= 1; ky++) {
            for (int kx = -1; kx <= 1; kx++) {
                final int sy = clampIndex(y + ky, gray.length);
                final int sx = clampIndex(x + kx, gray[0].length);
                value += gray[sy][sx] * kernel[ky + 1][kx + 1];
            }
        }
        return value;
    }

    private static int clampIndex(final int value, final int length) {
        return Math.clamp(value, 0, length - 1);
    }

    private static double[][] gaussianBlur5x5(final int[][] source) {
        final int[] kernel1d = {1, 4, 6, 4, 1};
        final int height = source.length;
        final int width = source[0].length;
        final double[][] temp = new double[height][width];
        final double[][] out = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double sum = 0.0;
                for (int i = -2; i <= 2; i++) {
                    final int sx = clampIndex(x + i, width);
                    sum += source[y][sx] * kernel1d[i + 2];
                }
                temp[y][x] = sum / 16.0;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double sum = 0.0;
                for (int i = -2; i <= 2; i++) {
                    final int sy = clampIndex(y + i, height);
                    sum += temp[sy][x] * kernel1d[i + 2];
                }
                out[y][x] = sum / 16.0;
            }
        }

        return out;
    }

    private static double[][] gaussianBlur3x3(final double[][] source) {
        final double[][] kernel = {
                {1.0, 2.0, 1.0},
                {2.0, 4.0, 2.0},
                {1.0, 2.0, 1.0}
        };
        final int height = source.length;
        final int width = source[0].length;
        final double[][] out = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double sum = 0.0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        final int sy = clampIndex(y + ky, height);
                        final int sx = clampIndex(x + kx, width);
                        sum += source[sy][sx] * kernel[ky + 1][kx + 1];
                    }
                }
                out[y][x] = sum / 16.0;
            }
        }

        return out;
    }

    private static double[][] nonMaximumSuppression(final double[][] magnitude,
                                                     final double[][] gradX,
                                                     final double[][] gradY) {
        final int height = magnitude.length;
        final int width = magnitude[0].length;
        final double[][] out = new double[height][width];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                final double angle = Math.toDegrees(Math.atan2(gradY[y][x], gradX[y][x]));
                final double positiveAngle = angle < 0 ? angle + 180.0 : angle;
                final double current = magnitude[y][x];
                double n1;
                double n2;

                if ((positiveAngle >= 0 && positiveAngle < 22.5) || (positiveAngle >= 157.5 && positiveAngle <= 180)) {
                    n1 = magnitude[y][x - 1];
                    n2 = magnitude[y][x + 1];
                } else if (positiveAngle >= 22.5 && positiveAngle < 67.5) {
                    n1 = magnitude[y - 1][x + 1];
                    n2 = magnitude[y + 1][x - 1];
                } else if (positiveAngle >= 67.5 && positiveAngle < 112.5) {
                    n1 = magnitude[y - 1][x];
                    n2 = magnitude[y + 1][x];
                } else {
                    n1 = magnitude[y - 1][x - 1];
                    n2 = magnitude[y + 1][x + 1];
                }

                out[y][x] = (current >= n1 && current >= n2) ? current : 0.0;
            }
        }

        return out;
    }

    private static boolean[][] hysteresis(final boolean[][] strong, final boolean[][] weak) {
        final int height = strong.length;
        final int width = strong[0].length;
        final boolean[][] edges = new boolean[height][width];
        final Queue<int[]> queue = new ArrayDeque<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (strong[y][x]) {
                    edges[y][x] = true;
                    queue.add(new int[]{x, y});
                }
            }
        }

        while (!queue.isEmpty()) {
            final int[] point = queue.poll();
            final int cx = point[0];
            final int cy = point[1];

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    final int nx = cx + dx;
                    final int ny = cy + dy;

                    if (isNotInside(nx, ny, width, height) || edges[ny][nx] || !weak[ny][nx]) {
                        continue;
                    }

                    edges[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        return edges;
    }

    private static boolean[][] toBinaryMatrix(final BufferedImage image, final int threshold) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int[] pixels = ImageUtils.getPixels(image);
        final boolean[][] binary = new boolean[height][width];
        final int thresholdValue = ImageUtils.clamp(threshold);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int argb = pixels[(y * width) + x];
                final int a = ImageUtils.getAlpha(argb);
                final int intensity = intensity(argb);
                binary[y][x] = a > 0 && intensity >= thresholdValue;
            }
        }

        return binary;
    }

    private static int floodFillArea(final boolean[][] binary, final boolean[][] visited,
                                     final int startX, final int startY,
                                     final Connectivity connectivity) {
        final int width = binary[0].length;
        final int height = binary.length;
        final Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;
        int area = 0;

        while (!queue.isEmpty()) {
            final int[] point = queue.poll();
            final int x = point[0];
            final int y = point[1];
            area++;

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    if (connectivity == Connectivity.FOUR && Math.abs(dx) + Math.abs(dy) != 1) {
                        continue;
                    }

                    final int nx = x + dx;
                    final int ny = y + dy;

                    if (isNotInside(nx, ny, width, height) || visited[ny][nx] || !binary[ny][nx]) {
                        continue;
                    }

                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        return area;
    }

    private static boolean isLocalMaximum(final double[][] matrix, final int x, final int y,
                                          final int radius, final double value) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                if (matrix[y + dy][x + dx] > value) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void paintCross(final int[] pixels, final int width, final int height,
                                   final int cx, final int cy, final int radius) {
        for (int d = -radius; d <= radius; d++) {
            paintPixel(pixels, width, height, cx + d, cy, ImageUtils.toARGB(255, 255, 60, 60));
            paintPixel(pixels, width, height, cx, cy + d, ImageUtils.toARGB(255, 255, 60, 60));
        }
    }

    private static void paintPixel(final int[] pixels, final int width, final int height,
                                   final int x, final int y, final int color) {
        if (isNotInside(x, y, width, height)) {
            return;
        }
        pixels[(y * width) + x] = color;
    }

    private static boolean isNotInside(final int x, final int y, final int width, final int height) {
        return x < 0 || x >= width || y < 0 || y >= height;
    }
}

