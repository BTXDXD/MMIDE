package btxds.mmide.api;

public interface IPlugin {

    void onLoad(PluginContext context);
    default void onUnload() {}

}