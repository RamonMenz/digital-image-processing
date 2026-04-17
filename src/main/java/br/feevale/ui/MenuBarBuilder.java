package br.feevale.ui;

import br.feevale.core.Filters;
import br.feevale.core.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;

/**
 * Classe responsável pela construção da barra de menus.
 * Etapa atual do curso: Transformações Geométricas e Filtros.
 * Funcionalidades ainda não implementadas exibem uma mensagem de "ainda não desenvolvida".
 */
public class MenuBarBuilder {
    private final JMenuBar menuBar;
    private final MainWindow mainWindow;
    private final ImageProcessor processor;

    private JMenuItem undoItem;
    private JMenuItem redoItem;

    private enum MorphologyAction {
        DILATE,
        ERODE,
        OPENING,
        CLOSING
    }

    public MenuBarBuilder(final MainWindow mainWindow, final ImageProcessor processor) {
        this.mainWindow = mainWindow;
        this.processor = processor;
        this.menuBar = new JMenuBar();
        applyLookAndFeelColors();
        buildMenus();
        updateUndoRedoState();
    }

    /**
     * Faz a JMenuBar herdar corretamente o visual do Look & Feel do sistema.
     */
    private void applyLookAndFeelColors() {
        Color background = UIManager.getColor("MenuBar.background");
        if (background == null) {
            background = UIManager.getColor("Panel.background");
        }
        if (background != null) {
            menuBar.setBackground(background);
            menuBar.setOpaque(true);
        }
        Color foreground = UIManager.getColor("MenuBar.foreground");
        if (foreground != null) {
            menuBar.setForeground(foreground);
        }
        var border = UIManager.getBorder("MenuBar.border");
        if (border != null) {
            menuBar.setBorder(border);
        }
    }

    private void buildMenus() {
        menuBar.add(buildFileMenu());
        menuBar.add(buildEditMenu());
        menuBar.add(buildGeometricTransformationsMenu());
        menuBar.add(buildFiltersMenu());
        menuBar.add(buildMorphologyMenu());
        menuBar.add(buildFeatureExtractionMenu());
    }

    private JMenu buildEditMenu() {
        JMenu menu = createMenu("Editar");

        undoItem = createMenuItem("Voltar", "ctrl Z", e -> {
            if (processor.undo()) {
                mainWindow.updateImages();
            }
            updateUndoRedoState();
        });
        redoItem = createMenuItem("Avançar", "ctrl Y", e -> {
            if (processor.redo()) {
                mainWindow.updateImages();
            }
            updateUndoRedoState();
        });

        menu.add(undoItem);
        menu.add(redoItem);
        return menu;
    }

    private JMenu buildFileMenu() {
        JMenu menu = createMenu("Arquivo");
        menu.add(createMenuItem("Abrir Imagem...", "ctrl O", e -> mainWindow.openImage()));
        menu.add(createMenuItem("Salvar Imagem...", "ctrl S", e -> mainWindow.saveImage()));
        menu.addSeparator();
        menu.add(createMenuItem("Aplicar como Original", "ctrl ENTER", e -> {
            processor.applyTransformationAsOriginal();
            mainWindow.updateImages();
            updateUndoRedoState();
        }));
        menu.add(createMenuItem("Restaurar Original", "ctrl R", e -> {
            processor.resetToOriginal();
            mainWindow.updateImages();
            updateUndoRedoState();
        }));
        menu.addSeparator();
        menu.add(createMenuItem("Sobre", null, e -> showAboutDialog()));
        menu.addSeparator();
        menu.add(createMenuItem("Sair", "alt F4", e -> System.exit(0)));
        return menu;
    }

