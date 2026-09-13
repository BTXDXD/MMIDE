package btxds.mmide.api;

public enum DataType {
    FLOW,

    STRING,     // java.lang.String
    INTEGER,    // int / Integer
    DOUBLE,     // double / float
    BOOLEAN,    // boolean

    COMPONENT,  // net.minecraft.network.chat.Component
    ITEM,       // net.minecraft.world.item.Item
    BLOCK,      // net.minecraft.world.level.block.Block
    ENTITY,     // net.minecraft.world.entity.Entity

    ANY;

    public String getDefaultValue() {
        return switch (this) {
            case INTEGER -> "0";
            case DOUBLE  -> "0.0";
            case BOOLEAN -> "false";
            default      -> "";
        };
    }

}