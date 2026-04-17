package br.feevale;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import br.feevale.ui.MainWindow;

public class App {

    public static void main(final String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Não foi possível configurar o Look and Feel: " + e.getMessage());
            }

            final var mainWindow = new MainWindow();
            mainWindow.setVisible(true);

            System.out.println("Sistema de Processamento Digital de Imagens iniciado.");
        });
    }

}
