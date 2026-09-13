package btxds.mmide.model;

import btxds.mmide.api.BlockDefinition;
import btxds.mmide.api.PinDefinition;
import btxds.mmide.api.PinDirection;

import java.util.ArrayList;
import java.util.List;

public class Node {

    private final String id;
    private final String typeId;
    private String title;
    private String groupId;

    private final List<Pin> inputs = new ArrayList<>();
    private final List<Pin> outputs = new ArrayList<>();

    public double x;
    public double y;

    public Node(String id, String typeId, String title) {
        this.id = id;
        this.typeId = typeId;
        this.title = title;
    }

    public static Node fromDefinition(String instanceId, BlockDefinition def, double x, double y) {
        Node node = new Node(instanceId, def.getId(), def.getDisplayName());
        node.x = x;
        node.y = y;

        for (PinDefinition p : def.getInputs())
            node.inputs.add(new Pin(p.id(), p.title(), p.direction(), p.dataType()));
        for (PinDefinition p : def.getOutputs())
            node.outputs.add(new Pin(p.id(), p.title(), p.direction(), p.dataType()));

        return node;
    }

    public void addInput(Pin pin) {
        if (pin.getDirection() != PinDirection.INPUT)
            throw new IllegalArgumentException("Pin must be INPUT");
        inputs.add(pin);
    }

    public void addOutput(Pin pin) {
        if (pin.getDirection() != PinDirection.OUTPUT)
            throw new IllegalArgumentException("Pin must be OUTPUT");
        outputs.add(pin);
    }

    public Pin findPin(String pinId) {
        for (Pin p : inputs) if (p.getId().equals(pinId)) return p;
        for (Pin p : outputs) if (p.getId().equals(pinId)) return p;
        return null;
    }

    public String getId() { return id; }
    public String getTypeId() { return typeId; }
    public String getTitle() { return title; }
    public List<Pin> getInputs() { return inputs; }
    public List<Pin> getOutputs() { return outputs; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

}