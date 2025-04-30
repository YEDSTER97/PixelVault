import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import javax.crypto.BadPaddingException;
import javax.crypto.AEADBadTagException;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.SystemInfo;

public class PixelVaultGui extends JFrame {

    // --- Branding & Constants ---
    private static final String APP_NAME = "PixelVault";
    private static final String DEVELOPER_NAME = "Super_Devs";
    private static final int PREVIEW_SIZE = 200;
    private static final int ENCRYPTED_PREVIEW_BUFFER_SIZE = 4096;
    private static final String PREVIEW_PLACEHOLDER = "-- Preview --";
    private static final String NO_IMAGE_PLACEHOLDER = "[Invalid/Error]";

    // --- GUI Components ---
    private JRadioButton encryptRadioButton;
    private JRadioButton decryptRadioButton;
    private JTextField inputFileField;
    private JTextField outputFileField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheckBox;
    private JCheckBox deleteInputCheckBox;
    private JButton inputBrowseButton;
    private JButton outputBrowseButton;
    private JButton executeButton;
    private JButton resetButton;
    private JTextArea statusTextArea;
    private JProgressBar progressBar;
    private JCheckBox themeCheckBox;
    private JLabel footerLabel;
    private JLabel beforePreviewLabel;
    private JLabel afterPreviewLabel;

    private JFileChooser fileChooser;
    private static boolean isDarkMode = false;

    // --- Variables for Button Hover Effects ---
    private Color originalExecuteForeground, hoverExecuteForeground;
    private Color originalResetForeground, hoverResetForeground;
    private Border originalExecuteBorder, hoverExecuteBorder;
    private Border originalResetBorder, hoverResetBorder;

    // --- Constructor ---
    public PixelVaultGui() {
        super(APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(650, 680));
        setLocationRelativeTo(null);

        if (SystemInfo.isMacOS) {
            getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(
                    SystemInfo.isMacFullWindowContentSupported ? 25 : 0, 0, 0, 0));
        }

        fileChooser = new JFileChooser();
        UIManager.put("Button.arc", 6);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JPanel headerPanel = createHeaderPanel();
        JPanel configPanel = createConfigPanel();
        JPanel previewPanel = createPreviewPanel();
        JScrollPane statusScrollPane = createStatusPanel();
        JPanel actionPanel = createActionPanel();
        JPanel footerPanel = createFooterPanel();

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(configPanel, BorderLayout.CENTER);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel centerSplitPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        centerSplitPanel.add(configPanel);
        centerSplitPanel.add(previewPanel);
        mainPanel.add(centerSplitPanel, BorderLayout.CENTER);

        JPanel bottomOuterPanel = new JPanel(new BorderLayout(10, 10));
        bottomOuterPanel.add(statusScrollPane, BorderLayout.CENTER);
        JPanel bottomActionFooterPanel = new JPanel(new BorderLayout(0, 5));
        bottomActionFooterPanel.add(actionPanel, BorderLayout.NORTH);
        bottomActionFooterPanel.add(footerPanel, BorderLayout.CENTER);
        bottomOuterPanel.add(bottomActionFooterPanel, BorderLayout.SOUTH);
        mainPanel.add(bottomOuterPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();

        setupInputValidationListener();
        setupActionListeners();
        initializeButtonHoverEffects();
        updateUiForMode();
        clearPreviewLabels();
        validateInputs();
    }

