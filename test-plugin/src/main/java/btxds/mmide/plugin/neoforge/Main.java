package btxds.mmide.plugin.neoforge;

import btxds.mmide.api.*;
import btxds.mmide.api.models.EditorBlock;
import btxds.mmide.api.models.Workspace;

@Plugin(
        id = "btxds_neoforge-1-21-1",
        name = "NeoForge 1.21.1 (by btxdxd)",
        isLoader = true
)
public class Main implements LoaderPlugin {

    private PluginContext context;

    @Override public void onLoad(PluginContext context) { this.context = context; }
    @Override public void onUnload() {}
    @Override public String getMinecraftVersion() { return "1.21.1"; }
    @Override public String getLoaderName() { return "NeoForge"; }

    @Override
    public void registerBlocks(BlockRegistry registry) {

        registry.register(BlockDefinition.builder("string_val", "String")
                .category("Java Types")
                .description("Plain Java String text.")
                .editable(DataType.STRING)
                .addDataOutput("out", "String Out", DataType.STRING)
                .build()
        );

        registry.register(BlockDefinition.builder("int_val", "Integer")
                .category("Java Types")
                .description("Whole integer number.")
                .editable(DataType.INTEGER)
                .addDataOutput("out", "Int Out", DataType.INTEGER)
                .build()
        );

        registry.register(BlockDefinition.builder("double_val", "Double")
                .category("Java Types")
                .description("Decimal floating point number.")
                .editable(DataType.DOUBLE)
                .addDataOutput("out", "Double Out", DataType.DOUBLE)
                .build()
        );

        registry.register(BlockDefinition.builder("bool_val", "Boolean")
                .category("Java Types")
                .description("Boolean toggle: true or false.")
                .editable(DataType.BOOLEAN)
                .addDataOutput("out", "Bool Out", DataType.BOOLEAN)
                .build()
        );

        registry.register(BlockDefinition.builder("translatable_component", "Translatable Text")
                .category("Components")
                .description("Minecraft translatable text component from lang file")
                .addDataInput("key", "Translation Key", DataType.STRING)
                .addDataOutput("out", "Component", DataType.COMPONENT)
                .build()
        );

        registry.register(BlockDefinition.builder("literal_component", "Literal Text")
                .category("Components")
                .description("Raw unlocalized text component")
                .addDataInput("text", "Raw Text", DataType.STRING)
                .addDataOutput("out", "Component", DataType.COMPONENT)
                .build()
        );

        registry.register(BlockDefinition.builder("new_item", "New Item")
                .category("Registry")
                .description("Registers a new custom item in the game.")
                .addExecInput("exec", "Register")
                .addDataInput("id", "Item ID", DataType.STRING)
                .addDataInput("name", "Name (Component)", DataType.COMPONENT)
                .addDataInput("max_stack", "Max Stack", DataType.INTEGER)
                .addDataInput("fire_resistant", "Fire Resistant", DataType.BOOLEAN)
                .build()
        );

    }

    @Override public void buildMod(BuildContext ctx) {}
    @Override public void launchGame(Workspace workspace) {}
    @Override public void hotReload(Workspace workspace, EditorBlock changedBlock) {}

}