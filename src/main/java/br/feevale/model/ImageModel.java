package br.feevale.model;

import br.feevale.core.ImageHistory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Modelo que encapsula a imagem e suas operações básicas.
 * Mantém referência à imagem original e à imagem transformada.
 */
public class ImageModel {
    
    private BufferedImage originalImage;
    private BufferedImage transformedImage;

    /**
     * Histórico de versões da imagem transformada (para voltar/avançar).
     * Mantém snapshots com capacidade limitada.
     */
    private final ImageHistory history = new ImageHistory();

    /**
     * Carrega uma imagem de um arquivo.
     * 
     * @param file Arquivo de imagem (PNG, JPG, BMP)
     * @throws IOException Se houver erro na leitura do arquivo
     */
    public void loadImage(final File file) throws IOException {
        final BufferedImage loadedImage = ImageIO.read(file);
        if (loadedImage == null) {
            throw new IOException("Formato de imagem não suportado ou arquivo inválido.");
        }

        this.originalImage = loadedImage;
        this.transformedImage = copyImage(originalImage);

        // Nova imagem => reinicia histórico no estado base
        history.initWith(this.transformedImage);
    }
    
    /**
     * Salva a imagem transformada em um arquivo.
     * 
     * @param file Arquivo de destino
     * @param format Formato da imagem (png, jpg, bmp)
     * @throws IOException Se houver erro na escrita do arquivo
     */
    public void saveImage(final File file, final String format) throws IOException {
        if (transformedImage != null) {
            ImageIO.write(transformedImage, format, file);
        }
    }
    
    /**
     * Cria uma cópia profunda de uma BufferedImage.
     * 
     * @param source Imagem de origem
     * @return Nova instância com os mesmos pixels
     */
    public static BufferedImage copyImage(final BufferedImage source) {
        if (source == null) return null;
        
        BufferedImage copy = new BufferedImage(
            source.getWidth(), 
            source.getHeight(), 
            source.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : source.getType()
        );
        
        // Copia pixels diretamente para melhor performance
        int[] pixels = source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth());
        copy.setRGB(0, 0, source.getWidth(), source.getHeight(), pixels, 0, source.getWidth());
        
        return copy;
    }
    
    /**
     * Aplica a imagem transformada atual como a nova base para transformações.
     * Útil para aplicar sequência de transformações.
     */
    public void applyTransformationAsOriginal() {
        if (transformedImage != null) {
            this.originalImage = copyImage(transformedImage);

            // Mudou a base de referência => reinicia histórico
            this.transformedImage = copyImage(this.originalImage);
            history.initWith(this.transformedImage);
        }
    }
    
    /**
     * Reseta a imagem transformada para a original.
     */
    public void resetToOriginal() {
        if (originalImage != null) {
            this.transformedImage = copyImage(originalImage);

            // Reset é um novo ponto de partida para as próximas transformações
            history.initWith(this.transformedImage);
        }
    }
    
    // Getters e Setters
    
    public BufferedImage getOriginalImage() {
        return originalImage;
    }
    
    public BufferedImage getTransformedImage() {
        return transformedImage;
    }
    
    public void setTransformedImage(final BufferedImage transformedImage) {
        this.transformedImage = transformedImage;
    }

    // ==================== Histórico (undo/redo) ====================

    /**
     * Registra a imagem transformada atual no histórico.
     * Deve ser chamado após uma transformação concluir.
     */
    public void commitTransformedVersion() {
        if (transformedImage != null) {
            history.push(transformedImage);
        }
    }

    public boolean canUndo() {
        return history.canUndo();
    }

    public boolean canRedo() {
        return history.canRedo();
    }

    /**
     * Volta para a versão anterior da imagem transformada.
     *
     * @return true se conseguiu voltar.
     */
    public boolean undo() {
        final BufferedImage img = history.undo();
        if (img == null) return false;
        this.transformedImage = img;
        return true;
    }

    /**
     * Avança para a próxima versão da imagem transformada.
     *
     * @return true se conseguiu avançar.
     */
    public boolean redo() {
        final BufferedImage img = history.redo();
        if (img == null) return false;
        this.transformedImage = img;
        return true;
    }


    public boolean hasImage() {
        return originalImage != null;
    }
    
    public int getWidth() {
        return originalImage != null ? originalImage.getWidth() : 0;
    }
    
    public int getHeight() {
        return originalImage != null ? originalImage.getHeight() : 0;
    }
}
