package br.feevale.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Painel personalizado para exibição de imagens.
 * Suporta zoom, ajuste automático ao tamanho do painel e
 * exibição centralizada da imagem.
 */
public class ImagePanel extends JPanel {

    private BufferedImage image;
    private final String label;
    private boolean fitToPanel = true;
    private double zoomFactor = 1.0;

    public ImagePanel(final String label) {
        this.label = label;
        setBackground(new Color(45, 45, 45));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 70), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    /**
     * Define a imagem a ser exibida.
     */
    public void setImage(final BufferedImage image) {
        this.image = image;
        repaint();
    }

    /**
     * Obtém a imagem atual.
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Define se a imagem deve ser ajustada ao tamanho do painel.
     */
    public void setFitToPanel(final boolean fitToPanel) {
        this.fitToPanel = fitToPanel;
        repaint();
    }

    /**
     * Define o fator de zoom.
     */
    public void setZoomFactor(final double zoomFactor) {
        this.zoomFactor = Math.clamp(zoomFactor, 0.1, 10.0);
        repaint();
    }

    /**
     * Obtém o fator de zoom atual.
     */
    public double getZoomFactor() {
        return zoomFactor;
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        final var g2d = (Graphics2D) g.create();

        // Configurações de renderização para melhor qualidade
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

        // Área disponível para desenho (considera padding)
        final var availableWidth = getWidth() - 10;
        final var availableHeight = getHeight() - 30; // Reserva espaço para o label

        // Desenha o label na parte superior
        g2d.setColor(new Color(200, 200, 200));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        var fm = g2d.getFontMetrics();
        final var labelWidth = fm.stringWidth(label);
        g2d.drawString(label, (getWidth() - labelWidth) / 2, 20);

        if (image != null) {
            final var imgWidth = image.getWidth();
            final var imgHeight = image.getHeight();

            int drawWidth, drawHeight;

            if (fitToPanel) {
                // Calcula a escala para ajustar ao painel mantendo proporções
                final var scaleX = (double) availableWidth / imgWidth;
                final var scaleY = (double) availableHeight / imgHeight;
                final var scale = Math.min(scaleX, scaleY);

                drawWidth = (int) (imgWidth * scale);
                drawHeight = (int) (imgHeight * scale);
            } else {
                // Usa o fator de zoom
                drawWidth = (int) (imgWidth * zoomFactor);
                drawHeight = (int) (imgHeight * zoomFactor);
            }

            // Centraliza a imagem
            final var x = (getWidth() - drawWidth) / 2;
            final var y = 25 + (availableHeight - drawHeight) / 2;

            // Desenha a imagem
            g2d.drawImage(image, x, y, drawWidth, drawHeight, null);

            // Desenha informações da imagem
            final var info = String.format("%dx%d", imgWidth, imgHeight);
            g2d.setColor(new Color(150, 150, 150));
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
            fm = g2d.getFontMetrics();
            final var infoWidth = fm.stringWidth(info);
            g2d.drawString(info, getWidth() - infoWidth - 10, getHeight() - 5);
        } else {
            // Desenha mensagem quando não há imagem
            g2d.setColor(new Color(100, 100, 100));
            g2d.setFont(new Font("SansSerif", Font.ITALIC, 14));
            String msg = "Nenhuma imagem carregada";
            final var fmMsg = g2d.getFontMetrics();
            final var msgWidth = fmMsg.stringWidth(msg);
            g2d.drawString(msg, (getWidth() - msgWidth) / 2, getHeight() / 2);
        }

        g2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        if (image != null) {
            return new Dimension(
                (int) (image.getWidth() * zoomFactor) + 20,
                (int) (image.getHeight() * zoomFactor) + 40
            );
        }
        return new Dimension(400, 300);
    }
}

