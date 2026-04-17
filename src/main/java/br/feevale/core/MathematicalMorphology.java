package br.feevale.core;

import br.feevale.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Operacoes de morfologia matematica sobre imagens binarias.
 *
 * <p>As operacoes convertem a imagem de entrada para binaria usando limiar de luminancia.
 * Objetos (foreground) sao representados por pixels brancos.</p>
 */
public final class MathematicalMorphology {

    private MathematicalMorphology() {
        throw new IllegalStateException("Utility class.");
    }

    /**
     * Aplica dilatacao binaria com elemento estruturante quadrado.
     *
     * <p>Pixels de foreground sao expandidos conforme o kernel e numero de iteracoes.
     * A imagem e binarizada internamente usando o limiar informado.</p>
     *
     * @param image imagem de entrada
     * @param kernelSize tamanho do kernel (par sera normalizado para impar)
     * @param iterations numero de iteracoes (minimo 1)
     * @param threshold limiar de binarizacao em [0,255]
     * @return nova imagem binaria resultante da dilatacao
     */
    public static BufferedImage dilate(
            final BufferedImage image, final int kernelSize,
            final int iterations, final int threshold
    ) {
        final boolean[][] kernel = squareKernel(kernelSize);
        final int safeIterations = Math.max(1, iterations);
        boolean[][] current = toBinaryMatrix(image, threshold);

        for (int i = 0; i < safeIterations; i++) {
            current = dilateBinary(current, kernel);
        }

        return fromBinaryMatrix(current, image);
    }

    /**
     * Aplica erosao binaria com elemento estruturante quadrado.
     *
     * <p>Reduz regioes de foreground removendo pixels de borda que nao comportam
     * completamente o kernel.</p>
     *
     * @param image imagem de entrada
     * @param kernelSize tamanho do kernel (par sera normalizado para impar)
     * @param iterations numero de iteracoes (minimo 1)
     * @param threshold limiar de binarizacao em [0,255]
     * @return nova imagem binaria resultante da erosao
     */
    public static BufferedImage erode(final BufferedImage image, final int kernelSize, final int iterations,
                                      final int threshold) {
        final boolean[][] kernel = squareKernel(kernelSize);
        final int safeIterations = Math.max(1, iterations);
        boolean[][] current = toBinaryMatrix(image, threshold);

        for (int i = 0; i < safeIterations; i++) {
            current = erodeBinary(current, kernel);
        }

        return fromBinaryMatrix(current, image);
    }

    /**
     * Aplica abertura morfologica (erosao seguida de dilatacao).
     *
     * <p>Util para remover ruido pequeno preservando melhor a forma global
     * dos objetos.</p>
     *
     * @param image imagem de entrada
     * @param kernelSize tamanho do kernel (par sera normalizado para impar)
     * @param iterations numero de iteracoes de cada etapa (minimo 1)
     * @param threshold limiar de binarizacao em [0,255]
     * @return nova imagem binaria apos abertura
     */
    public static BufferedImage opening(final BufferedImage image, final int kernelSize, final int iterations,
                                        final int threshold) {
        final boolean[][] kernel = squareKernel(kernelSize);
        final int safeIterations = Math.max(1, iterations);
        boolean[][] current = toBinaryMatrix(image, threshold);

        for (int i = 0; i < safeIterations; i++) {
            current = erodeBinary(current, kernel);
        }
        for (int i = 0; i < safeIterations; i++) {
            current = dilateBinary(current, kernel);
        }

        return fromBinaryMatrix(current, image);
    }

    /**
     * Aplica fechamento morfologico (dilatacao seguida de erosao).
     *
     * <p>Util para preencher pequenos buracos e conectar pequenas quebras
     * em objetos do foreground.</p>
     *
     * @param image imagem de entrada
     * @param kernelSize tamanho do kernel (par sera normalizado para impar)
     * @param iterations numero de iteracoes de cada etapa (minimo 1)
     * @param threshold limiar de binarizacao em [0,255]
     * @return nova imagem binaria apos fechamento
     */
    public static BufferedImage closing(final BufferedImage image, final int kernelSize, final int iterations,
                                        final int threshold) {
        final boolean[][] kernel = squareKernel(kernelSize);
        final int safeIterations = Math.max(1, iterations);
        boolean[][] current = toBinaryMatrix(image, threshold);

        for (int i = 0; i < safeIterations; i++) {
            current = dilateBinary(current, kernel);
        }
        for (int i = 0; i < safeIterations; i++) {
            current = erodeBinary(current, kernel);
        }

        return fromBinaryMatrix(current, image);
    }

