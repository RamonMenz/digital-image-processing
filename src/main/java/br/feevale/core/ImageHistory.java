package br.feevale.core;

import br.feevale.model.ImageModel;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Mantém um histórico de versões (snapshots) de uma imagem com capacidade limitada.
 *
 * <p>Implementado como uma lista de estados + cursor (índice atual), permitindo
 * navegação estilo "voltar/avançar" (undo/redo) com descarte automático do ramo
 * futuro quando uma nova versão é adicionada.</p>
 */
public final class ImageHistory {

    public static final int DEFAULT_MAX_STATES = 20;

    private final List<BufferedImage> states = new ArrayList<>();
    private int index = -1;
    private int maxStates;

    public ImageHistory() {
        this(DEFAULT_MAX_STATES);
    }

    public ImageHistory(final int maxStates) {
        setMaxStates(maxStates);
    }

    public void setMaxStates(final int maxStates) {
        this.maxStates = Math.max(1, maxStates);
        trimToLimit();
    }

    public int getMaxStates() {
        return maxStates;
    }

    /** Remove todos os estados e reseta o cursor. */
    public void clear() {
        states.clear();
        index = -1;
    }

    /**
     * Inicializa o histórico com um estado base (por exemplo, ao abrir imagem ou após reset).
     */
    public void initWith(final BufferedImage image) {
        clear();
        if (image != null) {
            states.add(ImageModel.copyImage(image));
            index = 0;
        }
    }

    /**
     * Adiciona um novo snapshot e posiciona o cursor nele.
     * Se houver estados "à frente" (redo), eles são descartados.
     */
    public void push(final BufferedImage image) {
        if (image == null) return;

        // descarta ramo futuro
        if (index < states.size() - 1) {
            states.subList(index + 1, states.size()).clear();
        }

        states.add(ImageModel.copyImage(image));
        index = states.size() - 1;

        trimToLimit();
    }

    public boolean canUndo() {
        return index > 0;
    }

    public boolean canRedo() {
        return index >= 0 && index < states.size() - 1;
    }

    /**
     * Move o cursor para trás e retorna uma cópia do estado atual.
     * Retorna null se não houver undo.
     */
    public BufferedImage undo() {
        if (!canUndo()) return null;
        index--;
        return ImageModel.copyImage(states.get(index));
    }

    /**
     * Move o cursor para frente e retorna uma cópia do estado atual.
     * Retorna null se não houver redo.
     */
    public BufferedImage redo() {
        if (!canRedo()) return null;
        index++;
        return ImageModel.copyImage(states.get(index));
    }

    /** Retorna uma cópia do estado atual, ou null se vazio. */
    public BufferedImage current() {
        if (index < 0 || index >= states.size()) return null;
        return ImageModel.copyImage(states.get(index));
    }

    private void trimToLimit() {
        if (states.isEmpty()) {
            index = -1;
            return;
        }

        while (states.size() > maxStates) {
            // remove estados mais antigos (início)
            states.removeFirst();
            index--;
        }

        if (index < 0) index = 0;
        if (index >= states.size()) index = states.size() - 1;
    }
}
