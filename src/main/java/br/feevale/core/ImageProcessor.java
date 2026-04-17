package br.feevale.core;

import br.feevale.model.ImageModel;

import java.awt.image.BufferedImage;

/**
 * Classe principal para processamento de imagens.
 * Atua como uma fachada que coordena as operações de processamento.
 */
public class ImageProcessor {

    private final ImageModel model;

    public ImageProcessor(final ImageModel model) {
        this.model = model;
    }

    /**
     * Verifica se há uma imagem carregada no modelo.
     */
    public boolean hasImage() {
        return model.hasImage();
    }

    /**
     * Obtém a imagem original.
     */
    public BufferedImage getOriginalImage() {
        return model.getOriginalImage();
    }

    /**
     * Obtém a imagem transformada.
     */
    public BufferedImage getTransformedImage() {
        return model.getTransformedImage();
    }

    /**
     * Registra a versão atual da imagem transformada no histórico.
     * Útil para operações que alteram a imagem diretamente.
     */
    private void commit(final BufferedImage image) {
        model.setTransformedImage(image);
        model.commitTransformedVersion();
    }

    // ==================== TRANSFORMAÇÕES GEOMÉTRICAS ====================

    /**
     * Aplica translação na imagem.
     */
    public void translate(int dx, int dy) {
        if (!hasImage()) return;
        final var result = GeometricTransformations.translate(model.getTransformedImage(), dx, dy);
        commit(result);
    }

    /**
     * Aplica rotação na imagem.
     */
    public void rotate(double angleDegrees) {
        if (!hasImage()) return;
        final var result = GeometricTransformations.rotate(model.getTransformedImage(), angleDegrees);
        commit(result);
    }

    /**
     * Aplica rotação mantendo as dimensões originais.
     */
    public void rotateKeepSize(double angleDegrees) {
        if (!hasImage()) return;
        final var result = GeometricTransformations.rotateKeepSize(model.getTransformedImage(), angleDegrees);
        commit(result);
    }

    /**
     * Aplica espelhamento horizontal.
     */
    public void mirrorHorizontal() {
        if (!hasImage()) return;
        final var result = GeometricTransformations.mirrorHorizontal(model.getTransformedImage());
        commit(result);
    }

    /**
     * Aplica espelhamento vertical.
     */
    public void mirrorVertical() {
        if (!hasImage()) return;
        final var result = GeometricTransformations.mirrorVertical(model.getTransformedImage());
        commit(result);
    }

    /**
     * Aumenta a imagem usando fatores de escala independentes em X e Y.
     */
    public void scaleUp(double scaleX, double scaleY) {
        if (!hasImage()) return;
        final var result = GeometricTransformations.scale(model.getTransformedImage(), scaleX, scaleY);
        commit(result);
    }

    /**
     * Diminui a imagem usando fatores de escala independentes em X e Y.
     */
    public void scaleDown(double scaleX, double scaleY) {
        if (!hasImage()) return;
        final var result = GeometricTransformations.scale(model.getTransformedImage(), 1 / scaleX, 1 / scaleY);
        commit(result);
    }

    // ==================== FILTROS ====================

    /**
     * Aplica brilho e contraste usando a fórmula: D(x,y) = C * f(x,y) + B.
     *
     * @param contrast   C (ganho). Ex.: 1.0 mantém.
     * @param brightness B (offset). Ex.: 0 mantém.
     */
    public void adjustBrightnessContrast(final double contrast, final int brightness) {
        if (!hasImage()) return;
        final var result = Filters.adjustBrightnessContrast(model.getTransformedImage(), contrast, brightness);
        commit(result);
    }

    /**
     * Converte para tons de cinza (grayscale) usando um tipo/fórmula específica.
     */
    public void grayscale(final Filters.GrayscaleType type) {
        if (!hasImage()) return;
        final var result = Filters.grayscale(model.getTransformedImage(), type);
        commit(result);
    }

    /**
     * Aplica negativo (inversão de cores).
     */
    public void negative() {
        if (!hasImage()) return;
        final var result = Filters.negative(model.getTransformedImage());
        commit(result);
    }

    /**
     * Passa baixa (suavização) via convolução.
     */
    public void lowPass(final Filters.BlurType type, final int kernelSize) {
        if (!hasImage()) return;
        final var result = Filters.lowPass(model.getTransformedImage(), type, kernelSize);
        commit(result);
    }

