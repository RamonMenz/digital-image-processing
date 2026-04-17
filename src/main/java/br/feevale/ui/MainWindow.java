package br.feevale.ui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;

import br.feevale.core.ImageProcessor;
import br.feevale.model.ImageModel;

/**
 * Janela principal do sistema de processamento de imagens.
 * Contém dois painéis para exibição da imagem original e transformada,
 * além da barra de menus com todas as funcionalidades.
 */
public class MainWindow extends JFrame {

    private static final String[] AUTHORS = {
        "Sistema de Processamento Digital de Imagens"
    };

    private final ImageModel model;
    private final ImageProcessor processor;

    private ImagePanel originalPanel;
    private ImagePanel transformedPanel;
    private JLabel statusBar;

    public MainWindow() {
        this.model = new ImageModel();
        this.processor = new ImageProcessor(model);

        initializeUI();
    }

    /**
     * Inicializa a interface gráfica.
     */
    private void initializeUI() {
        configureAppearance();
        setTitle("Sistema de Processamento Digital de Imagens");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));

        // Configura o layout principal
        setLayout(new BorderLayout());

        // Adiciona a barra de cabeçalho com autores
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Adiciona os painéis de imagem
        add(createMainPanel(), BorderLayout.CENTER);

        // Adiciona a barra de status
        add(createStatusBar(), BorderLayout.SOUTH);

        // Configura a barra de menus
        final var menuBuilder = new MenuBarBuilder(this, processor);
        setJMenuBar(menuBuilder.getMenuBar());

        // Centraliza na tela
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Cria o painel de cabeçalho com os nomes dos autores.
     */
    private JPanel createHeaderPanel() {
        final var headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(new Color(35, 35, 35));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 60)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        for (final var author : AUTHORS) {
            final var label = new JLabel(author);
            label.setForeground(new Color(180, 180, 180));
            label.setFont(new Font("SansSerif", Font.BOLD, 14));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            headerPanel.add(label);
        }

        return headerPanel;
    }

    /**
     * Cria o painel principal com as duas áreas de imagem.
     */
    private JPanel createMainPanel() {
        final var mainPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        mainPanel.setBackground(new Color(40, 40, 40));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Painel da imagem original
        originalPanel = new ImagePanel("Imagem Original");

        // Painel da imagem transformada
        transformedPanel = new ImagePanel("Imagem Transformada");

        // Adiciona scroll para imagens grandes
        final var originalScroll = new JScrollPane(originalPanel);
        originalScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        originalScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        originalScroll.setBorder(null);
        originalScroll.getViewport().setBackground(new Color(45, 45, 45));

        final var transformedScroll = new JScrollPane(transformedPanel);
        transformedScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        transformedScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        transformedScroll.setBorder(null);
        transformedScroll.getViewport().setBackground(new Color(45, 45, 45));

        mainPanel.add(originalScroll);
        mainPanel.add(transformedScroll);

        return mainPanel;
    }

    /**
     * Cria a barra de status.
     */
    private JPanel createStatusBar() {
        final var statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(35, 35, 35));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 60, 60)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        statusBar = new JLabel("Pronto. Carregue uma imagem para começar.");
        statusBar.setForeground(new Color(150, 150, 150));
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 11));

        statusPanel.add(statusBar, BorderLayout.WEST);

        return statusPanel;
    }

    /**
     * Configura a aparência da aplicação.
     */
    private void configureAppearance() {
        try {
            // Tenta usar o look and feel do sistema
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        } catch (Exception e) {
            System.out.println("Não foi possível configurar o Look and Feel: " + e.getMessage());
            // Usa o look and feel padrão se falhar
        }
    }

    /**
     * Abre uma imagem de arquivo.
     */
    public void openImage() {
        final var fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Abrir Imagem");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Imagens (PNG, JPG, BMP)", "png", "jpg", "jpeg", "bmp"
        ));

        final var result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            final var selectedFile = fileChooser.getSelectedFile();

            try {
                model.loadImage(selectedFile);
                updateImages();
                if (getJMenuBar() != null) {
                    getJMenuBar().repaint();
                }
                updateStatus("Imagem carregada: " + selectedFile.getName() +
                    " (" + model.getWidth() + "x" + model.getHeight() + ")");
            } catch (IOException e) {
                showError("Erro ao carregar a imagem: " + e.getMessage());
            }
        }
    }

    /**
     * Salva a imagem transformada em arquivo.
     */
    public void saveImage() {
        if (!model.hasImage() || model.getTransformedImage() == null) {
            showError("Nenhuma imagem para salvar!");
            return;
        }

        final var fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Imagem");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("JPEG", "jpg", "jpeg"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("BMP", "bmp"));

        final var result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            var selectedFile = fileChooser.getSelectedFile();

            // Determina o formato baseado na extensão
            final var filename = selectedFile.getName().toLowerCase();
            String format;

            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                format = "jpg";
            } else if (filename.endsWith(".bmp")) {
                format = "bmp";
            } else {
                format = "png";
                if (!filename.endsWith(".png")) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + ".png");
                }
            }

            try {
                model.saveImage(selectedFile, format);
                updateStatus("Imagem salva: " + selectedFile.getName());
            } catch (IOException e) {
                showError("Erro ao salvar a imagem: " + e.getMessage());
            }
        }
    }

    /**
     * Atualiza as imagens exibidas nos painéis.
     */
    public void updateImages() {
        originalPanel.setImage(model.getOriginalImage());
        transformedPanel.setImage(model.getTransformedImage());
    }

    /**
     * Atualiza a mensagem da barra de status.
     */
    public void updateStatus(final String message) {
        statusBar.setText(message);
    }

    /**
     * Exibe uma mensagem de erro.
     */
    public void showError(final String message) {
        JOptionPane.showMessageDialog(this, message, "Erro", JOptionPane.ERROR_MESSAGE);
    }

}
