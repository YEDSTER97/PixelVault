import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;


public class Main {

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
            System.out.println("FlatLaf Light Theme Initialized");
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                System.out.println("Using system Look and Feel as fallback.");
            } catch (Exception ignored) {
                System.err.println("Failed to set system L&F as fallback.");
            }
        }

        SwingUtilities.invokeLater(() -> {
            PixelVaultGui frame = new PixelVaultGui();
            frame.setVisible(true);
        });
    }
}