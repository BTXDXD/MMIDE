package btxds.mmide.api;

import btxds.mmide.api.models.EditorBlock;
import btxds.mmide.api.models.Workspace;

import java.io.File;
import java.util.List;

public interface BuildContext {

    Workspace getWorkspace();
    File getProjectDirectory();
    File getOutputDirectory();
    File getResourcesDirectory();

    List<EditorBlock> getBlocks();

    void log(String message);
    void logError(String message, Throwable error);
    void setProgress(int percent, String stageName);

}