    private JMenu buildGeometricTransformationsMenu() {
        JMenu menu = createMenu("Transformações Geométricas");
        menu.add(createMenuItem("Translação...", null, e -> showTranslationDialog()));
        menu.addSeparator();
        JMenu rotationMenu = createMenu("Rotação");
        rotationMenu.add(createMenuItem("Rotação Personalizada...", null, e -> showRotationDialog()));
        rotationMenu.addSeparator();
        rotationMenu.add(createMenuItem("90° Horário", null, e -> applyTransformation(() -> processor.rotate(90))));
        rotationMenu.add(createMenuItem("90° Anti-horário", null, e -> applyTransformation(() -> processor.rotate(-90))));
        rotationMenu.add(createMenuItem("180°", null, e -> applyTransformation(() -> processor.rotate(180))));
        menu.add(rotationMenu);
        menu.addSeparator();
        JMenu mirrorMenu = createMenu("Espelhamento");
        mirrorMenu.add(createMenuItem("Horizontal", null, e -> applyTransformation(processor::mirrorHorizontal)));
        mirrorMenu.add(createMenuItem("Vertical", null, e -> applyTransformation(processor::mirrorVertical)));
        menu.add(mirrorMenu);
        menu.addSeparator();
        JMenu scaleMenu = createMenu("Escala");
        scaleMenu.add(createMenuItem("Aumentar (2x)", null, e -> applyTransformation(() -> processor.scaleUp(2, 2))));
        scaleMenu.add(createMenuItem("Aumentar (1.5x)", null, e -> applyTransformation(() -> processor.scaleUp(1.5, 1.5))));
        scaleMenu.addSeparator();
        scaleMenu.add(createMenuItem("Diminuir (2x)", null, e -> applyTransformation(() -> processor.scaleDown(2, 2))));
        scaleMenu.add(createMenuItem("Diminuir (1.5x)", null, e -> applyTransformation(() -> processor.scaleDown(1.5, 1.5))));
        scaleMenu.addSeparator();
        scaleMenu.add(createMenuItem("Escala Personalizada...", null, e -> showScaleDialog()));
        menu.add(scaleMenu);
        return menu;
    }

    private JMenu buildFiltersMenu() {
        JMenu menu = createMenu("Filtros");

        menu.add(createMenuItem("Brilho e Contraste...", null, e -> showBrightnessContrastDialog()));
        menu.addSeparator();

        JMenu grayscaleMenu = createMenu("Grayscale");
        grayscaleMenu.add(createMenuItem("Média", null,
                e -> applyTransformation(() -> processor.grayscale(Filters.GrayscaleType.AVERAGE))));
        grayscaleMenu.add(createMenuItem("Ponderado (0.2125, 0.7154, 0.0721)", null,
                e -> applyTransformation(() -> processor.grayscale(Filters.GrayscaleType.WEIGHTED_2125_7154_0721_DIV3))));
        grayscaleMenu.add(createMenuItem("Ponderado (0.50, 0.419, 0.081)", null,
                e -> applyTransformation(() -> processor.grayscale(Filters.GrayscaleType.WEIGHTED_0500_0419_0081_DIV3))));
        menu.add(grayscaleMenu);

        menu.add(createMenuItem("Negativo", null, e -> applyTransformation(processor::negative)));
        menu.addSeparator();
        menu.add(createMenuItem("Passa Baixa (Suavização)...", null, e -> showLowPassDialog()));
        menu.add(createMenuItem("Passa Alta (Aguçamento)...", null, e -> showSharpenDialog()));
        menu.addSeparator();
        menu.add(createMenuItem("Threshold (Binarização)...", null, e -> showThresholdDialog()));
        return menu;
    }

    private JMenu buildMorphologyMenu() {
        JMenu menu = createMenu("Morfologia Matemática");
        menu.add(createMenuItem("Dilatação...", null, e -> showMorphologyDialog(
                "Dilatação",
                MorphologyAction.DILATE
        )));
        menu.add(createMenuItem("Erosão...", null, e -> showMorphologyDialog(
                "Erosão",
                MorphologyAction.ERODE
        )));
        menu.addSeparator();
        menu.add(createMenuItem("Abertura...", null, e -> showMorphologyDialog(
                "Abertura",
                MorphologyAction.OPENING
        )));
        menu.add(createMenuItem("Fechamento...", null, e -> showMorphologyDialog(
                "Fechamento",
                MorphologyAction.CLOSING
        )));
        menu.addSeparator();
        menu.add(createMenuItem("Afinamento...", null, e -> showThinningDialog()));
        menu.add(createMenuItem("Extração de Contorno...", null, e -> showContourDialog()));
        return menu;
    }

    private JMenu buildFeatureExtractionMenu() {
        JMenu menu = createMenu("Extração de Características");

        JMenu edgesMenu = createMenu("Detecção de Bordas");
        edgesMenu.add(createMenuItem("Sobel...", null, e -> showSobelDialog()));
        edgesMenu.add(createMenuItem("Canny...", null, e -> showCannyDialog()));
        menu.add(edgesMenu);

        menu.add(createMenuItem("Contagem de Objetos...", null, e -> showObjectCountDialog()));
        menu.add(createMenuItem("Histograma de Intensidades", null, e -> showHistogramDialog()));
        menu.add(createMenuItem("Detecção de Cantos...", null, e -> showCornerDialog()));
        menu.addSeparator();
        menu.add(createMenuItem("Desafio", null, e -> showUnavailableFeature("Desafio")));
        return menu;
    }

    private JMenu createMenu(final String title) {
        return new JMenu(title);
    }

