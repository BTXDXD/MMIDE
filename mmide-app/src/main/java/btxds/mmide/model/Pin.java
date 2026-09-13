package btxds.mmide.model;

import btxds.mmide.api.DataType;
import btxds.mmide.api.PinDirection;

public class Pin {

    private final String id;
    private final String title;
    private final PinDirection direction;
    private final DataType dataType;

    public Pin(String id, String title, PinDirection direction, DataType dataType) {
        this.id = id;
        this.title = title;
        this.direction = direction;
        this.dataType = dataType;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public PinDirection getDirection() { return direction; }
    public DataType getDataType() { return dataType; }

    public boolean canConnectTo(Pin target) {
        if (this.direction == target.direction) return false;

        if (this.dataType == DataType.FLOW || target.dataType == DataType.FLOW)
            return this.dataType == target.dataType;

        return this.dataType == target.dataType
                || this.dataType == DataType.ANY
                || target.dataType == DataType.ANY;
    }

}