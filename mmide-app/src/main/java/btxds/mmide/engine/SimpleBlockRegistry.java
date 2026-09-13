package btxds.mmide.engine;

import btxds.mmide.api.BlockDefinition;
import btxds.mmide.api.BlockRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SimpleBlockRegistry implements BlockRegistry {

    private static final Pattern BLOCK_ID_PATTERN = Pattern.compile("^[a-z0-9_]+$");
    private final Map<String, BlockDefinition> blocks = new LinkedHashMap<>();

    @Override
    public void register(BlockDefinition block) {
        if (block == null) {
            throw new IllegalArgumentException("BlockDefinition cannot be null!");
        }

        String id = block.getId();
        if (id == null || id.trim().isEmpty() || !BLOCK_ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "Invalid Block ID: '" + id + "' for block '" + block.getDisplayName() + "'.\n" +
                            "Block ID must contain only lowercase latin letters (a-z), numbers (0-9) and underscores (_)."
            );
        }

        if (blocks.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Duplicate Block ID: '" + id + "'. A block with this ID is already registered!"
            );
        }

        blocks.put(id, block);
    }

    @Override
    public BlockDefinition get(String id) {
        return blocks.get(id);
    }

    @Override
    public Collection<BlockDefinition> getAll() {
        return blocks.values();
    }

}