    // --- Panel Creation Helpers ---
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel titleLabel = new JLabel(APP_NAME);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 24));
        panel.add(titleLabel);
        return panel;
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(" Configuration "));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        encryptRadioButton = new JRadioButton("Encrypt", true);
        decryptRadioButton = new JRadioButton("Decrypt");
        ButtonGroup bg = new ButtonGroup(); bg.add(encryptRadioButton); bg.add(decryptRadioButton);
        JPanel mp = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); mp.add(encryptRadioButton); mp.add(decryptRadioButton);
        gbc.gridx=0; gbc.gridy=0; gbc.weightx=0.0; panel.add(new JLabel("Operation:"), gbc);
        gbc.gridx=1; gbc.gridwidth=2; gbc.weightx=1.0; panel.add(mp, gbc);

        gbc.gridwidth=1; gbc.gridx=0; gbc.gridy++; gbc.weightx=0.0; panel.add(new JLabel("Input File:"), gbc);
        inputFileField = new JTextField(30);
        gbc.gridx=1; gbc.weightx=1.0; panel.add(inputFileField, gbc);
        inputBrowseButton = new JButton("Browse..."); inputBrowseButton.setToolTipText("Select Input File");
        gbc.gridx=2; gbc.weightx=0.0; panel.add(inputBrowseButton, gbc);

        gbc.gridx=0; gbc.gridy++; gbc.weightx=0.0; panel.add(new JLabel("Output File:"), gbc);
        outputFileField = new JTextField(30);
        gbc.gridx=1; gbc.weightx=1.0; panel.add(outputFileField, gbc);
        outputBrowseButton = new JButton("Browse..."); outputBrowseButton.setToolTipText("Select Output File Location");
        gbc.gridx=2; gbc.weightx=0.0; panel.add(outputBrowseButton, gbc);

        gbc.gridx=0; gbc.gridy++; gbc.weightx=0.0; panel.add(new JLabel("Password:"), gbc);
        passwordField = new JPasswordField(30);
        gbc.gridx=1; gbc.weightx=1.0; panel.add(passwordField, gbc);
        showPasswordCheckBox = new JCheckBox("Show"); showPasswordCheckBox.setToolTipText("Show/Hide Password");
        gbc.gridx=2; gbc.weightx=0.0; panel.add(showPasswordCheckBox, gbc);

        return panel;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 0));
        panel.setBorder(BorderFactory.createTitledBorder(" Preview "));

        beforePreviewLabel = createPreviewLabel(PREVIEW_PLACEHOLDER);
        afterPreviewLabel = createPreviewLabel(PREVIEW_PLACEHOLDER);
        JScrollPane sp1 = new JScrollPane(beforePreviewLabel);
        sp1.setBorder(BorderFactory.createTitledBorder("Original"));
        JScrollPane sp2 = new JScrollPane(afterPreviewLabel);
        sp2.setBorder(BorderFactory.createTitledBorder("Result"));
        panel.add(sp1);
        panel.add(sp2);
        return panel;
    }

    private JLabel createPreviewLabel(String initialText) {
        JLabel label = new JLabel(initialText, SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(PREVIEW_SIZE + 20, PREVIEW_SIZE + 20));
        label.setMinimumSize(new Dimension(PREVIEW_SIZE / 2, PREVIEW_SIZE / 2));
        label.setBorder(BorderFactory.createEtchedBorder());
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private JScrollPane createStatusPanel() {
        statusTextArea = new JTextArea(6, 40);
        statusTextArea.setEditable(false);
        statusTextArea.setLineWrap(true);
        statusTextArea.setWrapStyleWord(true);
        statusTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(statusTextArea);
        sp.setBorder(BorderFactory.createTitledBorder(" Status Log "));
        return sp;
    }

    private JPanel createActionPanel() {
        JPanel actionOuterPanel = new JPanel(new BorderLayout(10, 5));
        JPanel buttonProgressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        executeButton = new JButton("Encrypt File");
        executeButton.setFont(executeButton.getFont().deriveFont(Font.BOLD));
        executeButton.setMargin(new Insets(5, 15, 5, 15));
        resetButton = new JButton("Reset");
        resetButton.setMargin(new Insets(5, 15, 5, 15));
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(150, 26));
        progressBar.setVisible(false);
        buttonProgressPanel.add(executeButton);
        buttonProgressPanel.add(resetButton);
        buttonProgressPanel.add(progressBar);

        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        deleteInputCheckBox = new JCheckBox("Delete Original Input on Success");
        deleteInputCheckBox.setToolTipText("WARNING: Permantently deletes input file!");
        deleteInputCheckBox.setFont(deleteInputCheckBox.getFont().deriveFont(11f));
        checkboxPanel.add(deleteInputCheckBox);

        actionOuterPanel.add(buttonProgressPanel, BorderLayout.CENTER);
        actionOuterPanel.add(checkboxPanel, BorderLayout.SOUTH);
        return actionOuterPanel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(new EmptyBorder(5, 0, 0, 0));
        themeCheckBox = new JCheckBox("Dark Mode");
        themeCheckBox.setSelected(isDarkMode);
        footerLabel = new JLabel("Developed by " + DEVELOPER_NAME + " ");
        footerLabel.setFont(footerLabel.getFont().deriveFont(Font.ITALIC, 10f));
        footerLabel.setForeground(Color.GRAY);
        panel.add(themeCheckBox, BorderLayout.WEST);
        panel.add(footerLabel, BorderLayout.EAST);
        return panel;
    }

    // --- Initialize and Apply Hover Effects ---
    private void initializeButtonHoverEffects() {
        Color hoverFg = UIManager.getColor("Button.hoverForeground");
        Color focusColor = UIManager.getColor("Component.focusColor");
        if (focusColor == null) { focusColor = new Color(90, 150, 220); }

        if (executeButton != null) {
            originalExecuteForeground = executeButton.getForeground();
            originalExecuteBorder = executeButton.getBorder();
            hoverExecuteForeground = (hoverFg != null) ? hoverFg : Color.WHITE;
            hoverExecuteBorder = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(focusColor, 2), originalExecuteBorder);
            MouseAdapter executeAdapter = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { if (executeButton.isEnabled()) { executeButton.setForeground(hoverExecuteForeground); executeButton.setBorder(hoverExecuteBorder); } }
                @Override public void mouseExited(MouseEvent e) { executeButton.setForeground(originalExecuteForeground); executeButton.setBorder(originalExecuteBorder); }
            };
            executeButton.addMouseListener(executeAdapter);
        }

        if (resetButton != null) {
            originalResetForeground = resetButton.getForeground();
            originalResetBorder = resetButton.getBorder();
            hoverResetForeground = (hoverFg != null) ? hoverFg.darker() : Color.DARK_GRAY;
            hoverResetBorder = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 2), originalResetBorder);
            MouseAdapter resetAdapter = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { if (resetButton.isEnabled()) { resetButton.setForeground(hoverResetForeground); resetButton.setBorder(hoverResetBorder); } }
                @Override public void mouseExited(MouseEvent e) { resetButton.setForeground(originalResetForeground); resetButton.setBorder(originalResetBorder); }
            };
            resetButton.addMouseListener(resetAdapter);
        }
    }


    // --- Image / Preview Handling ---
    private void clearPreviewLabels() {
        SwingUtilities.invokeLater(() -> {
            if (beforePreviewLabel != null) {
                beforePreviewLabel.setIcon(null);
                beforePreviewLabel.setText(PREVIEW_PLACEHOLDER);
                beforePreviewLabel.setToolTipText(null);
            }
            if (afterPreviewLabel != null) {
                afterPreviewLabel.setIcon(null);
                afterPreviewLabel.setText(PREVIEW_PLACEHOLDER);
                afterPreviewLabel.setToolTipText(null);
            }
        });
    }

    private void clearAfterPreviewLabel(String placeholderText) {
        SwingUtilities.invokeLater(() -> {
            if (afterPreviewLabel != null) {
                afterPreviewLabel.setIcon(null);
                afterPreviewLabel.setText(placeholderText);
                afterPreviewLabel.setToolTipText(null);
            }
        });
    }

    private void loadImageForPreview(String filePath, JLabel targetLabel) {
        if (filePath == null || filePath.trim().isEmpty()) {
            if (targetLabel == beforePreviewLabel) clearPreviewLabels();
            else clearAfterPreviewLabel(PREVIEW_PLACEHOLDER);
            return;
        }

        if (filePath.toLowerCase().endsWith(".enc")) {
            displayEncryptedPreview(filePath, targetLabel); // Use noise gen
            if (targetLabel == beforePreviewLabel) clearAfterPreviewLabel(PREVIEW_PLACEHOLDER);
            return;
        }

        try {
            File f = new File(filePath);
            if (!f.exists() || !f.isFile()) throw new FileNotFoundException("File not found.");
            BufferedImage i = ImageIO.read(f);
            if (i == null) throw new IOException("Unsupported format.");
            ImageIcon sI = scaleImage(i, PREVIEW_SIZE, PREVIEW_SIZE);
            SwingUtilities.invokeLater(() -> {
                targetLabel.setIcon(sI);
                targetLabel.setText(null);
                targetLabel.setToolTipText(filePath + " (" + i.getWidth() + "x" + i.getHeight() + ")");
            });
            if (targetLabel == beforePreviewLabel) clearAfterPreviewLabel(PREVIEW_PLACEHOLDER);
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                targetLabel.setIcon(null);
                targetLabel.setText(NO_IMAGE_PLACEHOLDER);
                String m = "Preview Err: " + (e instanceof FileNotFoundException ? "Not Found." : e.getMessage());
                targetLabel.setToolTipText(m);
                logStatus(m);
                if (targetLabel == beforePreviewLabel) clearAfterPreviewLabel(NO_IMAGE_PLACEHOLDER);
            });
            e.printStackTrace();
        }
    }

    private ImageIcon scaleImage(BufferedImage img, int maxW, int maxH) {
        int oW = img.getWidth(), oH = img.getHeight();
        if (oW <= maxW && oH <= maxH) return new ImageIcon(img);
        double r = Math.min((double) maxW / oW, (double) maxH / oH);
        int nW = (int) (oW * r), nH = (int) (oH * r);
        return new ImageIcon(img.getScaledInstance(nW, nH, Image.SCALE_SMOOTH));
    }

    private ImageIcon generateEncryptedPreview(String fp) {
        byte[] b = new byte[ENCRYPTED_PREVIEW_BUFFER_SIZE]; int br = 0;
        try (InputStream fis = new BufferedInputStream(Files.newInputStream(Paths.get(fp)))) {
            if (fis.skip(CryptoManager.SALT_LENGTH + CryptoManager.IV_LENGTH) != (CryptoManager.SALT_LENGTH + CryptoManager.IV_LENGTH)) { logStatus("EncP Err:Short Hdr"); return null; }
            br = fis.read(b);
        } catch (Exception e) { logStatus("EncP Err:Read " + e.getMessage()); return null; }
        if (br <= 0) { logStatus("EncP Err:No Data"); return null; }
        BufferedImage i = new BufferedImage(PREVIEW_SIZE, PREVIEW_SIZE, BufferedImage.TYPE_INT_ARGB);
        int[] p = ((DataBufferInt) i.getRaster().getDataBuffer()).getData();
        Random r = new Random(br ^ b[0]);
        int idx = 0;
        for (int k = 0; k < p.length; k++) {
            int R = b[idx++ % br] & 0xFF, G = b[idx++ % br] & 0xFF, B = b[idx++ % br] & 0xFF;
            R = (R + r.nextInt(32) - 16) & 0xFF; G = (G + r.nextInt(32) - 16) & 0xFF; B = (B + r.nextInt(32) - 16) & 0xFF;
            p[k] = (255 << 24) | (R << 16) | (G << 8) | B;
        }
        return new ImageIcon(i);
    }

    private void displayEncryptedPreview(String fp, JLabel tl) {
        tl.setText("Generating Preview...");
        tl.setIcon(null);
        SwingWorker<ImageIcon, Void> w = new SwingWorker<>() {
            protected ImageIcon doInBackground() throws Exception { return generateEncryptedPreview(fp); }
            protected void done() {
                try { ImageIcon i = get();
                    if (i != null) SwingUtilities.invokeLater(() -> { tl.setIcon(i); tl.setText(null); tl.setToolTipText("Enc Preview(" + fp + ")"); });
                    else SwingUtilities.invokeLater(() -> { tl.setIcon(null); tl.setText("[Enc Preview Error]"); tl.setToolTipText("Failed."); });
                } catch (Exception e) { logStatus("Enc Wrkr Err:" + e.getMessage()); SwingUtilities.invokeLater(() -> { tl.setIcon(null); tl.setText("[Enc Preview Error]"); tl.setToolTipText("Worker fail."); }); e.printStackTrace(); }
            }
        };
        w.execute();
    }

    // --- Input Validation Listener Setup ---
    private final DocumentListener inputValidationListener = new DocumentListener() {
        @Override public void insertUpdate(DocumentEvent e) { validateInputs(); }
        @Override public void removeUpdate(DocumentEvent e) { validateInputs(); }
        @Override public void changedUpdate(DocumentEvent e) { validateInputs(); } // Usually not needed for text
    };

    private void validateInputs() {
        boolean inputOk = !inputFileField.getText().trim().isEmpty();
        boolean outputOk = !outputFileField.getText().trim().isEmpty();
        boolean passwordOk = passwordField.getPassword().length > 0;
        if (executeButton != null) {
            executeButton.setEnabled(inputOk && outputOk && passwordOk);
        }
    }

    private void setupInputValidationListener() {
        inputFileField.getDocument().addDocumentListener(inputValidationListener);
        outputFileField.getDocument().addDocumentListener(inputValidationListener);
        passwordField.getDocument().addDocumentListener(inputValidationListener);

        inputFileField.getDocument().addDocumentListener(new DocumentListener() {
            private Timer previewTimer;
            private void updatePreviewAndSuggest() {
                if (previewTimer != null && previewTimer.isRunning()) {
                    previewTimer.stop();
                }
                previewTimer = new Timer(600, (ActionEvent event) -> {
                    loadImageForPreview(inputFileField.getText().trim(), beforePreviewLabel);
                });
                previewTimer.setRepeats(false);
                previewTimer.start();
                suggestOutput();
            }
            @Override public void insertUpdate(DocumentEvent e) { updatePreviewAndSuggest(); }
            @Override public void removeUpdate(DocumentEvent e) { updatePreviewAndSuggest(); }
            @Override public void changedUpdate(DocumentEvent e) {}
        });
    }


    // --- Action Listeners Setup ---
    private void setupActionListeners() {
        inputBrowseButton.addActionListener((ActionEvent e) -> {
            browseForFile(inputFileField, false);
        });
        outputBrowseButton.addActionListener((ActionEvent e) -> browseForFile(outputFileField, true));
        ActionListener modeListener = (ActionEvent e) -> {
            updateUiForMode(); suggestOutput(); if (encryptRadioButton.isSelected()) clearAfterPreviewLabel(PREVIEW_PLACEHOLDER); validateInputs();
        };
        encryptRadioButton.addActionListener(modeListener);
        decryptRadioButton.addActionListener(modeListener);
        showPasswordCheckBox.addActionListener((ActionEvent e) -> passwordField.setEchoChar(showPasswordCheckBox.isSelected() ? '\0' : '*'));
        executeButton.addActionListener((ActionEvent e) -> { clearAfterPreviewLabel("Processing..."); performOperation(); });
        resetButton.addActionListener((ActionEvent e) -> { logStatus("Resetting..."); resetGuiFields(); });
        themeCheckBox.addActionListener((ActionEvent e) -> switchTheme(themeCheckBox.isSelected()));
    }

    // --- UI Update / Theme / Suggestion Helpers ---
    private void switchTheme(boolean useDark) {
        isDarkMode = useDark;
        try {
            UIManager.setLookAndFeel(useDark ? new FlatDarkLaf() : new FlatLightLaf());
            logStatus("Theme Switched.");
            SwingUtilities.updateComponentTreeUI(this);
            if (fileChooser != null) SwingUtilities.updateComponentTreeUI(fileChooser);
            footerLabel.setForeground(useDark ? Color.LIGHT_GRAY : Color.GRAY);
            initializeButtonHoverEffects();
        } catch (Exception ex) {
            logStatus("Theme Error: " + ex.getMessage()); ex.printStackTrace();
        }
    }

    private void suggestOutput() {
        if (!outputFileField.getText().trim().isEmpty()) { return; }
        String inputPath = inputFileField.getText().trim(); if (inputPath.isEmpty()) { return; }
        try { File inputFile = new File(inputPath); if (!inputFile.isFile()) { return; }
            String inputDir = inputFile.getParent(); String inputName = inputFile.getName(); String baseName = inputName; String ext = "";
            int dotIndex = inputName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < inputName.length() - 1) { baseName = inputName.substring(0, dotIndex); ext = inputName.substring(dotIndex); }
            String suggestedName = encryptRadioButton.isSelected()
                    ? baseName + ext + ".enc" : (inputName.toLowerCase().endsWith(".enc")
                    ? inputName.substring(0, inputName.length() - 4) : baseName + "_decrypted" + ext);
            String suggestedPath = (inputDir != null) ? Paths.get(inputDir, suggestedName).toString() : suggestedName;
            if (!suggestedPath.equalsIgnoreCase(inputPath)) { SwingUtilities.invokeLater(()->outputFileField.setText(suggestedPath));}
        } catch (InvalidPathException e){ System.err.println("Path Suggest Err:"+e.getMessage()); } catch (Exception e){ System.err.println("Path Suggest Err (Other):"+e.getMessage());}
    }

    private void updateUiForMode() { executeButton.setText(encryptRadioButton.isSelected() ? "Encrypt File" : "Decrypt File"); }

    private void browseForFile(JTextField tf, boolean isSave) {
        fileChooser.setDialogTitle(isSave ? "Save Output As" : "Open Input File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File(""));
        String cp = tf.getText().trim();
        if (!cp.isEmpty()) {
            try {
                File cf = new File(cp);
                if (cf.isFile()){
                    fileChooser.setSelectedFile(cf);
                    fileChooser.setCurrentDirectory(cf.getParentFile());
                } else if (cf.isDirectory()) {
                    fileChooser.setCurrentDirectory(cf);
                } else {
                    File p = cf.getParentFile();
                    if (p != null && p.isDirectory()) fileChooser.setCurrentDirectory(p);
                }
            } catch (Exception ex) {}
        }
        int r = isSave ? fileChooser.showSaveDialog(this) : fileChooser.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File sf = fileChooser.getSelectedFile();
            tf.setText(sf.getAbsolutePath());
            logStatus("Selected: " + sf.getAbsolutePath());
        }
        else {
            logStatus("File selection cancelled.");
        }
    }

    // --- Log/Clear/Set UI Enabled ---
    private void logStatus(String message){
        SwingUtilities.invokeLater(()->{ statusTextArea.append(message + "\n");
            statusTextArea.setCaretPosition(statusTextArea.getDocument().getLength());
        });
    }
    private void clearLog(){ SwingUtilities.invokeLater(() -> statusTextArea.setText("")); }
    private void setUiEnabled(boolean enabled) {
        SwingUtilities.invokeLater(()->{
            Component[] comps = { encryptRadioButton, decryptRadioButton, inputFileField, outputFileField, passwordField,
                    showPasswordCheckBox, inputBrowseButton, outputBrowseButton, executeButton,
                    resetButton, themeCheckBox, deleteInputCheckBox };
            for (Component c : comps) { c.setEnabled(enabled); }
            if (enabled) { validateInputs(); } else { executeButton.setEnabled(false); }
            progressBar.setVisible(!enabled);
            progressBar.setIndeterminate(!enabled);
            progressBar.setString(enabled ? "" : "Processing...");
        });
    }

    // --- Reset GUI Fields and State ---
    private void resetGuiFields() {
        SwingUtilities.invokeLater(() -> {
            inputFileField.setText("");
            outputFileField.setText("");
            passwordField.setText("");
            showPasswordCheckBox.setSelected(false);
            passwordField.setEchoChar('*');
            deleteInputCheckBox.setSelected(false);
            encryptRadioButton.setSelected(true);
            updateUiForMode();
            clearLog();
            clearPreviewLabels();
            statusTextArea.append("--- Ready ---" + "\n");
            progressBar.setValue(0);
            progressBar.setVisible(false);
            validateInputs();
        });
    }

    // --- Perform Operation (Main Crypto Logic in Worker + Optional Deletion) ---
    private void performOperation() {
        clearLog(); clearAfterPreviewLabel("Processing...");

        final String inputFile = inputFileField.getText().trim();
        final String outputFile = outputFileField.getText().trim();
        final char[] password = passwordField.getPassword().clone();
        final boolean encryptMode = encryptRadioButton.isSelected();
        final boolean deleteInput = deleteInputCheckBox.isSelected();

        // --- Re-validate just before starting (safety check) ---
        if (inputFile.isEmpty() || outputFile.isEmpty() || password.length == 0 || inputFile.equalsIgnoreCase(outputFile)) {
            JOptionPane.showMessageDialog(this, "Invalid inputs detected. Please check paths and password.", "Input Error", JOptionPane.ERROR_MESSAGE);
            logStatus("Invalid inputs before starting.");
            if (password != null) Arrays.fill(password, ' ');
            clearAfterPreviewLabel(PREVIEW_PLACEHOLDER);
            setUiEnabled(true);
        }

        // --- Confirm Overwrite ---
        File outFile = new File(outputFile);
        if (outFile.exists()) {
            int c = JOptionPane.showConfirmDialog(this, "Output file '" + outFile.getName() + "' exists. Overwrite?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c != JOptionPane.YES_OPTION) {
                logStatus("Cancelled (overwrite).");
                if (password != null) Arrays.fill(password,' ');
                clearAfterPreviewLabel(PREVIEW_PLACEHOLDER); setUiEnabled(true);
            }
        }

        // --- Confirm Deletion (if selected) ---
        if (deleteInput) {
            int dc = JOptionPane.showConfirmDialog(this, "WARNING: Delete Original is checked.\nFile: " + inputFile + "\nPERMANENTLY DELETED on success.\nProceed?", "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (dc != JOptionPane.YES_OPTION) {
                logStatus("Cancelled (del confirm).");
                if (password != null) Arrays.fill(password,' ');
                clearAfterPreviewLabel(PREVIEW_PLACEHOLDER);
                setUiEnabled(true);
            }
            logStatus("User confirmed deletion.");
        }

        // --- Execute crypto in Background ---
        setUiEnabled(false);
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Starting...");
                try {
                    if (encryptMode) CryptoManager.encryptFile(inputFile, outputFile, password);
                    else CryptoManager.decryptFile(inputFile, outputFile, password);
                    publish("Completed.");
                }
                catch (Exception e) {
                    if (!encryptMode&&(e instanceof BadPaddingException||e instanceof AEADBadTagException)) throw new RuntimeException("Decrypt Fail: Bad Pwd/Data.",e);
                    else if(e instanceof FileNotFoundException) throw new RuntimeException("Error: Input missing.",e);
                    else if(e instanceof IOException&&e.getMessage()!=null&&(e.getMessage().contains("large")||e.getMessage().contains("space")||e.getMessage().contains("read")||e.getMessage().contains("write"))) throw new RuntimeException("Error: File processing fail.",e);
                    else if(e instanceof GeneralSecurityException) throw new RuntimeException("Error: Security setup fail.",e);
                    else throw new RuntimeException("Error:\n"+e.getMessage(),e);
                }
                finally {
                    Arrays.fill(password, ' ');
                }
                return null;
            }

            @Override protected void process(List<String> chunks) {
                for (String msg:chunks) logStatus(msg);
            }

            @Override protected void done() {
                boolean success = false;
                try {
                    get(); success = true;
                    logStatus("OK.");
                    JOptionPane.showMessageDialog(PixelVaultGui.this, (encryptMode ? "Enc" : "Dec") + "ryption OK!", "Done", JOptionPane.INFORMATION_MESSAGE);
                    if (encryptMode) displayEncryptedPreview(outputFile, afterPreviewLabel);
                    else loadImageForPreview(outputFile, afterPreviewLabel);

                    if (success && deleteInput) {
                        publish("Deleting original...");
                        try { Files.delete(Paths.get(inputFile));
                            logStatus("DELETED original:" + inputFile);
                            SwingUtilities.invokeLater(()->{beforePreviewLabel.setIcon(null);
                                beforePreviewLabel.setText("[Deleted]");
                                beforePreviewLabel.setToolTipText("Original file deleted.");
                            });
                        }
                        catch (NoSuchFileException e) { logStatus("Warn: Original already gone:" + inputFile); }
                        catch (IOException | SecurityException e) { logStatus("ERR: FAILED delete:" + e.getMessage());
                            JOptionPane.showMessageDialog(PixelVaultGui.this, "Could not delete original:\n" + e.getMessage(), "Deletion Warning", JOptionPane.WARNING_MESSAGE);
                        }
                    } else if (success) {
                        logStatus("Original kept.");
                    }

                } catch (Exception e) { // Handle get() or other done() exceptions
                    String t = "Error", m = "Op Fail.";
                    if(e instanceof InterruptedException){
                        t = "Int";
                        m = "Interrupted.";
                        logStatus(m);
                        Thread.currentThread().interrupt();
                    } else if(e instanceof ExecutionException){
                        Throwable c = e.getCause() != null ? e.getCause() : e;
                        m = "Operation Failed:\n" + c.getMessage();
                        logStatus("Error:" + c.getMessage());
                        System.err.println("BG Err:");
                        c.printStackTrace(System.err);
                    } else {
                        m = "Unexpected:" + e.getMessage();
                        logStatus(m); System.err.println("GUI Err:");
                        e.printStackTrace(System.err);
                    }
                    JOptionPane.showMessageDialog(PixelVaultGui.this, m, t, JOptionPane.ERROR_MESSAGE);
                    clearAfterPreviewLabel("[Error]");
                } finally {
                    setUiEnabled(true); progressBar.setVisible(false);
                }
            }
        };
        worker.execute();
    }
}