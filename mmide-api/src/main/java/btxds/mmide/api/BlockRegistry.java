package btxds.mmide.api;

import java.util.Collection;

public interface BlockRegistry {

    void register(BlockDefinition definition);
    BlockDefinition get(String id);
    Collection<BlockDefinition> getAll();

}