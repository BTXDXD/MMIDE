package btxds.mmide.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockDefinition {

    private final String id;
    private final String displayName;
    private final String description;
    private final String category;
    private final boolean editable;
    private final DataType editableType;
    private final List<PinDefinition> inputs;
    private final List<PinDefinition> outputs;

    private BlockDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.category = builder.category;
        this.editable = builder.editable;
        this.editableType = builder.editableType;
        this.inputs = Collections.unmodifiableList(builder.inputs);
        this.outputs = Collections.unmodifiableList(builder.outputs);
    }

    public static Builder builder(String id, String displayName) {
        return new Builder(id, displayName);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public boolean isEditable() { return editable; }
    public DataType getEditableType() { return editableType; }
    public List<PinDefinition> getInputs() { return inputs; }
    public List<PinDefinition> getOutputs() { return outputs; }

    public static class Builder {
        private final String id;
        private final String displayName;
        private String description = "";
        private String category = "General";
        private boolean editable = false;
        private DataType editableType = DataType.STRING;
        private final List<PinDefinition> inputs = new ArrayList<>();
        private final List<PinDefinition> outputs = new ArrayList<>();

        public Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder description(String desc) { this.description = desc; return this; }
        public Builder category(String cat) { this.category = cat; return this; }

        public Builder editable(DataType validationType) {
            this.editable = true;
            this.editableType = validationType;
            return this;
        }

        public Builder addExecInput(String id, String title) {
            inputs.add(PinDefinition.execIn(id, title));
            return this;
        }

        public Builder addExecOutput(String id, String title) {
            outputs.add(PinDefinition.execOut(id, title));
            return this;
        }

        public Builder addDataInput(String id, String title, DataType type) {
            inputs.add(PinDefinition.dataIn(id, title, type));
            return this;
        }

        public Builder addDataOutput(String id, String title, DataType type) {
            outputs.add(PinDefinition.dataOut(id, title, type));
            return this;
        }

        public BlockDefinition build() {
            return new BlockDefinition(this);
        }
    }

}