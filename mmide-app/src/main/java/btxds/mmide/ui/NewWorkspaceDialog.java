package btxds.mmide.ui;

import btxds.mmide.api.Plugin;
import btxds.mmide.api.models.Workspace;
import btxds.mmide.engine.Engine;
import com.formdev.flatlaf.FlatClientProperties;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class NewWorkspaceDialog extends JDialog {

    private final JTextField nameField = new JTextField();
    private final JTextField idField = new JTextField();
    private final JTextField versionField = new JTextField("1.0.0");
    private final JTextField authorField = new JTextField(System.getProperty("user.name"));
    private final JComboBox<String> licenseCombo = new JComboBox<>(new String[]{
            "All Rights Reserved",
            "MIT",
            "Apache-2.0",
            "BSD-2-Clause",
            "BSD-3-Clause",
            "ISC",
            "Unlicense",
            "WTFPL",
            "GPL-2.0-only",
            "GPL-3.0-only",
            "AGPL-3.0-only",
            "LGPL-2.1-only",
            "LGPL-3.0-only",
            "MPL-2.0",
            "EPL-2.0",
            "CC0-1.0 (Public Domain)",
            "CC-BY-4.0",
            "CC-BY-SA-4.0",
            "CC-BY-NC-4.0"
    });
    private final JComboBox<LoaderItem> loaderCombo = new JComboBox<>();
    private final JTextField pathField = new JTextField();

    private final JLabel iconPreview = new JLabel();
    private File selectedIconFile = null;
    private final JTextArea descriptionArea = new JTextArea();
    private final JTextField creditsField = new JTextField();

    private final JCheckBox allowExtractionCheck = new JCheckBox("Allow source extraction from compiled JAR", true);

    private boolean isIdManuallyEdited = false;
    private Workspace createdWorkspace = null;

    public record LoaderItem(String id, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    public NewWorkspaceDialog(JFrame parent, boolean modal) {
        super(parent, modal);
        setTitle("New Workspace");
        setSize(820, 520);
        setLocationRelativeTo(parent);
        setResizable(false);

        initLoadersList();
        initUI();
        initAutoModId();
    }

    private void initLoadersList() {
        loaderCombo.removeAllItems();
        for (Class<?> clazz : Engine.registeredPlugins) {
            Plugin plugin = clazz.getAnnotation(Plugin.class);
            if (plugin != null && plugin.isLoader()) {
                String name = plugin.name().isEmpty() ? plugin.id() : plugin.name();
                loaderCombo.addItem(new LoaderItem(plugin.id(), name));
            }
        }
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JPanel columnsContainer = new JPanel(new GridLayout(1, 2, 28, 0));

        JPanel leftColumn = new JPanel(new GridBagLayout());
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.fill = GridBagConstraints.HORIZONTAL;
        gbcL.insets = new Insets(5, 0, 5, 0);

        int leftRow = 0;
        nameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "My Mod");
        addFormRow(leftColumn, gbcL, leftRow++, "Mod Name:", nameField);

        idField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "mymod");
        addFormRow(leftColumn, gbcL, leftRow++, "Mod ID:", idField);

        addFormRow(leftColumn, gbcL, leftRow++, "Version:", versionField);
        addFormRow(leftColumn, gbcL, leftRow++, "Author:", authorField);
        addFormRow(leftColumn, gbcL, leftRow++, "License:", licenseCombo);
        addFormRow(leftColumn, gbcL, leftRow++, "Loader:", loaderCombo);

        pathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Select directory...");
        JPanel pathPanel = new JPanel(new BorderLayout(6, 0));
        JButton btnBrowse = new JButton("Browse...");
        btnBrowse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBrowse.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        btnBrowse.addActionListener(e -> chooseDirectory());
        pathPanel.add(pathField, BorderLayout.CENTER);
        pathPanel.add(btnBrowse, BorderLayout.EAST);
        addFormRow(leftColumn, gbcL, leftRow++, "Location:", pathPanel);

        gbcL.gridy = leftRow;
        gbcL.weighty = 1.0;
        leftColumn.add(Box.createVerticalGlue(), gbcL);

        columnsContainer.add(leftColumn);

        JPanel rightColumn = new JPanel(new BorderLayout(0, 10));

        JPanel iconSection = new JPanel(new BorderLayout(12, 0));
        iconSection.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        iconPreview.setPreferredSize(new Dimension(72, 72));
        iconPreview.setMaximumSize(new Dimension(72, 72));
        iconPreview.setMinimumSize(new Dimension(72, 72));
        iconPreview.setHorizontalAlignment(SwingConstants.CENTER);
        iconPreview.putClientProperty(FlatClientProperties.STYLE, "" +
                "border: 1,1,1,1,$Component.borderColor,,8;" +
                "background: $Panel.background;" +
                "arc: 8;");
        iconPreview.setText("512×512");
        iconPreview.setForeground(UIManager.getColor("Label.disabledForeground"));

        JPanel iconInfoBox = new JPanel();
        iconInfoBox.setLayout(new BoxLayout(iconInfoBox, BoxLayout.Y_AXIS));
        iconInfoBox.setOpaque(false);

        JLabel iconLabel = new JLabel("Mod Icon");
        iconLabel.putClientProperty(FlatClientProperties.STYLE, "font: bold; foreground: $Label.foreground");
        iconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconHint = new JLabel("Recommended: 512×512 PNG");
        iconHint.putClientProperty(FlatClientProperties.STYLE, "font: -1; foreground: $Label.disabledForeground");
        iconHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnChooseIcon = new JButton("Choose Icon...");
        btnChooseIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnChooseIcon.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        btnChooseIcon.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnChooseIcon.addActionListener(e -> chooseIcon());

        iconInfoBox.add(Box.createVerticalStrut(2));
        iconInfoBox.add(iconLabel);
        iconInfoBox.add(iconHint);
        iconInfoBox.add(Box.createVerticalStrut(6));
        iconInfoBox.add(btnChooseIcon);

        iconSection.add(iconPreview, BorderLayout.WEST);
        iconSection.add(iconInfoBox, BorderLayout.CENTER);

        JPanel descBox = new JPanel(new BorderLayout(0, 4));
        JLabel descLabel = new JLabel("Description:");
        descLabel.putClientProperty(FlatClientProperties.STYLE, "font: bold; foreground: $Label.foreground");

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Detailed description of your mod...");

        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.putClientProperty(FlatClientProperties.STYLE, "arc: 6");

        descBox.add(descLabel, BorderLayout.NORTH);
        descBox.add(descScroll, BorderLayout.CENTER);

        JPanel creditsBox = new JPanel(new BorderLayout(0, 4));
        JLabel creditsLabel = new JLabel("Credits:");
        creditsLabel.putClientProperty(FlatClientProperties.STYLE, "font: bold; foreground: $Label.foreground");
        creditsField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Contributors, artists, supporters...");
        creditsBox.add(creditsLabel, BorderLayout.NORTH);
        creditsBox.add(creditsField, BorderLayout.CENTER);

        JPanel rightCenter = new JPanel(new BorderLayout(0, 8));
        rightCenter.add(descBox, BorderLayout.CENTER);
        rightCenter.add(creditsBox, BorderLayout.SOUTH);

        rightColumn.add(iconSection, BorderLayout.NORTH);
        rightColumn.add(rightCenter, BorderLayout.CENTER);

        columnsContainer.add(rightColumn);

        root.add(columnsContainer, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(allowExtractionCheck, BorderLayout.WEST);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        JButton btnCreate = new JButton("Create");
        btnCreate.putClientProperty(FlatClientProperties.STYLE, "" +
                "background: $Component.accentColor;" +
                "foreground: #ffffff;" +
                "font: bold;" +
                "arc: 6");
        btnCreate.addActionListener(e -> onCreate());

        buttonsPanel.add(btnCancel);
        buttonsPanel.add(btnCreate);

        bottomPanel.add(buttonsPanel, BorderLayout.EAST);
        root.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent component) {
        gbc.gridy = row;
        gbc.weighty = 0.0;

        gbc.gridx = 0;
        gbc.weightx = 0.28;
        JLabel label = new JLabel(labelText);
        label.putClientProperty(FlatClientProperties.STYLE, "font: bold; foreground: $Label.foreground");
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.72;
        panel.add(component, gbc);
    }

    private void chooseIcon() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Mod Icon (PNG)");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedIconFile = chooser.getSelectedFile();
            ImageIcon rawIcon = new ImageIcon(selectedIconFile.getAbsolutePath());
            Image scaled = rawIcon.getImage().getScaledInstance(68, 68, Image.SCALE_SMOOTH);
            iconPreview.setIcon(new ImageIcon(scaled));
            iconPreview.setText(null);
        }
    }

    private void initAutoModId() {
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }

            private void update() {
                if (!isIdManuallyEdited) {
                    String cleanId = nameField.getText().trim()
                            .toLowerCase()
                            .replaceAll("[^a-z0-9_]", "_")
                            .replaceAll("_{2,}", "_");
                    idField.setText(cleanId);
                }
            }
        });

        idField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { check(); }
            public void removeUpdate(DocumentEvent e) { check(); }
            public void changedUpdate(DocumentEvent e) { check(); }

            private void check() {
                if (idField.isFocusOwner()) isIdManuallyEdited = true;
            }
        });
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Workspace Location");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
    }

    private void onCreate() {
        String name = nameField.getText().trim();
        String id = idField.getText().trim();
        String path = pathField.getText().trim();

        if (name.isEmpty()) {
            showError("Mod Name cannot be empty!");
            return;
        }

        if (id.isEmpty() || !id.matches("^[a-z0-9_]+$")) {
            showError("Mod ID must contain only lowercase letters (a-z), numbers and underscores (_)!");
            return;
        }

        if (path.isEmpty()) {
            showError("Please select a location directory!");
            return;
        }

        LoaderItem selectedLoader = (LoaderItem) loaderCombo.getSelectedItem();
        if (selectedLoader == null) {
            showError("No Loader Plugin selected!");
            return;
        }

        File projectDir = new File(path, id);
        if (projectDir.exists()) {
            showError("A folder with this name already exists at the target location!");
            return;
        }

        createdWorkspace = new Workspace();
        createdWorkspace.modName = name;
        createdWorkspace.modID = id;
        createdWorkspace.modVersion = versionField.getText().trim().isEmpty() ? "1.0.0" : versionField.getText().trim();
        createdWorkspace.modAuthor = authorField.getText().trim();
        createdWorkspace.modLicense = (String) licenseCombo.getSelectedItem();
        createdWorkspace.modDescription = descriptionArea.getText().trim();
        createdWorkspace.modCredits = creditsField.getText().trim();
        createdWorkspace.allowSourceExtraction = allowExtractionCheck.isSelected();
        createdWorkspace.loaderID = selectedLoader.id();
        createdWorkspace.path = projectDir.getAbsolutePath();
        createdWorkspace.dependencies = new ArrayList<>();
        createdWorkspace.modIcon = "icon.png";

        try {
            projectDir.mkdirs();
            File resDir = new File(projectDir, "resources");
            resDir.mkdirs();

            File codeFile = new File(projectDir, "code.json");
            Files.writeString(codeFile.toPath(), "[]");

            if (selectedIconFile != null && selectedIconFile.exists()) {
                File targetIcon = new File(resDir, "icon.png");
                Files.copy(selectedIconFile.toPath(), targetIcon.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            File manifestFile = new File(projectDir, "workspace.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = Files.newBufferedWriter(manifestFile.toPath())) {
                gson.toJson(createdWorkspace, writer);
            }

            System.out.println("[Workspace] Successfully initialized at: " + projectDir.getAbsolutePath());
        } catch (IOException e) {
            showError("Failed to create project files: " + e.getMessage());
            return;
        }

        dispose();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    public Workspace getCreatedWorkspace() {
        return createdWorkspace;
    }

}