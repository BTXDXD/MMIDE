package btxds.mmide.plugins;

import btxds.mmide.api.Plugin;
import btxds.mmide.engine.Engine;
import javax.swing.*;
import java.awt.*;

public class PluginGuard {

    public static boolean hasLoaderPlugin() {
        for (Class<?> clazz : Engine.registeredPlugins) {
            Plugin plugin = clazz.getAnnotation(Plugin.class);
            if (plugin != null && plugin.isLoader()) return true;
        }
        return false;
    }

    public static boolean canCreateWorkspace(Component parent) {
        if (!hasLoaderPlugin()) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Cannot create workspace!\n\n" +
                            "No Loader Plugins found in 'plugins' directory.\n" +
                            "Please put at least one loader plugin (.jar) into 'plugins' folder.",
                    "Missing Loader Plugin",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }
        return true;
    }

}