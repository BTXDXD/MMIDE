package btxds.mmide.api;

public record PinDefinition(
        String id,
        String title,
        DataType dataType,
        PinDirection direction,
        String defaultValue
) {

    public static PinDefinition execIn(String id, String title) {
        return new PinDefinition(id, title, DataType.FLOW, PinDirection.INPUT, null);
    }

    public static PinDefinition execOut(String id, String title) {
        return new PinDefinition(id, title, DataType.FLOW, PinDirection.OUTPUT, null);
    }

    public static PinDefinition dataIn(String id, String title, DataType type) {
        return new PinDefinition(id, title, type, PinDirection.INPUT, null);
    }

    public static PinDefinition dataOut(String id, String title, DataType type) {
        return new PinDefinition(id, title, type, PinDirection.OUTPUT, null);
    }

}