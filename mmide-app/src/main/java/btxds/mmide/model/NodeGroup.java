package btxds.mmide.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class NodeGroup {

    private final String id;
    private String title;
    public double x, y, width, height;
    public Color color = new Color(60, 60, 60, 100);

    private final List<String> nodeIds = new ArrayList<>();

    public NodeGroup(String id, String title, double x, double y, double width, double height) {
        this.id = id;
        this.title = title;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<String> getNodeIds() { return nodeIds; }

    public void addNode(String nodeId) {
        if (!nodeIds.contains(nodeId)) nodeIds.add(nodeId);
    }

    public void removeNode(String nodeId) {
        nodeIds.remove(nodeId);
    }

}