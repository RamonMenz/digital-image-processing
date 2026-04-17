package br.feevale.core;

import java.awt.image.BufferedImage;
import br.feevale.utils.ImageUtils;

/**
 * Classe responsável pelas transformações geométricas em imagens.
 * Todas as transformações são implementadas manualmente, manipulando
 * diretamente os pixels da imagem.
 *
 * <p>A classe implementa transformacoes espaciais por meio de matrizes homogeneas 3x3,
 * usando mapeamento inverso (pixel de destino para coordenada de origem).</p>
 *
 * <p>Embora os pixels sejam armazenados como inteiros (ARGB), as coordenadas transformadas geralmente
 * tornam-se fracionárias (rotação, escala não-inteira, inversa de matriz, etc). Por isso os cálculos
 * geométricos usam {@code double}, e somente na amostragem convertemos para coordenadas inteiras.</p>
 *
 * <p>Quando a coordenada de origem calculada fica fora da imagem, o pixel de destino
 * recebe {@code ARGB 0x00000000} (transparente).</p>
 */
public class GeometricTransformations {

	private GeometricTransformations() {
		throw new IllegalStateException("Utility class.");
	}

	/**
	 * Estrategias de amostragem usadas durante o remapeamento.
	 */
	private enum SamplingMode {
		/** Vizinho mais proximo, mais rapido e com serrilhado mais evidente. */
		NEAREST,
		/** Interpolacao bilinear, mais suave para escala e rotacoes. */
		BILINEAR
	}

	/** Cor transparente usada para preenchimento fora dos limites da origem. */
	private static final int TRANSPARENT = 0x00000000;

