package btxds.mmide.ui;

import btxds.mmide.api.Block;
import btxds.mmide.api.models.Workspace;
import btxds.mmide.engine.Engine;
import btxds.mmide.plugins.PluginGuard;
import btxds.mmide.ui.editor.GraphEditor;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppFrame extends JFrame {

    private final JPanel centerArea = new JPanel(new BorderLayout());
    private Workspace openedWorkspace;
    private final GraphEditor graphEditor = new GraphEditor();

    private JMenuItem itemSave;

    public AppFrame() {
        setTitle("Minecraft Modding IDE");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Контролируем закрытие сами!
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        initTitleBar();
        initEditorLayout();
    }

    private void handleExit() {
        if (openedWorkspace != null && graphEditor.isDirty()) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Save changes to workspace '" + openedWorkspace.modName + "' before exit?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                saveCurrentWorkspace();
                dispose();
                System.exit(0);
            } else if (result == JOptionPane.NO_OPTION) {
                dispose();
                System.exit(0);
            }
        } else {
            dispose();
            System.exit(0);
        }
    }

    private void initTitleBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(null);

        JMenu menuWorkspace = new JMenu("Workspace");

        JMenuItem itemNew = new JMenuItem("Create new Workspace");
        JMenuItem itemOpen = new JMenuItem("Open Workspace");
        JMenuItem itemOpenFromJar = new JMenuItem("Open Workspace from .jar");
        itemSave = new JMenuItem("Save Workspace");

        itemNew.setIcon(UIManager.getIcon("FileChooser.newFolderIcon"));
        itemOpen.setIcon(UIManager.getIcon("FileView.directoryIcon"));
        itemOpenFromJar.setIcon(UIManager.getIcon("FileView.fileIcon"));
        itemSave.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));

        itemNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        itemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        itemOpenFromJar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        itemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        itemSave.setEnabled(false);

        itemNew.addActionListener(e -> {
            if (!PluginGuard.canCreateWorkspace(this)) return;

            NewWorkspaceDialog dialog = new NewWorkspaceDialog(this, true);
            dialog.setVisible(true);

            Workspace ws = dialog.getCreatedWorkspace();
            if (ws != null) applyOpenedWorkspace(ws);
        });

        itemOpen.addActionListener(e -> openWorkspace());
        itemSave.addActionListener(e -> saveCurrentWorkspace());

        menuWorkspace.add(itemNew);
        menuWorkspace.add(itemOpen);
        menuWorkspace.add(itemOpenFromJar);
        menuWorkspace.addSeparator();
        menuWorkspace.add(itemSave);

        menuBar.add(menuWorkspace);
        menuBar.add(Box.createHorizontalGlue());

        setJMenuBar(menuBar);
    }

    private void openWorkspace() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Workspace");
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter(new FileNameExtensionFilter("MMIDE Workspace (workspace.json or directory)", "json"));
        chooser.setAcceptAllFileFilterUsed(true);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            File projectDir = selected.isDirectory() ? selected : selected.getParentFile();
            File manifestFile = new File(projectDir, "workspace.json");

            if (!manifestFile.exists()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Selected directory is not a valid MMIDE workspace!\nMissing 'workspace.json':\n" + manifestFile.getAbsolutePath(),
                        "Invalid Workspace",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            Gson gson = new Gson();
            try (Reader reader = Files.newBufferedReader(manifestFile.toPath())) {
                Workspace ws = gson.fromJson(reader, Workspace.class);
                if (ws == null) {
                    JOptionPane.showMessageDialog(this, "Failed to parse workspace.json!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ws.path = projectDir.getAbsolutePath();
                applyOpenedWorkspace(ws);
                System.out.println("[Workspace] Opened: " + ws.modName + " from " + ws.path);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to read workspace:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void saveCurrentWorkspace() {
        if (openedWorkspace == null || openedWorkspace.path == null) return;

        File projectDir = new File(openedWorkspace.path);
        File manifestFile = new File(projectDir, "workspace.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (Writer writer = Files.newBufferedWriter(manifestFile.toPath())) {
            gson.toJson(openedWorkspace, writer);
            graphEditor.saveToCodeJson(projectDir);
            System.out.println("[Workspace] Successfully saved to: " + manifestFile.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to save workspace:\n" + ex.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void applyOpenedWorkspace(Workspace ws) {
        this.openedWorkspace = ws;

        setTitle("Minecraft Modding IDE - " + ws.modName + " (" + ws.loaderID + ")");
        itemSave.setEnabled(true);

        graphEditor.initWorkspaceGraph(ws);

        initEditorLayout();
        revalidate();
        repaint();
    }

    private void initEditorLayout() {
        if (openedWorkspace == null) {
            JPanel centerPanel = new JPanel(new GridBagLayout());

            JLabel hintLabel = new JLabel("<html>"
                    + "<table cellpadding='3'>"
                    + "  <tr>"
                    + "    <td align='right' style='color: gray;'>Create Workspace</td>"
                    + "    <td align='left' style='padding-left: 15px;'><b>Ctrl + N</b></td>"
                    + "  </tr>"
                    + "  <tr>"
                    + "    <td align='right' style='color: gray;'>Open Workspace</td>"
                    + "    <td align='left' style='padding-left: 15px;'><b>Ctrl + O</b></td>"
                    + "  </tr>"
                    + "  <tr>"
                    + "    <td align='right' style='color: gray;'>Open Workspace from .jar</td>"
                    + "    <td align='left' style='padding-left: 15px;'><b>Ctrl + Shift + O</b></td>"
                    + "  </tr>"
                    + "</table>"
                    + "</html>");

            hintLabel.putClientProperty(FlatClientProperties.STYLE, "font: +2");
            centerPanel.add(hintLabel);

            setContentPane(centerPanel);
            return;
        }

        JPanel leftSidebar = new JPanel(new BorderLayout());
        leftSidebar.setMinimumSize(new Dimension(240, 0));
        leftSidebar.add(buildPresetsSidebar(), BorderLayout.CENTER);

        centerArea.setMinimumSize(new Dimension(400, 0));
        centerArea.putClientProperty(FlatClientProperties.STYLE, "background: darken(@background, 17%)");

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSidebar, centerArea);
        styleSplitPane(splitPane);
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerLocation(260);

        centerArea.removeAll();
        centerArea.add(graphEditor, BorderLayout.CENTER);

        setContentPane(splitPane);
    }

    private JPanel buildPresetsSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UIManager.getColor("Tree.background"));

        JPanel searchBox = new JPanel(new BorderLayout());
        searchBox.setOpaque(false);
        searchBox.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        JTextField searchField = new JTextField();
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search");
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        searchField.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");

        searchBox.add(searchField, BorderLayout.CENTER);
        sidebar.add(searchBox, BorderLayout.NORTH);

        JPanel listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);
        listContainer.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));

        List<btxds.mmide.api.Block> allBlocks = Engine.getBlocksForLoader(openedWorkspace.loaderID);

        Map<String, List<btxds.mmide.api.Block>> blocksByCategory = allBlocks.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getCategory() == null ? "General" : b.getCategory(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Runnable updateList = () -> {
            listContainer.removeAll();
            String query = searchField.getText().trim().toLowerCase();

            for (Map.Entry<String, List<btxds.mmide.api.Block>> entry : blocksByCategory.entrySet()) {
                String category = entry.getKey();
                List<btxds.mmide.api.Block> categoryBlocks = entry.getValue();

                List<btxds.mmide.api.Block> filtered = categoryBlocks.stream()
                        .filter(b -> query.isEmpty()
                                || b.getDisplayName().toLowerCase().contains(query)
                                || (b.getDescription() != null && b.getDescription().toLowerCase().contains(query))
                                || category.toLowerCase().contains(query))
                        .toList();

                if (!filtered.isEmpty()) {
                    JLabel categoryLabel = new JLabel(category.toUpperCase());
                    categoryLabel.putClientProperty(FlatClientProperties.STYLE, "" +
                            "font: bold -1;" +
                            "foreground: lighten($Label.disabledForeground, 30%);");
                    categoryLabel.setBorder(BorderFactory.createEmptyBorder(10, 4, 4, 4));
                    categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    listContainer.add(categoryLabel);

                    for (btxds.mmide.api.Block b : filtered) {
                        listContainer.add(createBlockButton(b));
                        listContainer.add(Box.createVerticalStrut(4));
                    }

                    listContainer.add(Box.createVerticalStrut(8));
                }
            }

            listContainer.add(Box.createVerticalGlue());
            listContainer.revalidate();
            listContainer.repaint();
        };

        updateList.run();

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateList.run(); }
            public void removeUpdate(DocumentEvent e) { updateList.run(); }
            public void changedUpdate(DocumentEvent e) { updateList.run(); }
        });

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);

        sidebar.add(scrollPane, BorderLayout.CENTER);
        return sidebar;
    }

    private JButton createBlockButton(btxds.mmide.api.Block block) {
        JButton button = new JButton(block.getDisplayName());
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(block.getDescription());

        button.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc: 8;" +
                "margin: 4, 12, 4, 12;" +
                "background: lighten(@background, 6%);" +
                "hoverBackground: lighten(@background, 10%);" +
                "foreground: $Label.foreground;" +
                "font: bold +0;");

        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        button.addActionListener(e -> graphEditor.addBlock(block));

        return button;
    }

    private JButton createPresetButton(Block preset) {
        JButton button = new JButton(preset.getDisplayName());
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(preset.getDescription());

        button.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc: 8;" +
                "margin: 4, 12, 4, 12;" +
                "background: lighten(@background, 6%);" +
                "hoverBackground: lighten(@background, 10%);" +
                "foreground: $Label.foreground;" +
                "font: bold +0;");

        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        button.addActionListener(e -> graphEditor.addBlock(preset));

        return button;
    }

    private void styleSplitPane(JSplitPane splitPane) {
        splitPane.setBorder(null);
        splitPane.setDividerSize(2);
    }

}