    private JMenuItem createMenuItem(String title, String accelerator, ActionListener action) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(action);
        if (accelerator != null) {
            item.setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }
        return item;
    }

    private void applyTransformation(final Runnable transformation) {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }
        mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                transformation.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    mainWindow.updateImages();
                    updateUndoRedoState();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    mainWindow.showError("Operação interrompida.");
                } catch (ExecutionException ex) {
                    final Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    mainWindow.showError("Falha ao aplicar transformação: " + cause.getMessage());
                } finally {
                    mainWindow.setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    private void updateUndoRedoState() {
        if (undoItem != null) {
            undoItem.setEnabled(processor.hasImage() && processor.canUndo());
        }
        if (redoItem != null) {
            redoItem.setEnabled(processor.hasImage() && processor.canRedo());
        }
    }

    private void showAboutDialog() {
        String message = """
                <html>
                <div style='text-align: center; padding: 10px;'>
                    <h2>Sistema de Processamento Digital de Imagens</h2>
                    <p>Feito por: Ramon Pedro Menz</p>
                    <br>
                    <p>Java %s • Swing</p>
                </div>
                </html>
                """.formatted(System.getProperty("java.version"));
        JOptionPane.showMessageDialog(mainWindow, message, "Sobre", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showTranslationDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JSpinner dxSpinner = new JSpinner(new SpinnerNumberModel(0, -2000, 2000, 10));
        JSpinner dySpinner = new JSpinner(new SpinnerNumberModel(0, -2000, 2000, 10));
        panel.add(new JLabel("Deslocamento X:"));
        panel.add(dxSpinner);
        panel.add(new JLabel("Deslocamento Y:"));
        panel.add(dySpinner);
        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Translação", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int dx = (int) dxSpinner.getValue();
            int dy = (int) dySpinner.getValue();
            applyTransformation(() -> processor.translate(dx, dy));
        }
    }

    private void showRotationDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JSpinner angleSpinner = new JSpinner(new SpinnerNumberModel(45.0, -360.0, 360.0, 15.0));
        JCheckBox keepSizeCheck = new JCheckBox("Manter dimensões originais");
        panel.add(new JLabel("Ângulo (graus):"));
        panel.add(angleSpinner);
        panel.add(keepSizeCheck);
        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Rotação", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            double angle = (double) angleSpinner.getValue();
            boolean keepSize = keepSizeCheck.isSelected();
            if (keepSize) {
                applyTransformation(() -> processor.rotateKeepSize(angle));
            } else {
                applyTransformation(() -> processor.rotate(angle));
            }
        }
    }

    private void showScaleDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        JSpinner factorXSpinner = new JSpinner(new SpinnerNumberModel(2.0, 0.1, 10.0, 0.1));
        JSpinner factorYSpinner = new JSpinner(new SpinnerNumberModel(2.0, 0.1, 10.0, 0.1));
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Aumentar", "Diminuir"});
        panel.add(new JLabel("Fator de escala (X):"));
        panel.add(factorXSpinner);
        panel.add(new JLabel("Fator de escala (Y):"));
        panel.add(factorYSpinner);
        panel.add(new JLabel("Tipo:"));
        panel.add(typeCombo);
        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Escala", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            double factorX = (double) factorXSpinner.getValue();
            double factorY = (double) factorYSpinner.getValue();
            boolean scaleUp = typeCombo.getSelectedIndex() == 0;
            if (scaleUp) {
                applyTransformation(() -> processor.scaleUp(factorX, factorY));
            } else {
                applyTransformation(() -> processor.scaleDown(factorX, factorY));
            }
        }
    }

    private void showLowPassDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JComboBox<Filters.BlurType> typeCombo = new JComboBox<>(Filters.BlurType.values());
        JSpinner kernelSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 99, 2));

        panel.add(new JLabel("Tipo de blur:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Tamanho do kernel (ímpar):"));
        panel.add(kernelSpinner);

        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Passa Baixa", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            Filters.BlurType type = (Filters.BlurType) typeCombo.getSelectedItem();
            int kernelSize = (int) kernelSpinner.getValue();
            if (type != null) {
                applyTransformation(() -> processor.lowPass(type, kernelSize));
            }
        }
    }

    private void showBrightnessContrastDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JSpinner contrastSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 5.0, 0.1));
        JSpinner brightnessSpinner = new JSpinner(new SpinnerNumberModel(0, -255, 255, 5));

        panel.add(new JLabel("Contraste (C):"));
        panel.add(contrastSpinner);
        panel.add(new JLabel("Brilho (B):"));
        panel.add(brightnessSpinner);

        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Brilho e Contraste", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            double contrast = ((Number) contrastSpinner.getValue()).doubleValue();
            int brightness = ((Number) brightnessSpinner.getValue()).intValue();
            applyTransformation(() -> processor.adjustBrightnessContrast(contrast, brightness));
        }
    }

    private void showSharpenDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        JComboBox<Filters.BlurType> typeCombo = new JComboBox<>(Filters.BlurType.values());
        JSpinner kernelSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 99, 2));
        JSpinner amountSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 10.0, 0.1));

        panel.add(new JLabel("Tipo de blur:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Tamanho do kernel (ímpar):"));
        panel.add(kernelSpinner);
        panel.add(new JLabel("Intensidade (amount):"));
        panel.add(amountSpinner);

        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Passa Alta (Aguçamento)", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            Filters.BlurType type = (Filters.BlurType) typeCombo.getSelectedItem();
            int kernelSize = (int) kernelSpinner.getValue();
            double amount = ((Number) amountSpinner.getValue()).doubleValue();
            if (type != null) {
                applyTransformation(() -> processor.highPass(type, kernelSize, amount));
            }
        }
    }

    private void showThresholdDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(128, 0, 255, 1));
        panel.add(new JLabel("Valor do threshold (0-255):"));
        panel.add(thresholdSpinner);

        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Threshold (Binarização)", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int threshold = (int) thresholdSpinner.getValue();
            applyTransformation(() -> processor.threshold(threshold));
        }
    }

    private void showMorphologyDialog(final String operationTitle, final MorphologyAction action) {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        JSpinner kernelSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 31, 2));
        JSpinner iterationsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(128, 0, 255, 1));

        panel.add(new JLabel("Tamanho do elemento estruturante (ímpar):"));
        panel.add(kernelSpinner);
        panel.add(new JLabel("Iterações:"));
        panel.add(iterationsSpinner);
        panel.add(new JLabel("Threshold de binarização (0-255):"));
        panel.add(thresholdSpinner);

        int result = JOptionPane.showConfirmDialog(mainWindow, panel, operationTitle, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int kernelSize = ((Number) kernelSpinner.getValue()).intValue();
            int iterations = ((Number) iterationsSpinner.getValue()).intValue();
            int threshold = ((Number) thresholdSpinner.getValue()).intValue();

            applyTransformation(() -> {
                switch (action) {
                    case DILATE -> processor.dilate(kernelSize, iterations, threshold);
                    case ERODE -> processor.erode(kernelSize, iterations, threshold);
                    case OPENING -> processor.opening(kernelSize, iterations, threshold);
                    case CLOSING -> processor.closing(kernelSize, iterations, threshold);
                }
            });
        }
    }

    private void showThinningDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(128, 0, 255, 1));
        JSpinner maxIterationsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 300, 1));

        panel.add(new JLabel("Threshold de binarização (0-255):"));
        panel.add(thresholdSpinner);
        panel.add(new JLabel("Máx. iterações (0 = até convergir):"));
        panel.add(maxIterationsSpinner);

        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Afinamento", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int threshold = ((Number) thresholdSpinner.getValue()).intValue();
            int maxIterations = ((Number) maxIterationsSpinner.getValue()).intValue();
            applyTransformation(() -> processor.thinning(threshold, maxIterations));
        }
    }

    private void showContourDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JSpinner kernelSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 31, 2));
        JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(128, 0, 255, 1));

        panel.add(new JLabel("Tamanho do elemento estruturante (ímpar):"));
        panel.add(kernelSpinner);
        panel.add(new JLabel("Threshold de binarização (0-255):"));
        panel.add(thresholdSpinner);

        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Extração de Contorno", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int kernelSize = ((Number) kernelSpinner.getValue()).intValue();
            int threshold = ((Number) thresholdSpinner.getValue()).intValue();
            applyTransformation(() -> processor.extractContour(kernelSize, threshold));
        }
    }

    private void showSobelDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(90, 0, 255, 1));
        panel.add(new JLabel("Threshold de borda (0-255):"));
        panel.add(thresholdSpinner);

        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Detecção de Bordas - Sobel", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int threshold = ((Number) thresholdSpinner.getValue()).intValue();
            applyTransformation(() -> processor.detectEdgesSobel(threshold));
        }
    }

    private void showCannyDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JSpinner lowSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 255, 1));
        JSpinner highSpinner = new JSpinner(new SpinnerNumberModel(120, 0, 255, 1));
        panel.add(new JLabel("Threshold baixo (0-255):"));
        panel.add(lowSpinner);
        panel.add(new JLabel("Threshold alto (0-255):"));
        panel.add(highSpinner);

        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Detecção de Bordas - Canny", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int low = ((Number) lowSpinner.getValue()).intValue();
            int high = ((Number) highSpinner.getValue()).intValue();
            if (low >= high) {
                mainWindow.showError("O threshold baixo deve ser menor que o threshold alto.");
                return;
            }
            applyTransformation(() -> processor.detectEdgesCanny(low, high));
        }
    }

    private void showObjectCountDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(128, 0, 255, 1));
        JComboBox<String> connectivityCombo = new JComboBox<>(new String[]{"4-conectado", "8-conectado"});
        JSpinner minAreaSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 50000, 1));
        panel.add(new JLabel("Threshold de binarização (0-255):"));
        panel.add(thresholdSpinner);
        panel.add(new JLabel("Conectividade:"));
        panel.add(connectivityCombo);
        panel.add(new JLabel("Área mínima (pixels):"));
        panel.add(minAreaSpinner);

        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Contagem de Objetos", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int threshold = ((Number) thresholdSpinner.getValue()).intValue();
            boolean useEightConnectivity = connectivityCombo.getSelectedIndex() == 1;
            int minArea = ((Number) minAreaSpinner.getValue()).intValue();
            int count = processor.countObjects(threshold, useEightConnectivity, minArea);

            String connectivityText = useEightConnectivity ? "8" : "4";
            String message = "Objetos detectados: " + count + "\nConectividade: " + connectivityText
                    + "\nÁrea mínima: " + minArea + " px";
            JOptionPane.showMessageDialog(mainWindow, message, "Contagem de Objetos", JOptionPane.INFORMATION_MESSAGE);
            mainWindow.updateStatus("Contagem de objetos concluída: " + count + " objetos.");
        }
    }

    private void showHistogramDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }

        int[] histogram = processor.intensityHistogram();
        int totalPixels = 0;
        int peakIntensity = 0;
        int peakCount = 0;

        for (int i = 0; i < histogram.length; i++) {
            totalPixels += histogram[i];
            if (histogram[i] > peakCount) {
                peakCount = histogram[i];
                peakIntensity = i;
            }
        }

        String message = "Pixels analisados: " + totalPixels
                + "\nPico do histograma: intensidade " + peakIntensity + " (" + peakCount + " pixels)"
                + "\nO histograma visual foi aplicado na imagem transformada.";
        JOptionPane.showMessageDialog(mainWindow, message, "Histograma de Intensidades", JOptionPane.INFORMATION_MESSAGE);

        applyTransformation(() -> processor.renderIntensityHistogram(640, 360));
    }

    private void showCornerDialog() {
        if (!processor.hasImage()) {
            mainWindow.showError("Nenhuma imagem carregada!");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JSpinner thresholdRatioSpinner = new JSpinner(new SpinnerNumberModel(0.05, 0.001, 0.5, 0.005));
        JSpinner suppressionRadiusSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 8, 1));
        panel.add(new JLabel("Razão de threshold (0.001-0.5):"));
        panel.add(thresholdRatioSpinner);
        panel.add(new JLabel("Raio de supressão local:"));
        panel.add(suppressionRadiusSpinner);

        int result = JOptionPane.showConfirmDialog(mainWindow, panel, "Detecção de Cantos (Harris)", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            double thresholdRatio = ((Number) thresholdRatioSpinner.getValue()).doubleValue();
            int suppressionRadius = ((Number) suppressionRadiusSpinner.getValue()).intValue();

            mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SwingWorker<Integer, Void> worker = new SwingWorker<>() {
                @Override
                protected Integer doInBackground() {
                    return processor.detectCorners(thresholdRatio, suppressionRadius);
                }

                @Override
                protected void done() {
                    mainWindow.setCursor(Cursor.getDefaultCursor());
                    try {
                        int corners = get();
                        mainWindow.updateImages();
                        updateUndoRedoState();
                        mainWindow.updateStatus("Detecção de cantos concluída: " + corners + " cantos encontrados.");
                        JOptionPane.showMessageDialog(
                                mainWindow,
                                "Cantos detectados: " + corners,
                                "Detecção de Cantos",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } catch (InterruptedException | ExecutionException ex) {
                        mainWindow.showError("Falha na detecção de cantos: " + ex.getMessage());
                        Thread.currentThread().interrupt();
                    }
                }
            };
            worker.execute();
        }
    }


    private void showUnavailableFeature(final String featureName) {
        JOptionPane.showMessageDialog(
                mainWindow,
                "A funcionalidade '" + featureName + "' ainda não foi desenvolvida.",
                "Ainda não desenvolvido",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }
}