	/**
	 * Aplica uma translação (deslocamento) na imagem.
	 * Pixels que saem dos limites sao descartados, e novos pixels sao preenchidos com transparente.
	 *
	 * @param image imagem de entrada
	 * @param dx deslocamento horizontal (positivo = direita, negativo = esquerda)
	 * @param dy deslocamento vertical (positivo = baixo, negativo = cima)
	 * @return imagem transladada com as mesmas dimensoes da entrada
	 */
	public static BufferedImage translate(final BufferedImage image, final int dx, final int dy) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		final double[][] transform = translationMatrix(dx, dy);
		return applyTransform(image, transform, width, height, SamplingMode.NEAREST);
	}

	/**
	 * Rotaciona a imagem em torno do centro da imagem original.
	 *
	 * <p>O canvas de saida e expandido para acomodar toda a imagem rotacionada.
	 * A amostragem e feita por vizinho mais proximo.</p>
	 *
	 * @param image imagem de entrada
	 * @param angleDegrees angulo em graus (positivo = anti-horario no sistema cartesiano da matriz)
	 * @return imagem rotacionada com dimensoes recalculadas
	 */
	public static BufferedImage rotate(final BufferedImage image, final double angleDegrees) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		final double centerX = (width - 1) / 2.0;
		final double centerY = (height - 1) / 2.0;

		final double[][] rotation = rotationAroundPointMatrix(angleDegrees, centerX, centerY);
		final Bounds bounds = computeTransformedBounds(width, height, rotation);
		final double[][] withOffset = multiply(
				translationMatrix(-bounds.minX, -bounds.minY),
				rotation);
		final SamplingMode mode = isRightAngleRotation(angleDegrees) ? SamplingMode.NEAREST : SamplingMode.BILINEAR;

		return applyTransform(image, withOffset, bounds.width, bounds.height, mode);
	}

	/**
	 * Rotaciona a imagem mantendo as dimensoes originais.
	 *
	 * @param image imagem de entrada
	 * @param angleDegrees angulo em graus
	 * @return imagem rotacionada no mesmo tamanho da entrada
	 */
	public static BufferedImage rotateKeepSize(final BufferedImage image, final double angleDegrees) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		final double centerX = (width - 1) / 2.0;
		final double centerY = (height - 1) / 2.0;

		final double[][] rotation = rotationAroundPointMatrix(angleDegrees, centerX, centerY);
		final SamplingMode mode = isRightAngleRotation(angleDegrees) ? SamplingMode.NEAREST : SamplingMode.BILINEAR;
		return applyTransform(image, rotation, width, height, mode);
	}

	/**
	 * Espelha a imagem horizontalmente (flip horizontal).
	 *
	 * @param image imagem de entrada
	 * @return imagem espelhada no eixo vertical (esquerda/direita)
	 */
	public static BufferedImage mirrorHorizontal(final BufferedImage image) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		final double[][] transform = {
				{-1.0, 0.0, width - 1.0},
				{0.0, 1.0, 0.0},
				{0.0, 0.0, 1.0}
		};
		return applyTransform(image, transform, width, height, SamplingMode.NEAREST);
	}

	/**
	 * Espelha a imagem verticalmente (flip vertical).
	 *
	 * @param image imagem de entrada
	 * @return imagem espelhada no eixo horizontal (cima/baixo)
	 */
	public static BufferedImage mirrorVertical(final BufferedImage image) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		final double[][] transform = {
				{1.0, 0.0, 0.0},
				{0.0, -1.0, height - 1.0},
				{0.0, 0.0, 1.0}
		};
		return applyTransform(image, transform, width, height, SamplingMode.NEAREST);
	}

	/**
	 * Escala a imagem com fatores independentes em X e Y.
	 *
	 * <p>Valores maiores que 1 aumentam e valores entre 0 e 1 diminuem o eixo correspondente.</p>
	 *
	 * @param image imagem de entrada
	 * @param factorX fator horizontal (> 0)
	 * @param factorY fator vertical (> 0)
	 * @return imagem escalada; se algum fator for invalido, retorna copia da original
	 */
	public static BufferedImage scale(
			final BufferedImage image,
			final double factorX, final double factorY
	) {
		if (factorX <= 0 || factorY <= 0) {
			return ImageUtils.copyImage(image);
		}

		final int srcWidth = image.getWidth();
		final int srcHeight = image.getHeight();
		final int dstWidth = Math.max(1, (int) (srcWidth * factorX));
		final int dstHeight = Math.max(1, (int) (srcHeight * factorY));

		final double[][] transform = scalingMatrix(factorX, factorY);
		return applyTransform(image, transform, dstWidth, dstHeight, SamplingMode.BILINEAR);
	}

	/**
	 * Aplica uma transformacao afim 3x3 em uma imagem usando mapeamento inverso.
	 *
	 * @param image imagem de entrada
	 * @param forwardTransform matriz afim direta (origem -> destino)
	 * @param dstWidth largura da imagem de saida
	 * @param dstHeight altura da imagem de saida
	 * @param samplingMode estrategia de amostragem
	 * @return imagem transformada
	 * @throws IllegalArgumentException se a matriz nao for invertivel
	 */
	private static BufferedImage applyTransform(
			final BufferedImage image,
			final double[][] forwardTransform,
			final int dstWidth, final int dstHeight,
			final SamplingMode samplingMode
	) {
		final BufferedImage result = ImageUtils.createImage(dstWidth, dstHeight);
		final int srcWidth = image.getWidth();
		final int srcHeight = image.getHeight();
		final int[] srcPixels = ImageUtils.getPixels(image);
		final int[] dstPixels = new int[dstWidth * dstHeight];

		final double[][] inverse = invertMatrix(forwardTransform);

		for (int y = 0; y < dstHeight; y++) {
			for (int x = 0; x < dstWidth; x++) {
				final double srcX = inverse[0][0] * x + inverse[0][1] * y + inverse[0][2];
				final double srcY = inverse[1][0] * x + inverse[1][1] * y + inverse[1][2];

				final int sampled = samplingMode == SamplingMode.BILINEAR
						? sampleBilinear(srcPixels, srcWidth, srcHeight, srcX, srcY)
						: sampleNearest(srcPixels, srcWidth, srcHeight, srcX, srcY);

				dstPixels[y * dstWidth + x] = sampled;
			}
		}

		ImageUtils.setPixels(result, dstPixels);
		return result;
	}

	/**
	 * Amostra um pixel por vizinho mais proximo.
	 *
	 * @return pixel ARGB da origem ou {@link #TRANSPARENT} se estiver fora dos limites
	 */
	private static int sampleNearest(
			final int[] srcPixels,
			final int srcWidth, final int srcHeight,
			final double srcX, final double srcY
	) {
		final int sx = (int) Math.round(srcX);
		final int sy = (int) Math.round(srcY);

		if (!ImageUtils.isValidCoordinate(sx, sy, srcWidth, srcHeight)) {
			return TRANSPARENT;
		}

		return srcPixels[sy * srcWidth + sx];
	}

	/**
	 * Amostra um pixel por interpolacao bilinear.
	 *
	 * @return pixel ARGB interpolado ou {@link #TRANSPARENT} se estiver fora dos limites
	 */
	private static int sampleBilinear(
			final int[] srcPixels,
			final int srcWidth, final int srcHeight,
			final double srcX, final double srcY
	) {
		final double epsilon = 1e-9;
		if (srcX < -epsilon || srcY < -epsilon || srcX > (srcWidth - 1.0) + epsilon || srcY > (srcHeight - 1.0) + epsilon) {
			return TRANSPARENT;
		}

		final double clampedX = Math.clamp(srcX, 0.0, srcWidth - 1.0);
		final double clampedY = Math.clamp(srcY, 0.0, srcHeight - 1.0);

		final int x0 = (int) Math.floor(clampedX);
		final int y0 = (int) Math.floor(clampedY);
		final int x1 = Math.min(x0 + 1, srcWidth - 1);
		final int y1 = Math.min(y0 + 1, srcHeight - 1);

		final double wx = clampedX - x0;
		final double wy = clampedY - y0;

		final int p00 = srcPixels[y0 * srcWidth + x0];
		final int p01 = srcPixels[y0 * srcWidth + x1];
		final int p10 = srcPixels[y1 * srcWidth + x0];
		final int p11 = srcPixels[y1 * srcWidth + x1];

		final int a = bilinearInterpolate(
				ImageUtils.getAlpha(p00), ImageUtils.getAlpha(p01),
				ImageUtils.getAlpha(p10), ImageUtils.getAlpha(p11), wx, wy);

		final int r = bilinearInterpolate(
				ImageUtils.getRed(p00), ImageUtils.getRed(p01),
				ImageUtils.getRed(p10), ImageUtils.getRed(p11), wx, wy);

		final int g = bilinearInterpolate(
				ImageUtils.getGreen(p00), ImageUtils.getGreen(p01),
				ImageUtils.getGreen(p10), ImageUtils.getGreen(p11), wx, wy);

		final int b = bilinearInterpolate(
				ImageUtils.getBlue(p00), ImageUtils.getBlue(p01),
				ImageUtils.getBlue(p10), ImageUtils.getBlue(p11), wx, wy);

		return ImageUtils.toARGB(a, r, g, b);
	}

	/**
	 * Cria uma matriz de translacao 2D em coordenadas homogeneas.
	 */
	private static double[][] translationMatrix(final double tx, final double ty) {
		return new double[][]{
				{1.0, 0.0, tx},
				{0.0, 1.0, ty},
				{0.0, 0.0, 1.0}
		};
	}

	/**
	 * Cria uma matriz de escala 2D em coordenadas homogeneas.
	 */
	private static double[][] scalingMatrix(final double sx, final double sy) {
		return new double[][]{
				{sx, 0.0, 0.0},
				{0.0, sy, 0.0},
				{0.0, 0.0, 1.0}
		};
	}

	/**
	 * Cria uma matriz de rotacao 2D em coordenadas homogeneas.
	 */
	private static double[][] rotationMatrix(final double angleDegrees) {
		final double radians = Math.toRadians(angleDegrees);
		final double cos = Math.cos(radians);
		final double sin = Math.sin(radians);

		return new double[][]{
				{cos, -sin, 0.0},
				{sin, cos, 0.0},
				{0.0, 0.0, 1.0}
		};
	}

	/**
	 * Interpola bilinearmente quatro amostras escalares.
	 *
	 * @param v00 valor no canto superior esquerdo
	 * @param v01 valor no canto superior direito
	 * @param v10 valor no canto inferior esquerdo
	 * @param v11 valor no canto inferior direito
	 * @param wx peso horizontal no intervalo [0, 1]
	 * @param wy peso vertical no intervalo [0, 1]
	 * @return valor interpolado convertido para inteiro
	 */
	private static int bilinearInterpolate(
			final int v00, final int v01,
			final int v10, final int v11,
			final double wx, final double wy
	) {
		final double top = v00 * (1 - wx) + v01 * wx;
		final double bottom = v10 * (1 - wx) + v11 * wx;
		return (int) (top * (1 - wy) + bottom * wy);
	}

	/**
	 * Monta a matriz de rotacao em torno de um ponto arbitrario.
	 *
	 * @param angleDegrees angulo em graus
	 * @param px coordenada X do pivô
	 * @param py coordenada Y do pivô
	 * @return matriz 3x3 de rotacao em torno do ponto informado
	 */
	private static double[][] rotationAroundPointMatrix(final double angleDegrees, final double px, final double py) {
		return multiply(
				translationMatrix(px, py),
				multiply(
						rotationMatrix(angleDegrees),
						translationMatrix(-px, -py)));
	}

	/**
	 * Multiplica duas matrizes 3x3 ({@code out = a x b}).
	 */
	private static double[][] multiply(final double[][] a, final double[][] b) {
		final double[][] out = new double[3][3];

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				out[i][j] = 0.0;
				for (int k = 0; k < 3; k++) {
					out[i][j] += a[i][k] * b[k][j];
				}
			}
		}

		return out;
	}

	/**
	 * Inverte uma matriz afim 3x3 do tipo:
	 * <pre>
	 * [ a b c ]
	 * [ d e f ]
	 * [ 0 0 1 ]
	 * </pre>
	 *
	 * @param matrix matriz afim 3x3
	 * @return matriz inversa
	 * @throws IllegalArgumentException se o determinante for aproximadamente zero
	 */
	private static double[][] invertMatrix(final double[][] matrix) {
		final double a = matrix[0][0];
		final double b = matrix[0][1];
		final double c = matrix[0][2];
		final double d = matrix[1][0];
		final double e = matrix[1][1];
		final double f = matrix[1][2];

		final double det = a * e - b * d;
		if (Math.abs(det) < 1e-12) {
			throw new IllegalArgumentException("Transformacao nao invertivel");
		}

		final double invDet = 1.0 / det;
		return new double[][]{
				{e * invDet, -b * invDet, (b * f - e * c) * invDet},
				{-d * invDet, a * invDet, (d * c - a * f) * invDet},
				{0.0, 0.0, 1.0}
		};
	}

	/**
	 * Calcula o retangulo delimitador (bounding box) dos quatro cantos apos a transformacao.
	 *
	 * @param width largura original
	 * @param height altura original
	 * @param transform matriz aplicada aos pontos de entrada
	 * @return limites minimos e dimensoes inteiras para o canvas de saida
	 */
	private static Bounds computeTransformedBounds(final int width, final int height, final double[][] transform) {
		final double[] p0 = transformPoint(transform, 0.0, 0.0);
		final double[] p1 = transformPoint(transform, width - 1.0, 0.0);
		final double[] p2 = transformPoint(transform, 0.0, height - 1.0);
		final double[] p3 = transformPoint(transform, width - 1.0, height - 1.0);

		final double minX = Math.min(Math.min(p0[0], p1[0]), Math.min(p2[0], p3[0]));
		final double maxX = Math.max(Math.max(p0[0], p1[0]), Math.max(p2[0], p3[0]));
		final double minY = Math.min(Math.min(p0[1], p1[1]), Math.min(p2[1], p3[1]));
		final double maxY = Math.max(Math.max(p0[1], p1[1]), Math.max(p2[1], p3[1]));

		final int outWidth = (int) Math.ceil(maxX - minX + 1.0);
		final int outHeight = (int) Math.ceil(maxY - minY + 1.0);

		return new Bounds(minX, minY, Math.max(1, outWidth), Math.max(1, outHeight));
	}

	/**
	 * Aplica uma matriz afim 3x3 a um ponto 2D.
	 *
	 * @param matrix matriz da transformacao
	 * @param x coordenada X de entrada
	 * @param y coordenada Y de entrada
	 * @return vetor com {@code [x', y']}
	 */
	private static double[] transformPoint(final double[][] matrix, final double x, final double y) {
		return new double[]{
				matrix[0][0] * x + matrix[0][1] * y + matrix[0][2],
				matrix[1][0] * x + matrix[1][1] * y + matrix[1][2]
		};
	}

	private static boolean isRightAngleRotation(final double angleDegrees) {
		final double normalized = ((angleDegrees % 360.0) + 360.0) % 360.0;
		final double distanceToQuarterTurn = normalized % 90.0;
		final double epsilon = 1e-9;
		return distanceToQuarterTurn < epsilon || (90.0 - distanceToQuarterTurn) < epsilon;
	}

	/**
	 * Estrutura imutavel com limites minimos e dimensoes do bounding box transformado.
	 */
	private record Bounds(double minX, double minY, int width, int height) {
	}

}
