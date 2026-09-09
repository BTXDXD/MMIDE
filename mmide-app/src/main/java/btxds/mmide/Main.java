package btxds.mmide;

import btxds.mmide.engine.Engine;
import btxds.mmide.ui.AppFrame;

import javax.swing.*;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;

public class Main {

    public static void main(String[] args) {
        Engine.loadPlugins();

        FlatMaterialDarkerIJTheme.setup();
        SwingUtilities.invokeLater(() -> {
            new AppFrame().setVisible(true);
        });
    }

}