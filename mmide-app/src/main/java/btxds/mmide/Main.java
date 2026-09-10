package btxds.mmide;

import btxds.mmide.engine.Engine;
import btxds.mmide.engine.SettingsManager;
import btxds.mmide.ui.AppFrame;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        FlatMaterialDarkerIJTheme.setup();

        boolean success = Engine.loadPlugins();
        if (!success) System.exit(1);

        SettingsManager.load();
        SwingUtilities.invokeLater(() -> {
            new AppFrame().setVisible(true);
        });
    }

}