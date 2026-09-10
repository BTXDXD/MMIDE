package btxds.mmide.api;

import java.util.Collection;

public interface BlockRegistry {

    void register(Block block);
    Block getBlock(String id);
    Collection<Block> getAllBlocks();

}