package btxds.mmide.model;

import btxds.mmide.api.DataType;

import java.util.*;

public class NodeGraph {

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, NodeGroup> groups = new LinkedHashMap<>();
    private final Set<Connection> connections = new HashSet<>();

    public void addNode(Node node) {
        nodes.put(node.getId(), node);
    }

    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
        connections.removeIf(conn ->
                conn.getSourceNodeId().equals(nodeId) || conn.getTargetNodeId().equals(nodeId));
        for (NodeGroup g : groups.values()) g.removeNode(nodeId);
    }

    public boolean canConnect(String fromNodeId, String fromPinId, String toNodeId, String toPinId) {
        if (fromNodeId.equals(toNodeId)) return false;

        Node fromNode = nodes.get(fromNodeId);
        Node toNode = nodes.get(toNodeId);
        if (fromNode == null || toNode == null) return false;

        Pin fromPin = fromNode.findPin(fromPinId);
        Pin toPin = toNode.findPin(toPinId);
        if (fromPin == null || toPin == null) return false;

        return fromPin.canConnectTo(toPin);
    }

    public boolean connect(String fromNodeId, String fromPinId, String toNodeId, String toPinId) {
        if (!canConnect(fromNodeId, fromPinId, toNodeId, toPinId)) return false;

        Node toNode = nodes.get(toNodeId);
        Pin toPin = toNode.findPin(toPinId);
        if (toPin.getDataType() != DataType.FLOW)
            connections.removeIf(c -> c.getTargetNodeId().equals(toNodeId) && c.getTargetPinId().equals(toPinId));

        connections.add(new Connection(fromNodeId, fromPinId, toNodeId, toPinId));
        return true;
    }

    public void disconnect(Connection connection) {
        connections.remove(connection);
    }

    public Map<String, Node> getNodes() { return nodes; }
    public Map<String, NodeGroup> getGroups() { return groups; }
    public Set<Connection> getConnections() { return connections; }

}