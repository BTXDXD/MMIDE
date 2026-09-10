package btxds.mmide.api;

import java.io.File;

public interface PluginContext {

    String getPluginId();
    void log(String message);
    void logError(String message, Throwable throwable);
    File getDataFolder();

}