    /**
     * Passa alta (aguçamento) via Unsharp Mask.
     */
    public void highPass(final Filters.BlurType blurType, final int blurKernelSize, final double amount) {
        if (!hasImage()) return;
        final var result = Filters.highPass(model.getTransformedImage(), blurType, blurKernelSize, amount);
        commit(result);
    }

    /**
     * Aplica binarização (threshold) com base na luminância.
     */
    public void threshold(final int threshold) {
        if (!hasImage()) return;
        final var result = Filters.threshold(model.getTransformedImage(), threshold);
        commit(result);
    }

    // ==================== MORFOLOGIA MATEMATICA ====================

    public void dilate(final int kernelSize, final int iterations, final int threshold) {
        if (!hasImage()) return;
        final var result = MathematicalMorphology.dilate(model.getTransformedImage(), kernelSize, iterations, threshold);
        commit(result);
    }

    public void erode(final int kernelSize, final int iterations, final int threshold) {
        if (!hasImage()) return;
        final var result = MathematicalMorphology.erode(model.getTransformedImage(), kernelSize, iterations, threshold);
        commit(result);
    }

    public void opening(final int kernelSize, final int iterations, final int threshold) {
        if (!hasImage()) return;
        final var result = MathematicalMorphology.opening(model.getTransformedImage(), kernelSize, iterations, threshold);
        commit(result);
    }

    public void closing(final int kernelSize, final int iterations, final int threshold) {
        if (!hasImage()) return;
        final var result = MathematicalMorphology.closing(model.getTransformedImage(), kernelSize, iterations, threshold);
        commit(result);
    }

    public void thinning(final int threshold, final int maxIterations) {
        if (!hasImage()) return;
        final var result = MathematicalMorphology.thinning(model.getTransformedImage(), threshold, maxIterations);
        commit(result);
    }

    public void extractContour(final int kernelSize, final int threshold) {
        if (!hasImage()) return;
        final var result = MathematicalMorphology.extractContour(model.getTransformedImage(), kernelSize, threshold);
        commit(result);
    }

    // ==================== EXTRAÇÃO DE CARACTERÍSTICAS ====================

    public void detectEdgesSobel(final int threshold) {
        if (!hasImage()) return;
        final var result = FeatureExtraction.detectEdgesSobel(model.getTransformedImage(), threshold);
        commit(result);
    }

    public void detectEdgesCanny(final int lowThreshold, final int highThreshold) {
        if (!hasImage()) return;
        final var result = FeatureExtraction.detectEdgesCanny(model.getTransformedImage(), lowThreshold, highThreshold);
        commit(result);
    }

    public int countObjects(final int threshold, final boolean useEightConnectivity, final int minArea) {
        if (!hasImage()) return 0;
        final var connectivity = useEightConnectivity
                ? FeatureExtraction.Connectivity.EIGHT
                : FeatureExtraction.Connectivity.FOUR;
        return FeatureExtraction.countObjects(model.getTransformedImage(), threshold, connectivity, minArea);
    }

    public int[] intensityHistogram() {
        if (!hasImage()) return new int[256];
        return FeatureExtraction.intensityHistogram(model.getTransformedImage());
    }

    public void renderIntensityHistogram(final int width, final int height) {
        if (!hasImage()) return;
        final int[] histogram = FeatureExtraction.intensityHistogram(model.getTransformedImage());
        final var result = FeatureExtraction.renderIntensityHistogram(histogram, width, height);
        commit(result);
    }

    public int detectCorners(final double thresholdRatio, final int suppressionRadius) {
        if (!hasImage()) return 0;
        final var result = FeatureExtraction.detectCornersHarris(
                model.getTransformedImage(), thresholdRatio, suppressionRadius
        );
        commit(result.image());
        return result.cornerCount();
    }

    /**
     * Aplica a transformação atual como a nova imagem original.
     */
    public void applyTransformationAsOriginal() {
        model.applyTransformationAsOriginal();
    }

    /**
     * Reseta para a imagem original.
     */
    public void resetToOriginal() {
        model.resetToOriginal();
    }

    public boolean canUndo() {
        return model.canUndo();
    }

    public boolean canRedo() {
        return model.canRedo();
    }

    public boolean undo() {
        return model.undo();
    }

    public boolean redo() {
        return model.redo();
    }

}
