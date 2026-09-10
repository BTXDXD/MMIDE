package btxds.mmide.api;

import btxds.mmide.api.models.EditorBlock;
import btxds.mmide.api.models.Workspace;

public interface LoaderPlugin extends IPlugin {

    String getMinecraftVersion();
    String getLoaderName();

    void registerBlocks(BlockRegistry registry);

    void buildMod(BuildContext context);

    void launchGame(Workspace workspace);
    void hotReload(Workspace workspace, EditorBlock changedBlock);

}