package btxds.mmide.model;

import java.util.Objects;

public class Connection {

    private final String sourceNodeId;
    private final String sourcePinId;
    private final String targetNodeId;
    private final String targetPinId;

    public Connection(String sourceNodeId, String sourcePinId, String targetNodeId, String targetPinId) {
        this.sourceNodeId = sourceNodeId;
        this.sourcePinId = sourcePinId;
        this.targetNodeId = targetNodeId;
        this.targetPinId = targetPinId;
    }

    public String getSourceNodeId() { return sourceNodeId; }
    public String getSourcePinId() { return sourcePinId; }
    public String getTargetNodeId() { return targetNodeId; }
    public String getTargetPinId() { return targetPinId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Connection that)) return false;
        return Objects.equals(sourceNodeId, that.sourceNodeId) &&
                Objects.equals(sourcePinId, that.sourcePinId) &&
                Objects.equals(targetNodeId, that.targetNodeId) &&
                Objects.equals(targetPinId, that.targetPinId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceNodeId, sourcePinId, targetNodeId, targetPinId);
    }

}