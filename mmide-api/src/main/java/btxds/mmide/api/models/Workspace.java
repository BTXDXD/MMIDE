package btxds.mmide.api.models;

import java.util.ArrayList;
import java.util.List;

public class Workspace {

    // Cache
    public transient String path;

    // Mod Metadata
    public String modName;
    public String modID;
    public String modVersion;
    public String modDescription;
    public String modAuthor;
    public String modCredits;
    public String modLicense;
    public String modIcon;

    // Workspace & Plugins
    public String loaderID;
    public List<String> dependencies = new ArrayList<>();
    public boolean allowSourceExtraction = true;

}