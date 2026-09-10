package btxds.mmide.ui.editor;

import btxds.mmide.api.Block;
import btxds.mmide.api.models.EditorBlock;
import btxds.mmide.api.models.Workspace;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class GraphEditor extends JComponent {

    private final Map<Long, EditorBlock> blocks = new LinkedHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private boolean isDirty = false;

    private double zoom = 1.0;
    private int panX = 40;
    private int panY = 40;
    private int dragStartX, dragStartY;

    private EditorBlock selectedBlock;
    private EditorBlock hoveredBlock;
    private int blockDragOffsetX, blockDragOffsetY;

    private EditorBlock connectingBlock;
    private int mouseScreenX, mouseScreenY;

    public GraphEditor() {
        setOpaque(true);
        setBackground(new Color(24, 24, 24));

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseScreenX = e.getX();
                mouseScreenY = e.getY();
                int worldX = toWorldX(e.getX());
                int worldY = toWorldY(e.getY());

                if (SwingUtilities.isRightMouseButton(e)) {
                    for (EditorBlock b : getBlocksReversed()) {
                        if (b.hasOutput && isInsideCircle(worldX, worldY, b.x + b.width, b.y + b.height / 2, 12)) {
                            b.nextBlockIds.clear();
                            disconnectAllArgumentsFrom(b.id);
                            isDirty = true;
                            repaint();
                            return;
                        }

                        if (b.hasInput && isInsideCircle(worldX, worldY, b.x, b.y + b.height / 2, 12)) {
                            disconnectAllIncomingTo(b.id);
                            isDirty = true;
                            repaint();
                            return;
                        }

                        int paramIdx = getParamPinIndex(b, worldX, worldY);
                        if (paramIdx != -1) {
                            String paramName = b.getAllParams().get(paramIdx);
                            b.paramConnections.remove(paramName);
                            isDirty = true;
                            repaint();
                            return;
                        }

                        if (worldX >= b.x && worldX <= b.x + b.width && worldY >= b.y && worldY <= b.y + b.height) {
                            showBlockContextMenu(b, e.getX(), e.getY());
                            return;
                        }
                    }

                    dragStartX = e.getX() - panX;
                    dragStartY = e.getY() - panY;
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        for (EditorBlock b : getBlocksReversed()) {
                            if (worldX >= b.x && worldX <= b.x + b.width && worldY >= b.y && worldY <= b.y + b.height) {
                                editBlockValue(b);
                                return;
                            }
                        }
                    }

                    for (EditorBlock b : getBlocksReversed()) {
                        if (b.hasOutput && isInsideCircle(worldX, worldY, b.x + b.width, b.y + b.height / 2, 10)) {
                            connectingBlock = b;
                            return;
                        }

                        if (worldX >= b.x && worldX <= b.x + b.width && worldY >= b.y && worldY <= b.y + b.height) {
                            selectedBlock = b;
                            blockDragOffsetX = worldX - b.x;
                            blockDragOffsetY = worldY - b.y;
                            repaint();
                            return;
                        }
                    }

                    selectedBlock = null;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && connectingBlock != null) {
                    int worldX = toWorldX(e.getX());
                    int worldY = toWorldY(e.getY());

                    for (EditorBlock target : getBlocksReversed()) {
                        if (target.id == connectingBlock.id) continue;

                        if (target.hasInput && isInsideCircle(worldX, worldY, target.x, target.y + target.height / 2, 14)) {
                            if (canConnect(connectingBlock, target) && !connectingBlock.nextBlockIds.contains(target.id)) {
                                connectingBlock.nextBlockIds.add(target.id);
                                isDirty = true;
                            }
                            break;
                        }

                        int paramIdx = getParamPinIndex(target, worldX, worldY);
                        if (paramIdx != -1) {
                            if (canConnect(connectingBlock, target)) {
                                String paramName = target.getAllParams().get(paramIdx);
                                target.paramConnections.put(paramName, connectingBlock.id);
                                isDirty = true;
                            }
                            break;
                        }
                    }

                    connectingBlock = null;
                    repaint();
                }

                if (selectedBlock != null) {
                    selectedBlock = null;
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseScreenX = e.getX();
                mouseScreenY = e.getY();

                if (SwingUtilities.isRightMouseButton(e)) {
                    panX = e.getX() - dragStartX;
                    panY = e.getY() - dragStartY;
                    repaint();
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (selectedBlock != null) {
                        selectedBlock.x = toWorldX(e.getX()) - blockDragOffsetX;
                        selectedBlock.y = toWorldY(e.getY()) - blockDragOffsetY;
                        isDirty = true;
                        repaint();
                    } else if (connectingBlock != null) {
                        repaint();
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mouseScreenX = e.getX();
                mouseScreenY = e.getY();
                int worldX = toWorldX(e.getX());
                int worldY = toWorldY(e.getY());

                EditorBlock prevHovered = hoveredBlock;
                hoveredBlock = null;

                for (EditorBlock b : getBlocksReversed()) {
                    if (worldX >= b.x && worldX <= b.x + b.width && worldY >= b.y && worldY <= b.y + b.height) {
                        hoveredBlock = b;
                        break;
                    }
                }

                if (prevHovered != hoveredBlock) repaint();
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        addMouseWheelListener(e -> {
            double oldZoom = zoom;
            if (e.getPreciseWheelRotation() < 0) {
                zoom = Math.min(2.5, zoom * 1.12);
            } else {
                zoom = Math.max(0.35, zoom / 1.12);
            }

            panX = (int) (e.getX() - (e.getX() - panX) * (zoom / oldZoom));
            panY = (int) (e.getY() - (e.getY() - panY) * (zoom / oldZoom));
            repaint();
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteBlock");
        getActionMap().put("deleteBlock", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hoveredBlock != null) deleteBlock(hoveredBlock.id);
            }
        });
    }

    public void initWorkspaceGraph(Workspace ws) {
        blocks.clear();
        File codeFile = new File(ws.path, "code.json");

        if (codeFile.exists()) {
            loadFromCodeJson(new File(ws.path));
        }

        boolean hasRoot = blocks.values().stream().anyMatch(b -> b.isRoot);
        if (!hasRoot) {
            EditorBlock root = new EditorBlock(1L, "root_mod", ws.modName, ws.modID, 60, 60, List.of(), List.of(), List.of());
            root.isRoot = true;
            root.hasInput = false;
            root.hasOutput = true;
            blocks.put(root.id, root);
            isDirty = true;
        }

        repaint();
    }

    public boolean canConnect(EditorBlock source, EditorBlock target) {
        if (source == null || target == null || source.id == target.id) return false;
        if (source.allowedTargets.isEmpty()) return true;
        return source.allowedTargets.contains(target.typeId);
    }

    public void addBlock(Block block) {
        long newId = System.currentTimeMillis() + new Random().nextInt(1000);
        int spawnX = toWorldX(getWidth() / 2) - 80;
        int spawnY = toWorldY(getHeight() / 2) - 30;

        EditorBlock eb = new EditorBlock(
                newId,
                block.getId(),
                block.getDisplayName(),
                block.getCategory(),
                spawnX,
                spawnY,
                block.getRequiredParams(),
                block.getOptionalParams(),
                block.getAllowedTargets()
        );

        eb.hasInput = block.hasInput();
        eb.hasOutput = block.hasOutput();

        blocks.put(newId, eb);
        isDirty = true;
        repaint();
    }

    public void deleteBlock(long id) {
        EditorBlock b = blocks.get(id);
        if (b != null && b.isRoot) return;

        blocks.remove(id);
        for (EditorBlock other : blocks.values()) {
            other.nextBlockIds.remove(id);
            other.paramConnections.values().removeIf(val -> val.equals(id));
        }
        hoveredBlock = null;
        isDirty = true;
        repaint();
    }

    public void disconnectAllIncomingTo(long targetId) {
        for (EditorBlock b : blocks.values()) b.nextBlockIds.remove(targetId);
    }

    public void disconnectAllArgumentsFrom(long sourceId) {
        for (EditorBlock b : blocks.values()) {
            b.paramConnections.values().removeIf(val -> val.equals(sourceId));
        }
    }

    private void showBlockContextMenu(EditorBlock b, int screenX, int screenY) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem editItem = new JMenuItem("Edit Value");
        editItem.addActionListener(e -> editBlockValue(b));
        menu.add(editItem);

        JMenuItem disconnectItem = new JMenuItem("Disconnect All");
        disconnectItem.addActionListener(e -> {
            b.nextBlockIds.clear();
            b.paramConnections.clear();
            disconnectAllIncomingTo(b.id);
            disconnectAllArgumentsFrom(b.id);
            isDirty = true;
            repaint();
        });
        menu.add(disconnectItem);

        if (!b.isRoot) {
            menu.addSeparator();
            JMenuItem deleteItem = new JMenuItem("Delete Block");
            deleteItem.addActionListener(e -> deleteBlock(b.id));
            menu.add(deleteItem);
        }

        menu.show(this, screenX, screenY);
    }

    private void editBlockValue(EditorBlock b) {
        String val = JOptionPane.showInputDialog(this, "Enter value for " + b.name + ":", b.customValue);
        if (val != null) {
            b.customValue = val.trim();
            isDirty = true;
            repaint();
        }
    }

    public void saveToCodeJson(File workspaceDir) {
        File codeFile = new File(workspaceDir, "code.json");
        try (Writer writer = Files.newBufferedWriter(codeFile.toPath())) {
            gson.toJson(blocks.values(), writer);
            isDirty = false;
            System.out.println("[GraphEditor] Saved " + blocks.size() + " blocks to " + codeFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadFromCodeJson(File workspaceDir) {
        blocks.clear();
        File codeFile = new File(workspaceDir, "code.json");
        if (!codeFile.exists()) {
            repaint();
            return;
        }

        try (Reader reader = Files.newBufferedReader(codeFile.toPath())) {
            Type listType = new TypeToken<List<EditorBlock>>() {}.getType();
            List<EditorBlock> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                for (EditorBlock b : loaded) blocks.put(b.id, b);
            }
            isDirty = false;
            System.out.println("[GraphEditor] Loaded " + blocks.size() + " blocks from " + codeFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        repaint();
    }

    public boolean isDirty() {
        return isDirty;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(24, 24, 24));
        g2.fillRect(0, 0, getWidth(), getHeight());

        drawGrid(g2);

        AffineTransform defaultTx = g2.getTransform();
        g2.translate(panX, panY);
        g2.scale(zoom, zoom);

        for (EditorBlock b : blocks.values()) {
            if (b.hasOutput) {
                for (Long targetId : b.nextBlockIds) {
                    EditorBlock target = blocks.get(targetId);
                    if (target != null && target.hasInput) {
                        drawOrthogonalToLeftPin(g2, b.x + b.width, b.y + b.height / 2, target.x, target.y + target.height / 2, new Color(200, 200, 200));
                    }
                }
            }

            List<String> allParams = b.getAllParams();
            int total = allParams.size();

            for (Map.Entry<String, Long> entry : b.paramConnections.entrySet()) {
                String paramName = entry.getKey();
                Long providerId = entry.getValue();
                EditorBlock provider = blocks.get(providerId);

                if (provider != null && provider.hasOutput) {
                    int idx = allParams.indexOf(paramName);
                    if (idx != -1) {
                        int pinX = b.x + (b.width * (idx + 1)) / (total + 1);
                        int pinY = b.y + b.height;
                        drawOrthogonalToBottomPin(g2, provider.x + provider.width, provider.y + provider.height / 2, pinX, pinY, new Color(255, 200, 50));
                    }
                }
            }
        }

        if (connectingBlock != null) {
            int startX = connectingBlock.x + connectingBlock.width;
            int startY = connectingBlock.y + connectingBlock.height / 2;
            int targetWorldX = toWorldX(mouseScreenX);
            int targetWorldY = toWorldY(mouseScreenY);

            Color wireColor = Color.ORANGE;
            boolean isOverBottomPin = false;

            for (EditorBlock target : blocks.values()) {
                if (target.id == connectingBlock.id) continue;

                if (target.hasInput && isInsideCircle(targetWorldX, targetWorldY, target.x, target.y + target.height / 2, 14)) {
                    wireColor = canConnect(connectingBlock, target) ? new Color(100, 255, 100) : new Color(255, 80, 80);
                    targetWorldX = target.x;
                    targetWorldY = target.y + target.height / 2;
                    break;
                }

                int paramIdx = getParamPinIndex(target, targetWorldX, targetWorldY);
                if (paramIdx != -1) {
                    int total = target.getAllParams().size();
                    wireColor = canConnect(connectingBlock, target) ? new Color(100, 255, 100) : new Color(255, 80, 80);
                    targetWorldX = target.x + (target.width * (paramIdx + 1)) / (total + 1);
                    targetWorldY = target.y + target.height;
                    isOverBottomPin = true;
                    break;
                }
            }

            if (isOverBottomPin) {
                drawOrthogonalToBottomPin(g2, startX, startY, targetWorldX, targetWorldY, wireColor);
            } else {
                drawOrthogonalToLeftPin(g2, startX, startY, targetWorldX, targetWorldY, wireColor);
            }
        }

        for (EditorBlock b : blocks.values()) drawBlockCard(g2, b);

        g2.setTransform(defaultTx);

        if (hoveredBlock != null) {
            drawParamTooltip(g2, hoveredBlock, mouseScreenX + 12, mouseScreenY + 12);
        }
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(34, 34, 34));
        int baseGrid = 24;
        int scaledGrid = (int) Math.max(8, baseGrid * zoom);

        int startX = panX % scaledGrid;
        int startY = panY % scaledGrid;

        for (int x = startX; x < getWidth(); x += scaledGrid) g2.drawLine(x, 0, x, getHeight());
        for (int y = startY; y < getHeight(); y += scaledGrid) g2.drawLine(0, y, getWidth(), y);
    }

    private void drawBlockCard(Graphics2D g2, EditorBlock b) {
        g2.setColor(b.isRoot ? new Color(42, 38, 28) : new Color(36, 36, 36));
        g2.fillRect(b.x, b.y, b.width, b.height);

        Color border = b.isRoot ? new Color(255, 195, 0) : (b == hoveredBlock ? Color.WHITE : new Color(140, 140, 140));
        g2.setColor(border);
        g2.setStroke(new BasicStroke(b == hoveredBlock || b.isRoot ? 2f : 1.5f));
        g2.drawRect(b.x, b.y, b.width, b.height);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 13));
        String title = b.name + (b.customValue.isEmpty() ? "" : ": " + b.customValue);
        g2.drawString(title, b.x + 12, b.y + 24);

        g2.setColor(new Color(170, 170, 170));
        g2.setFont(new Font("Consolas", Font.PLAIN, 10));
        g2.drawString(b.category, b.x + 12, b.y + 40);

        if (b.hasInput) drawPin(g2, b.x, b.y + b.height / 2);
        if (b.hasOutput) drawPin(g2, b.x + b.width, b.y + b.height / 2);

        if (!b.isRoot) {
            List<String> allParams = b.getAllParams();
            int total = allParams.size();

            for (int i = 0; i < total; i++) {
                int px = b.x + (b.width * (i + 1)) / (total + 1);
                int py = b.y + b.height;
                String paramName = allParams.get(i);
                boolean isConnected = b.paramConnections.containsKey(paramName);

                drawParamPin(g2, px, py, isConnected, b.requiredParams.contains(paramName));
            }
        }
    }

    private void drawPin(Graphics2D g2, int cx, int cy) {
        int r = 4;
        g2.setColor(Color.BLACK);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);
    }

    private void drawParamPin(Graphics2D g2, int cx, int cy, boolean isConnected, boolean isRequired) {
        int r = 4;
        g2.setColor(isConnected ? new Color(255, 200, 50) : Color.BLACK);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);

        g2.setColor(isRequired ? new Color(255, 120, 120) : Color.WHITE);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);
    }

    private void drawOrthogonalToLeftPin(Graphics2D g2, int x1, int y1, int x2, int y2, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));

        if (x2 <= x1 + 16) {
            int offset = 22;
            g2.drawLine(x1, y1, x1 + offset, y1);
            g2.drawLine(x1 + offset, y1, x1 + offset, y2);
            g2.drawLine(x1 + offset, y2, x2, y2);
        } else {
            int midX = x1 + (x2 - x1) / 2;
            g2.drawLine(x1, y1, midX, y1);
            g2.drawLine(midX, y1, midX, y2);
            g2.drawLine(midX, y2, x2, y2);
        }

        int sq = 6;
        g2.fillRect(x2 - sq, y2 - sq / 2, sq, sq);
    }

    private void drawOrthogonalToBottomPin(Graphics2D g2, int x1, int y1, int x2, int y2, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));

        int midX = x1 + 20;
        int bottomY = Math.max(y1, y2) + 24;

        g2.drawLine(x1, y1, midX, y1);
        g2.drawLine(midX, y1, midX, bottomY);
        g2.drawLine(midX, bottomY, x2, bottomY);
        g2.drawLine(x2, bottomY, x2, y2);

        int sq = 6;
        g2.fillRect(x2 - sq / 2, y2, sq, sq);
    }

    private void drawParamTooltip(Graphics2D g2, EditorBlock b, int tx, int ty) {
        int w = 210;
        int h = 78;

        g2.setColor(new Color(20, 20, 20, 235));
        g2.fillRect(tx, ty, w, h);
        g2.setColor(new Color(160, 160, 160));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(tx, ty, w, h);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.drawString(b.name + " Params:", tx + 8, ty + 16);

        g2.setFont(new Font("Consolas", Font.PLAIN, 10));
        g2.setColor(new Color(255, 120, 120));
        g2.drawString("req: " + (b.requiredParams.isEmpty() ? "none" : String.join(", ", b.requiredParams)), tx + 8, ty + 36);

        g2.setColor(new Color(130, 220, 130));
        g2.drawString("opt: " + (b.optionalParams.isEmpty() ? "none" : String.join(", ", b.optionalParams)), tx + 8, ty + 52);

        g2.setColor(new Color(170, 170, 255));
        g2.drawString("allow: " + (b.allowedTargets.isEmpty() ? "any" : String.join(", ", b.allowedTargets)), tx + 8, ty + 68);
    }

    private int getParamPinIndex(EditorBlock b, int worldX, int worldY) {
        if (b.isRoot) return -1;
        List<String> allParams = b.getAllParams();
        int total = allParams.size();

        for (int i = 0; i < total; i++) {
            int px = b.x + (b.width * (i + 1)) / (total + 1);
            int py = b.y + b.height;
            if (isInsideCircle(worldX, worldY, px, py, 9)) return i;
        }

        return -1;
    }

    private int toWorldX(int screenX) {
        return (int) ((screenX - panX) / zoom);
    }

    private int toWorldY(int screenY) {
        return (int) ((screenY - panY) / zoom);
    }

    private List<EditorBlock> getBlocksReversed() {
        List<EditorBlock> list = new ArrayList<>(blocks.values());
        Collections.reverse(list);
        return list;
    }

    private boolean isInsideCircle(int px, int py, int cx, int cy, int r) {
        return (px - cx) * (px - cx) + (py - cy) * (py - cy) <= r * r;
    }

}