    /**
     * Aplica afinamento (skeletonization) pelo algoritmo de Zhang-Suen.
     *
     * <p>A operacao remove iterativamente pixels de borda sem quebrar conectividade
     * principal do objeto. Quando {@code maxIterations == 0}, executa ate convergir.</p>
     *
     * @param image imagem de entrada
     * @param threshold limiar de binarizacao em [0,255]
     * @param maxIterations limite de iteracoes (0 para convergencia)
     * @return nova imagem binaria afinada
     */
    public static BufferedImage thinning(final BufferedImage image, final int threshold, final int maxIterations) {
        boolean[][] current = toBinaryMatrix(image, threshold);
        final int height = current.length;
        final int width = current[0].length;
        final int safeMaxIterations = Math.max(0, maxIterations);

        boolean changed;
        int iteration = 0;
        do {
            changed = false;

            final List<int[]> toRemoveStep1 = new ArrayList<>();
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    if (!current[y][x]) {
                        continue;
                    }

                    final boolean[] n = neighbors(current, x, y);
                    final int transitions = transitionsZeroToOne(n);
                    final int blackNeighbors = neighborCount(n);

                    if (blackNeighbors >= 2 && blackNeighbors <= 6
                            && transitions == 1
                            && !(n[0] && n[2] && n[4])
                            && !(n[2] && n[4] && n[6])) {
                        toRemoveStep1.add(new int[]{x, y});
                    }
                }
            }
            if (!toRemoveStep1.isEmpty()) {
                changed = true;
                for (int[] point : toRemoveStep1) {
                    current[point[1]][point[0]] = false;
                }
            }

            final List<int[]> toRemoveStep2 = new ArrayList<>();
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    if (!current[y][x]) {
                        continue;
                    }

                    final boolean[] n = neighbors(current, x, y);
                    final int transitions = transitionsZeroToOne(n);
                    final int blackNeighbors = neighborCount(n);

