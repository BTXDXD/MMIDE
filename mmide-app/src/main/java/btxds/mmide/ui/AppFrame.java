package btxds.mmide.ui;

import btxds.mmide.api.models.Workspace;
import btxds.mmide.plugins.PluginGuard;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class AppFrame extends JFrame {

    private final JPanel centerArea = new JPanel(new BorderLayout());
    private Workspace openedWorkspace;

    public AppFrame() {
        setTitle("Minecraft Modding IDE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);

        initTitleBar();
        initEditorLayout();
    }

    private void initTitleBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(null);

        JMenu menuWorkspace = new JMenu("Workspace");

        JMenuItem itemNew = new JMenuItem("Create new Workspace");
        JMenuItem itemOpen = new JMenuItem("Open Workspace");
        JMenuItem itemOpenFromJar = new JMenuItem("Open Workspace from .jar");
        JMenuItem itemSave = new JMenuItem("Save Workspace");

        itemNew.setIcon(UIManager.getIcon("FileChooser.newFolderIcon"));
        itemOpen.setIcon(UIManager.getIcon("FileView.directoryIcon"));
        itemOpenFromJar.setIcon(UIManager.getIcon("FileView.fileIcon"));
        itemSave.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));

        itemNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        itemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        itemOpenFromJar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        itemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        itemNew.addActionListener(e -> {
            if (!PluginGuard.canCreateWorkspace(this)) return;

            NewWorkspaceDialog dialog = new NewWorkspaceDialog(this, true);
            dialog.setVisible(true);

            Workspace ws = dialog.getCreatedWorkspace();
            if (ws != null) {
                this.openedWorkspace = ws;
                setTitle("Minecraft Modding IDE - " + ws.modName);

                initEditorLayout();
                revalidate();
                repaint();
            }
        });

        menuWorkspace.add(itemNew);
        menuWorkspace.add(itemOpen);
        menuWorkspace.add(itemOpenFromJar);
        menuWorkspace.add(itemSave);

        menuBar.add(menuWorkspace);
        menuBar.add(Box.createHorizontalGlue());

        setJMenuBar(menuBar);
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
                    + "</table>"
                    + "</html>");

            hintLabel.putClientProperty(FlatClientProperties.STYLE, "font: +2");
            centerPanel.add(hintLabel);

            setContentPane(centerPanel);
            return;
        }

        JPanel leftSidebar = new JPanel(new BorderLayout());
        JPanel centerArea = new JPanel(new BorderLayout());
        JPanel rightSidebar = new JPanel(new BorderLayout());

        leftSidebar.setMinimumSize(new Dimension(150, 0));
        centerArea.setMinimumSize(new Dimension(300, 0));
        rightSidebar.setMinimumSize(new Dimension(150, 0));

        centerArea.putClientProperty(FlatClientProperties.STYLE, "background: darken(@background, 17%)");

        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerArea, rightSidebar);
        styleSplitPane(rightSplitPane);

        rightSplitPane.setResizeWeight(1.0);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSidebar, rightSplitPane);
        styleSplitPane(mainSplitPane);

        mainSplitPane.setResizeWeight(0.0);

        mainSplitPane.setDividerLocation(220);
        rightSplitPane.setDividerLocation(getWidth() - 220 - 200);

        setContentPane(mainSplitPane);
    }

    private void styleSplitPane(JSplitPane splitPane) {
        splitPane.setBorder(null);
        splitPane.setDividerSize(2);
    }

}
