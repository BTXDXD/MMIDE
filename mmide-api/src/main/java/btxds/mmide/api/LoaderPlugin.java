package btxds.mmide.api;

import btxds.mmide.api.models.Workspace;

public interface LoaderPlugin extends IPlugin {

    String getMinecraftVersion();
    String getLoaderName();
    void buildMod(Workspace workspace);

}