                    if (blackNeighbors >= 2 && blackNeighbors <= 6
                            && transitions == 1
                            && !(n[0] && n[2] && n[6])
                            && !(n[0] && n[4] && n[6])) {
                        toRemoveStep2.add(new int[]{x, y});
                    }
                }
            }
            if (!toRemoveStep2.isEmpty()) {
                changed = true;
                for (int[] point : toRemoveStep2) {
                    current[point[1]][point[0]] = false;
                }
            }

            iteration++;
        } while (changed && (safeMaxIterations == 0 || iteration < safeMaxIterations));

        return fromBinaryMatrix(current, image);
    }

    /**
     * Extrai contorno morfologico pela diferenca entre imagem binaria original e erosao.
     *
     * @param image imagem de entrada
     * @param kernelSize tamanho do kernel da erosao (par sera normalizado para impar)
     * @param threshold limiar de binarizacao em [0,255]
     * @return imagem contendo somente pixels de contorno
     */
    public static BufferedImage extractContour(final BufferedImage image, final int kernelSize, final int threshold) {
        final boolean[][] source = toBinaryMatrix(image, threshold);
        final boolean[][] eroded = erodeBinary(source, squareKernel(kernelSize));
        final int height = source.length;
        final int width = source[0].length;
        final boolean[][] contour = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                contour[y][x] = source[y][x] && !eroded[y][x];
            }
        }

        return fromBinaryMatrix(contour, image);
    }

    /**
     * Cria um kernel quadrado totalmente ativo (todos os elementos verdadeiros).
     *
     * @param kernelSize tamanho desejado
     * @return matriz booleana do kernel
     */
    private static boolean[][] squareKernel(final int kernelSize) {
        final int size = normalizeOddKernelSize(kernelSize);
        final boolean[][] kernel = new boolean[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                kernel[y][x] = true;
            }
        }
        return kernel;
    }

    /**
     * Garante tamanho de kernel valido e impar.
     *
     * @param kernelSize tamanho solicitado
     * @return tamanho normalizado (>= 1 e impar)
     */
    private static int normalizeOddKernelSize(final int kernelSize) {
        int size = Math.max(1, kernelSize);
        if (size % 2 == 0) {
            size += 1;
        }
        return size;
    }

    /**
     * Converte imagem ARGB para matriz binaria (foreground/background).
     *
     * <p>Foreground: pixel com alpha maior que zero e intensidade >= threshold.</p>
     *
     * @param image imagem de entrada
     * @param threshold limiar de binarizacao em [0,255]
     * @return matriz booleana [altura][largura]
     */
    private static boolean[][] toBinaryMatrix(final BufferedImage image, final int threshold) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int thresholdValue = ImageUtils.clamp(threshold);
        final int[] pixels = ImageUtils.getPixels(image);
        final boolean[][] binary = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int argb = pixels[(y * width) + x];
                final int a = ImageUtils.getAlpha(argb);
                final int r = ImageUtils.getRed(argb);
                final int g = ImageUtils.getGreen(argb);
                final int b = ImageUtils.getBlue(argb);
                final int intensity = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
                binary[y][x] = a > 0 && intensity >= thresholdValue;
            }
        }

        return binary;
    }

    /**
     * Converte matriz binaria para imagem ARGB preservando alpha da referencia.
     *
     * @param binary matriz binaria [altura][largura]
     * @param reference imagem usada para herdar canal alpha
     * @return imagem binaria (preto/branco)
     */
    private static BufferedImage fromBinaryMatrix(final boolean[][] binary, final BufferedImage reference) {
        final int height = binary.length;
        final int width = binary[0].length;
        final int[] referencePixels = ImageUtils.getPixels(reference);
        final int[] outPixels = new int[referencePixels.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int index = (y * width) + x;
                final int alpha = ImageUtils.getAlpha(referencePixels[index]);
                final int value = binary[y][x] ? 255 : 0;
                outPixels[index] = ImageUtils.toARGB(alpha, value, value, value);
            }
        }

        final BufferedImage out = ImageUtils.createImage(width, height);
        ImageUtils.setPixels(out, outPixels);
        return out;
    }

    /**
     * Executa dilatacao sobre matriz binaria.
     *
     * @param source matriz de origem
     * @param kernel elemento estruturante
     * @return matriz dilatada
     */
    private static boolean[][] dilateBinary(final boolean[][] source, final boolean[][] kernel) {
        final int height = source.length;
        final int width = source[0].length;
        final int kHeight = kernel.length;
        final int kWidth = kernel[0].length;
        final int cy = kHeight / 2;
        final int cx = kWidth / 2;
        final boolean[][] out = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean hit = false;

                for (int ky = 0; ky < kHeight && !hit; ky++) {
                    for (int kx = 0; kx < kWidth; kx++) {
                        if (!kernel[ky][kx]) {
                            continue;
                        }
                        final int sx = x + (kx - cx);
                        final int sy = y + (ky - cy);

                        if (isInside(sx, sy, width, height) && source[sy][sx]) {
                            hit = true;
                            break;
                        }
                    }
                }

                out[y][x] = hit;
            }
        }

        return out;
    }

    /**
     * Executa erosao sobre matriz binaria.
     *
     * @param source matriz de origem
     * @param kernel elemento estruturante
     * @return matriz erodida
     */
    private static boolean[][] erodeBinary(final boolean[][] source, final boolean[][] kernel) {
        final int height = source.length;
        final int width = source[0].length;
        final int kHeight = kernel.length;
        final int kWidth = kernel[0].length;
        final int cy = kHeight / 2;
        final int cx = kWidth / 2;
        final boolean[][] out = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean keep = true;

                for (int ky = 0; ky < kHeight && keep; ky++) {
                    for (int kx = 0; kx < kWidth; kx++) {
                        if (!kernel[ky][kx]) {
                            continue;
                        }
                        final int sx = x + (kx - cx);
                        final int sy = y + (ky - cy);

                        if (!isInside(sx, sy, width, height) || !source[sy][sx]) {
                            keep = false;
                            break;
                        }
                    }
                }

                out[y][x] = keep;
            }
        }

        return out;
    }

    /**
     * Verifica se coordenada pertence ao dominio da imagem/matriz.
     *
     * @param x coordenada horizontal
     * @param y coordenada vertical
     * @param width largura limite
     * @param height altura limite
     * @return {@code true} se estiver dentro dos limites
     */
    private static boolean isInside(final int x, final int y, final int width, final int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Retorna vizinhanca p2..p9 na ordem de Zhang-Suen:
     * p2(N), p3(NE), p4(E), p5(SE), p6(S), p7(SW), p8(W), p9(NW).
     *
     * @param matrix matriz binaria
     * @param x coordenada horizontal do pixel central
     * @param y coordenada vertical do pixel central
     * @return vetor com 8 vizinhos na ordem circular esperada pelo algoritmo
     */
    private static boolean[] neighbors(final boolean[][] matrix, final int x, final int y) {
        return new boolean[]{
                matrix[y - 1][x],
                matrix[y - 1][x + 1],
                matrix[y][x + 1],
                matrix[y + 1][x + 1],
                matrix[y + 1][x],
                matrix[y + 1][x - 1],
                matrix[y][x - 1],
                matrix[y - 1][x - 1]
        };
    }

    /**
     * Conta transicoes 0->1 na sequencia circular de vizinhos.
     *
     * @param neighbors vetor de 8 vizinhos na ordem Zhang-Suen
     * @return quantidade de transicoes de falso para verdadeiro
     */
    private static int transitionsZeroToOne(final boolean[] neighbors) {
        int transitions = 0;
        for (int i = 0; i < neighbors.length; i++) {
            final boolean current = neighbors[i];
            final boolean next = neighbors[(i + 1) % neighbors.length];
            if (!current && next) {
                transitions++;
            }
        }
        return transitions;
    }

    /**
     * Conta quantos vizinhos estao ativos (foreground).
     *
     * @param neighbors vetor de 8 vizinhos
     * @return quantidade de vizinhos verdadeiros
     */
    private static int neighborCount(final boolean[] neighbors) {
        int count = 0;
        for (boolean neighbor : neighbors) {
            if (neighbor) {
                count++;
            }
        }
        return count;
    }

}
