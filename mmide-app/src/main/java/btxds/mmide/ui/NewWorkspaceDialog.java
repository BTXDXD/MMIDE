package btxds.mmide.ui;

import btxds.mmide.api.models.Workspace;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class NewWorkspaceDialog extends JDialog {

    private final JTextField nameField = new JTextField();
    private final JTextField idField = new JTextField();
    private final JTextField pathField = new JTextField();
    private final JTextField versionField = new JTextField("1.0.0");
    private final JTextField authorField = new JTextField(System.getProperty("user.name"));

    private final JComboBox<String> loaderCombo = new JComboBox<>(new String[]{
            "Fabric (1.21.1)",
            "NeoForge (1.21.1)",
            "Forge (1.20.1)",
            "Forge (1.8.9)"
    });

    private final JCheckBox allowExtractionCheck = new JCheckBox("Allow source extraction from compiled JAR", true);

    // Флаг, чтобы знать, редактировал ли пользователь modID вручную
    private boolean isIdManuallyEdited = false;

    // Результат создания
    private Workspace createdWorkspace = null;

    public NewWorkspaceDialog(JFrame parent, boolean modal) {
        super(parent, modal);
        setTitle("New Workspace");
        setSize(540, 480);
        setLocationRelativeTo(parent);
        setResizable(false);

        initUI();
        initAutoModId();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 15));
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // --- Центральная форма ---
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 4, 5, 4);

        int row = 0;

        // 1. Mod Name
        nameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "My Mod");
        addFormRow(form, gbc, row++, "Mod Name:", nameField);

        // 2. Mod ID
        idField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "mymod");
        addFormRow(form, gbc, row++, "Mod ID:", idField);

        // 3. Location (Path + Browse Button)
        pathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Select directory...");
        JPanel pathPanel = new JPanel(new BorderLayout(6, 0));
        JButton btnBrowse = new JButton("Browse...");
        btnBrowse.addActionListener(e -> chooseDirectory());
        pathPanel.add(pathField, BorderLayout.CENTER);
        pathPanel.add(btnBrowse, BorderLayout.EAST);
        addFormRow(form, gbc, row++, "Location:", pathPanel);

        // 4. Loader / Version
        addFormRow(form, gbc, row++, "Loader:", loaderCombo);

        // 5. Mod Version
        addFormRow(form, gbc, row++, "Version:", versionField);

        // 6. Author
        addFormRow(form, gbc, row++, "Author:", authorField);

        // 7. Checkbox "Allow Source Extraction"
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 4, 5, 4);
        form.add(allowExtractionCheck, gbc);

        root.add(form, BorderLayout.CENTER);

        // --- Нижняя панель кнопок ---
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        JButton btnCreate = new JButton("Create");
        // Акцентная кнопка темы
        btnCreate.putClientProperty(FlatClientProperties.STYLE, "" +
                "background: $Component.accentColor;" +
                "foreground: #ffffff;" +
                "font: bold;" +
                "arc: 6");
        btnCreate.addActionListener(e -> onCreate());

        buttonsPanel.add(btnCancel);
        buttonsPanel.add(btnCreate);

        root.add(buttonsPanel, BorderLayout.SOUTH);
        setContentPane(root);
    }

    // Вспомогательный метод для добавления строк "Метка -> Поле"
    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent component) {
        gbc.gridwidth = 1;
        gbc.gridy = row;

        gbc.gridx = 0;
        gbc.weightx = 0.25;
        JLabel label = new JLabel(labelText);
        label.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.75;
        panel.add(component, gbc);
    }

    // Умная генерация modID на лету при вводе названия мода (как в MCreator / IntelliJ)
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

        // Если пользователь сам начал менять modID — больше не перезаписываем его
        idField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { check(); }
            public void removeUpdate(DocumentEvent e) { check(); }
            public void changedUpdate(DocumentEvent e) { check(); }

            private void check() {
                if (idField.isFocusOwner()) {
                    isIdManuallyEdited = true;
                }
            }
        });
    }

    // Выбор папки
    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Workspace Location");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // Создание Workspace
    private void onCreate() {
        String name = nameField.getText().trim();
        String id = idField.getText().trim();
        String path = pathField.getText().trim();

        if (name.isEmpty()) {
            showError("Mod Name cannot be empty!");
            return;
        }

        // В майнкрафте modID строго a-z, 0-9 и подчёркивание
        if (id.isEmpty() || !id.matches("^[a-z0-9_]+$")) {
            showError("Mod ID must contain only lowercase letters (a-z), numbers and underscores (_)!");
            return;
        }

        if (path.isEmpty()) {
            showError("Please select a location directory!");
            return;
        }

        // Собираем объект Workspace
        createdWorkspace = new Workspace();
        createdWorkspace.modName = name;
        createdWorkspace.modID = id;
        createdWorkspace.modVersion = versionField.getText().trim().isEmpty() ? "1.0.0" : versionField.getText().trim();
        createdWorkspace.modAuthor = authorField.getText().trim();
        createdWorkspace.allowSourceExtraction = allowExtractionCheck.isSelected();
        createdWorkspace.loaderID = (String) loaderCombo.getSelectedItem();
        createdWorkspace.dependencies = new ArrayList<>();

        createdWorkspace.path = path + File.separator + id;

        dispose();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    public Workspace getCreatedWorkspace() {
        return createdWorkspace;
    }
}