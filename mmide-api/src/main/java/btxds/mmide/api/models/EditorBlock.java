package btxds.mmide.api.models;

import btxds.mmide.api.DataType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EditorBlock {

    public long id;
    public String typeId;
    public int x;
    public int y;
    public boolean isRoot = false;
    public String customValue = "";
    public List<Long> nextBlockIds = new ArrayList<>();
    public Map<String, Long> paramConnections = new LinkedHashMap<>();

    public transient String name = "";
    public transient String category = "";
    public transient int width = 170;
    public transient int height = 60;
    public transient boolean hasInput = false;
    public transient boolean hasOutput = false;
    public transient boolean editable = false;
    public transient DataType editableType = DataType.STRING;
    public transient DataType outputType = DataType.FLOW;
    public transient Map<String, DataType> paramTypes = new LinkedHashMap<>();
    public transient List<String> requiredParams = new ArrayList<>();

    public EditorBlock() {}

    public EditorBlock(long id, String typeId, int x, int y) {
        this.id = id;
        this.typeId = typeId;
        this.x = x;
        this.y = y;
    }

    public List<String> getAllParams() {
        return requiredParams;
    }

}