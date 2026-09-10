package btxds.mmide.plugin.neoforge;

import btxds.mmide.api.*;
import btxds.mmide.api.models.EditorBlock;
import btxds.mmide.api.models.Workspace;

import java.util.Collections;
import java.util.List;

@Plugin(
        id = "btxds_neoforge-1-21-1",
        name = "NeoForge 1.21.1 (by btxdxd)",
        isLoader = true
)
public class Main implements LoaderPlugin {

    private PluginContext context;

    @Override
    public void onLoad(PluginContext context) {
        this.context = context;
        context.log("NeoForge 1.21.1 Loader Plugin successfully loaded!");
    }

    @Override
    public void onUnload() {
        if (context != null) context.log("NeoForge 1.21.1 Loader Plugin unloaded.");
    }

    @Override
    public String getMinecraftVersion() {
        return "1.21.1";
    }

    @Override
    public String getLoaderName() {
        return "NeoForge";
    }

    @Override
    public void registerBlocks(BlockRegistry registry) {
        // --- 1. РЕГИСТРАЦИЯ (REGISTRY) ---

        // Создание предмета (БЕЗ ВЫХОДА)
        registry.register(new BlockDef(
                "new_item", "New Item", "Creates a custom item. Accepts Minecraft Text Component as name.", "Registry",
                true, false,
                List.of("id", "name (Component)"),
                List.of("max_stack", "rarity", "food_properties"),
                Collections.emptyList()
        ));

        // Создание блока (БЕЗ ВЫХОДА)
        registry.register(new BlockDef(
                "new_block", "New Block", "Creates a custom block with models, collision and hardness.", "Registry",
                true, false,
                List.of("id", "name (Component)", "material"),
                List.of("hardness", "resistance", "light_level", "sound_type"),
                Collections.emptyList()
        ));

        // Создание кейбинда (С ВЫХОДОМ НА СОБЫТИЯ)
        registry.register(new BlockDef(
                "new_keybind", "New Keybind", "Registers a keybinding for players.", "Registry",
                true, true,
                List.of("key_code"),
                List.of("category"),
                List.of("on_press", "on_release")
        ));

        // --- 2. СОБЫТИЯ КЛАВИШ (KB EVENTS) ---

        registry.register(new BlockDef(
                "on_press", "On Press Event", "Triggered when the key is pressed down.", "KB Events",
                true, true,
                Collections.emptyList(),
                Collections.emptyList(),
                List.of("kill_action", "send_message")
        ));

        registry.register(new BlockDef(
                "on_release", "On Release Event", "Triggered when the key is released.", "KB Events",
                true, true,
                Collections.emptyList(),
                Collections.emptyList(),
                List.of("kill_action", "send_message")
        ));

        // --- 3. ДЕЙСТВИЯ (ACTIONS) ---

        registry.register(new BlockDef(
                "kill_action", "Kill Entity", "Kills the specified target entity.", "Actions",
                true, false,
                List.of("target (@s, @p)"),
                Collections.emptyList(),
                Collections.emptyList()
        ));

        registry.register(new BlockDef(
                "send_message", "Send Message", "Sends a chat message to target.", "Actions",
                true, false,
                List.of("target", "message (Component)"),
                Collections.emptyList(),
                Collections.emptyList()
        ));

        // --- 4. ТИПЫ ДАННЫХ И ЗНАЧЕНИЯ (TYPES) ---

        // Компонент текста Minecraft (для названий предметов, сообщений и лора)
        registry.register(new BlockDef(
                "mc_component", "Text Component", "Minecraft Rich Text Component with color and styling.", "Types",
                false, false,
                List.of("text"),
                List.of("color", "bold", "italic"),
                List.of("new_item", "new_block", "send_message")
        ));

        // Майнкрафтовский селектор (@s, @p, @a, @e)
        registry.register(new BlockDef(
                "target_selector", "Target Selector", "Minecraft target selector (@s, @p, @a, @e).", "Types",
                false, false,
                List.of("selector (@s/@p)"),
                Collections.emptyList(),
                List.of("kill_action", "send_message")
        ));

        // Обычная строка
        registry.register(new BlockDef(
                "string_val", "String Value", "Plain text value.", "Types",
                false, false,
                List.of("value"),
                Collections.emptyList(),
                Collections.emptyList()
        ));

        // Обычное число
        registry.register(new BlockDef(
                "number_val", "Number Value", "Integer or decimal number.", "Types",
                false, false,
                List.of("value"),
                Collections.emptyList(),
                Collections.emptyList()
        ));
    }

    @Override
    public void buildMod(BuildContext ctx) {
    }

    @Override
    public void launchGame(Workspace workspace) {
    }

    @Override
    public void hotReload(Workspace workspace, EditorBlock changedBlock) {
    }

    private record BlockDef(
            String id,
            String displayName,
            String description,
            String category,
            boolean hasInput,
            boolean hasOutput,
            List<String> requiredParams,
            List<String> optionalParams,
            List<String> allowedTargets
    ) implements Block {
        @Override public String getId() { return id; }
        @Override public String getDisplayName() { return displayName; }
        @Override public String getDescription() { return description; }
        @Override public String getCategory() { return category; }
        @Override public boolean hasInput() { return hasInput; }
        @Override public boolean hasOutput() { return hasOutput; }
        @Override public List<String> getRequiredParams() { return requiredParams; }
        @Override public List<String> getOptionalParams() { return optionalParams; }
        @Override public List<String> getAllowedTargets() { return allowedTargets; }
    }

}