package btxds.mmide.api.models;

import btxds.mmide.api.Plugin;

import java.util.List;

public class Workspace {

    // Cache
    public String path;
    public Plugin loader;

    // Mod
    public String modName;
    public String modID;
    public String modVersion;
    public String modDescription;
    public String modAuthor;
    public String modCredits;
    public String modLicense;
    public String modIcon;

    // Workspace
    public List<Plugin> dependencies;
    public boolean allowSourceExtraction;
    public String loaderID;

}
