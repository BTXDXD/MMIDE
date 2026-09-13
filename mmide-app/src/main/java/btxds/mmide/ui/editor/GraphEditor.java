package btxds.mmide.ui.editor;

import btxds.mmide.api.BlockDefinition;
import btxds.mmide.api.DataType;
import btxds.mmide.api.models.EditorBlock;
import btxds.mmide.api.models.Workspace;
import btxds.mmide.engine.Engine;
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
    private static final Type BLOCKS_TYPE = new TypeToken<List<EditorBlock>>() {}.getType();

    private Workspace currentWorkspace;
    private boolean hadLoadIssues = false;
    private boolean isDirty = false;

    private static final int MAX_HISTORY = 256;
    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();
    private String dragStartSnapshot = null;
    private boolean draggedBlockMoved = false;

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
                            recordAction();
                            b.nextBlockIds.clear();
                            disconnectAllArgumentsFrom(b.id);
                            isDirty = true;
                            repaint();
                            return;
                        }

                        if (b.hasInput && isInsideCircle(worldX, worldY, b.x, b.y + b.height / 2, 12)) {
                            recordAction();
                            disconnectAllIncomingTo(b.id);
                            isDirty = true;
                            repaint();
                            return;
                        }

                        int paramIdx = getParamPinIndex(b, worldX, worldY);
                        if (paramIdx != -1) {
                            String paramName = b.getAllParams().get(paramIdx);
                            recordAction();
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
                    if (e.getClickCount() == 2)
                        for (EditorBlock b : getBlocksReversed())
                            if (worldX >= b.x && worldX <= b.x + b.width && worldY >= b.y && worldY <= b.y + b.height) {
                                if (b.editable) editBlockValue(b);
                                return;
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
                            dragStartSnapshot = takeSnapshotJson();
                            draggedBlockMoved = false;
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
                            if (canConnectFlow(connectingBlock, target) && !connectingBlock.nextBlockIds.contains(target.id)) {
                                recordAction();
                                connectingBlock.nextBlockIds.add(target.id);
                                isDirty = true;
                            }
                            break;
                        }

                        int paramIdx = getParamPinIndex(target, worldX, worldY);
                        if (paramIdx != -1) {
                            String paramName = target.getAllParams().get(paramIdx);
                            if (canConnectParam(connectingBlock, target, paramName)) {
                                recordAction();
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
                    if (draggedBlockMoved && dragStartSnapshot != null) {
                        pushUndo(dragStartSnapshot);
                        isDirty = true;
                    }
                    selectedBlock = null;
                    dragStartSnapshot = null;
                    draggedBlockMoved = false;
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
                        int targetWorldX = toWorldX(e.getX()) - blockDragOffsetX;
                        int targetWorldY = toWorldY(e.getY()) - blockDragOffsetY;

                        int dx = targetWorldX - selectedBlock.x;
                        int dy = targetWorldY - selectedBlock.y;

                        if (dx != 0 || dy != 0) {
                            draggedBlockMoved = true;

                            if (e.isShiftDown())
                                for (EditorBlock b : getAttachedBlocks(selectedBlock)) {
                                    b.x += dx;
                                    b.y += dy;
                                }
                            else {
                                selectedBlock.x = targetWorldX;
                                selectedBlock.y = targetWorldY;
                            }

                            isDirty = true;
                            repaint();
                        }
                    } else if (connectingBlock != null) repaint();
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

                for (EditorBlock b : getBlocksReversed())
                    if (worldX >= b.x && worldX <= b.x + b.width && worldY >= b.y && worldY <= b.y + b.height) {
                        hoveredBlock = b;
                        break;
                    }

                if (prevHovered != hoveredBlock) repaint();
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        addMouseWheelListener(e -> {
            double oldZoom = zoom;
            if (e.getPreciseWheelRotation() < 0) zoom = Math.min(2.5, zoom * 1.12);
            else zoom = Math.max(0.35, zoom / 1.12);
            panX = (int) (e.getX() - (e.getX() - panX) * (zoom / oldZoom));
            panY = (int) (e.getY() - (e.getY() - panY) * (zoom / oldZoom));
            repaint();
        });

        initKeyBindings();
    }

    private void initKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteBlock");
        am.put("deleteBlock", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EditorBlock target = hoveredBlock != null ? hoveredBlock : selectedBlock;
                if (target != null) requestDeleteBlock(target.id);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "editBlock");
        am.put("editBlock", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EditorBlock target = hoveredBlock != null ? hoveredBlock : selectedBlock;
                if (target != null && target.editable) editBlockValue(target);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        am.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "redo");
        am.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });
    }

    private void editBlockValue(EditorBlock b) {
        if (!b.editable) return;

        String input = JOptionPane.showInputDialog(this, "Enter value for " + b.name + " (" + b.editableType + "):", b.customValue);
        if (input == null) return;
        input = input.trim();

        String validated = null;
        switch (b.editableType) {
            case INTEGER -> {
                try {
                    validated = String.valueOf((long) Double.parseDouble(input));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid integer number: '" + input + "'", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            case DOUBLE -> {
                try {
                    validated = String.valueOf(Double.parseDouble(input));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid decimal number: '" + input + "'", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            case BOOLEAN -> {
                if ("true".equalsIgnoreCase(input) || "1".equals(input)) validated = "true";
                else if ("false".equalsIgnoreCase(input) || "0".equals(input)) validated = "false";
                else {
                    JOptionPane.showMessageDialog(this, "Boolean must be 'true' or 'false'!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            default -> validated = input;
        }

        if (validated != null && !validated.equals(b.customValue)) {
            recordAction();
            b.customValue = validated;
            isDirty = true;
            repaint();
        }
    }

    public void hydrateBlock(EditorBlock eb, BlockDefinition def) {
        eb.name = def.getDisplayName();
        eb.category = def.getCategory();
        eb.editable = def.isEditable();
        eb.editableType = def.getEditableType();

        if (eb.editable && (eb.customValue == null || (eb.customValue.isEmpty() && eb.editableType != DataType.STRING)))
            eb.customValue = eb.editableType.getDefaultValue();

        List<String> paramNames = new ArrayList<>();
        Map<String, DataType> paramTypes = new LinkedHashMap<>();
        for (var pin : def.getInputs())
            if (pin.dataType() != DataType.FLOW) {
                paramNames.add(pin.title());
                paramTypes.put(pin.title(), pin.dataType());
            }

        eb.requiredParams = paramNames;
        eb.paramTypes = paramTypes;
        eb.hasInput = def.getInputs().stream().anyMatch(p -> p.dataType() == DataType.FLOW);
        eb.hasOutput = !def.getOutputs().isEmpty();
        eb.outputType = def.getOutputs().stream()
                .filter(p -> p.dataType() == DataType.FLOW)
                .map(p -> p.dataType())
                .findFirst()
                .orElseGet(() -> def.getOutputs().isEmpty() ? DataType.FLOW : def.getOutputs().get(0).dataType());

        int total = eb.getAllParams().size();
        if (total > 3) eb.width = Math.max(170, (total + 1) * 32);
    }

    public boolean loadFromCodeJson(File workspaceDir, Workspace ws) {
        this.currentWorkspace = ws;
        this.hadLoadIssues = false;
        blocks.clear();

        File codeFile = new File(workspaceDir, "code.json");
        if (!codeFile.exists()) {
            ensureRootBlock(ws);
            return true;
        }

        List<EditorBlock> loaded;
        try (Reader reader = Files.newBufferedReader(codeFile.toPath())) {
            loaded = gson.fromJson(reader, BLOCKS_TYPE);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (loaded == null) {
            ensureRootBlock(ws);
            return true;
        }

        Map<String, BlockDefinition> defMap = new HashMap<>();
        for (BlockDefinition def : Engine.getBlocksForLoader(ws.loaderID)) defMap.put(def.getId(), def);

        List<String> issues = new ArrayList<>();
        List<EditorBlock> validBlocks = new ArrayList<>();

        for (EditorBlock b : loaded) {
            if (b.isRoot) {
                b.name = ws.modName;
                b.category = ws.modID;
                b.hasInput = false;
                b.hasOutput = true;
                b.outputType = DataType.FLOW;
                validBlocks.add(b);
                continue;
            }

            BlockDefinition def = defMap.get(b.typeId);
            if (def == null) {
                issues.add("• Unknown/Missing block: '" + b.typeId + "' (ID: " + b.id + ")");
                continue;
            }

            hydrateBlock(b, def);

            Iterator<Map.Entry<String, Long>> it = b.paramConnections.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> entry = it.next();
                if (!b.paramTypes.containsKey(entry.getKey())) {
                    issues.add("• Obsolete parameter pin '" + entry.getKey() + "' on block '" + def.getDisplayName() + "'");
                    it.remove();
                }
            }

            validBlocks.add(b);
        }

        if (!issues.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "The workspace contains unrecognized blocks or invalid connections:\n\n"
                            + String.join("\n", issues.subList(0, Math.min(issues.size(), 10)))
                            + (issues.size() > 10 ? "\n... and " + (issues.size() - 10) + " more." : "")
                            + "\n\nDo you want to open the workspace anyway?\n"
                            + "(Invalid elements and severed connections will be removed from the editor,\n"
                            + "but your code.json file will NOT be modified until you save).",
                    "Workspace Compatibility Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (result != JOptionPane.YES_OPTION) return false;
            this.hadLoadIssues = true;
        }

        for (EditorBlock b : validBlocks) blocks.put(b.id, b);

        ensureRootBlock(ws);
        isDirty = false;
        repaint();
        return true;
    }

    public boolean saveToCodeJson(File workspaceDir) {
        if (hadLoadIssues) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Warning: When this workspace was opened, some missing blocks or invalid connections were severed.\n"
                            + "Saving now will PERMANENTLY remove them from code.json.\n\n"
                            + "Do you want to proceed with saving?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) return false;
        }

        File codeFile = new File(workspaceDir, "code.json");
        try (Writer writer = Files.newBufferedWriter(codeFile.toPath())) {
            gson.toJson(blocks.values(), writer);
            isDirty = false;
            hadLoadIssues = false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void initWorkspaceGraph(Workspace ws) {
        this.currentWorkspace = ws;
        undoStack.clear();
        redoStack.clear();
        loadFromCodeJson(new File(ws.path), ws);
    }

    private void ensureRootBlock(Workspace ws) {
        boolean hasRoot = blocks.values().stream().anyMatch(b -> b.isRoot);
        if (!hasRoot) {
            EditorBlock root = new EditorBlock(1L, "root_mod", 60, 60);
            root.name = ws.modName;
            root.category = ws.modID;
            root.isRoot = true;
            root.hasInput = false;
            root.hasOutput = true;
            root.outputType = DataType.FLOW;
            blocks.put(root.id, root);
            isDirty = true;
        } else
            blocks.values().stream().filter(b -> b.isRoot).findFirst().ifPresent(r -> {
                r.name = ws.modName;
                r.category = ws.modID;
                r.hasInput = false;
                r.hasOutput = true;
                r.outputType = DataType.FLOW;
            });
    }

    public void addBlock(BlockDefinition def) {
        recordAction();

        long newId = System.currentTimeMillis() + new Random().nextInt(1000);
        int spawnX = toWorldX(getWidth() / 2) - 80;
        int spawnY = toWorldY(getHeight() / 2) - 30;

        EditorBlock eb = new EditorBlock(newId, def.getId(), spawnX, spawnY);
        hydrateBlock(eb, def);

        if (def.isEditable()) eb.customValue = def.getEditableType().getDefaultValue();

        blocks.put(newId, eb);
        isDirty = true;
        repaint();
    }

    public void requestDeleteBlock(long id) {
        EditorBlock b = blocks.get(id);
        if (b == null || b.isRoot) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete block '" + b.name + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            recordAction();
            deleteBlock(id);
        }
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
        selectedBlock = null;
        isDirty = true;
        repaint();
    }

    private Set<EditorBlock> getAttachedBlocks(EditorBlock root) {
        Set<EditorBlock> attached = new LinkedHashSet<>();
        attached.add(root);
        Queue<EditorBlock> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            EditorBlock current = queue.poll();
            for (Long providerId : current.paramConnections.values()) {
                EditorBlock provider = blocks.get(providerId);
                if (provider != null && attached.add(provider)) queue.add(provider);
            }
            for (Long nextId : current.nextBlockIds) {
                EditorBlock next = blocks.get(nextId);
                if (next != null && attached.add(next)) queue.add(next);
            }
        }
        return attached;
    }

    private String takeSnapshotJson() {
        return gson.toJson(blocks.values());
    }

    private void recordAction() {
        pushUndo(takeSnapshotJson());
    }

    private void pushUndo(String snapshot) {
        if (undoStack.size() >= MAX_HISTORY) undoStack.removeFirst();
        undoStack.addLast(snapshot);
        redoStack.clear();
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        redoStack.addLast(takeSnapshotJson());
        restoreFromSnapshot(undoStack.removeLast());
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.addLast(takeSnapshotJson());
        restoreFromSnapshot(redoStack.removeLast());
    }

    private void restoreFromSnapshot(String snapshot) {
        List<EditorBlock> loaded = gson.fromJson(snapshot, BLOCKS_TYPE);
        if (loaded != null && currentWorkspace != null) {
            blocks.clear();
            Map<String, BlockDefinition> defMap = new HashMap<>();
            for (BlockDefinition def : Engine.getBlocksForLoader(currentWorkspace.loaderID)) defMap.put(def.getId(), def);

            for (EditorBlock b : loaded) {
                if (b.isRoot) {
                    b.name = currentWorkspace.modName;
                    b.category = currentWorkspace.modID;
                    b.hasInput = false;
                    b.hasOutput = true;
                    b.outputType = DataType.FLOW;
                } else {
                    BlockDefinition def = defMap.get(b.typeId);
                    if (def != null) hydrateBlock(b, def);
                }
                blocks.put(b.id, b);
            }
            hoveredBlock = null;
            selectedBlock = null;
            connectingBlock = null;
            isDirty = true;
            repaint();
        }
    }

    public boolean canConnectFlow(EditorBlock source, EditorBlock target) {
        if (source == null || target == null || source.id == target.id) return false;
        if (!target.hasInput || source.outputType != DataType.FLOW) return false;

        if (source.isRoot) return "Registry".equalsIgnoreCase(target.category);
        if ("Actions".equalsIgnoreCase(source.category)) return "Actions".equalsIgnoreCase(target.category);
        if ("Keybinds".equalsIgnoreCase(source.category) || "Events".equalsIgnoreCase(source.category)) return "Actions".equalsIgnoreCase(target.category);
        if ("Registry".equalsIgnoreCase(source.category)) return "Registry".equalsIgnoreCase(target.category);
        return false;
    }

    public boolean canConnectParam(EditorBlock source, EditorBlock target, String paramName) {
        if (source == null || target == null || source.id == target.id) return false;
        if (source.isRoot || source.outputType == DataType.FLOW) return false;

        DataType expectedType = target.paramTypes.get(paramName);
        if (expectedType == null || expectedType == DataType.ANY) return true;
        return source.outputType == expectedType;
    }

    public void disconnectAllIncomingTo(long targetId) {
        for (EditorBlock b : blocks.values()) b.nextBlockIds.remove(targetId);
    }

    public void disconnectAllArgumentsFrom(long sourceId) {
        for (EditorBlock b : blocks.values()) b.paramConnections.values().removeIf(val -> val.equals(sourceId));
    }

    private void showBlockContextMenu(EditorBlock b, int screenX, int screenY) {
        JPopupMenu menu = new JPopupMenu();
        if (b.editable) {
            JMenuItem editItem = new JMenuItem("Edit Value (F2)");
            editItem.addActionListener(e -> editBlockValue(b));
            menu.add(editItem);
        }

        JMenuItem disconnectItem = new JMenuItem("Disconnect All");
        disconnectItem.addActionListener(e -> {
            recordAction();
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
            JMenuItem deleteItem = new JMenuItem("Delete Block (Del)");
            deleteItem.addActionListener(e -> requestDeleteBlock(b.id));
            menu.add(deleteItem);
        }
        menu.show(this, screenX, screenY);
    }

    public boolean isDirty() { return isDirty; }

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
            if (b.hasOutput)
                for (Long targetId : b.nextBlockIds) {
                    EditorBlock target = blocks.get(targetId);
                    if (target != null && target.hasInput)
                        drawOrthogonalToLeftPin(g2, b.x + b.width, b.y + b.height / 2, target.x, target.y + target.height / 2, new Color(200, 200, 200));
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
                    wireColor = canConnectFlow(connectingBlock, target) ? new Color(100, 255, 100) : new Color(255, 80, 80);
                    targetWorldX = target.x;
                    targetWorldY = target.y + target.height / 2;
                    break;
                }

                int paramIdx = getParamPinIndex(target, targetWorldX, targetWorldY);
                if (paramIdx != -1) {
                    int total = target.getAllParams().size();
                    String paramName = target.getAllParams().get(paramIdx);

                    wireColor = canConnectParam(connectingBlock, target, paramName) ? new Color(100, 255, 100) : new Color(255, 80, 80);

                    targetWorldX = target.x + (target.width * (paramIdx + 1)) / (total + 1);
                    targetWorldY = target.y + target.height;
                    isOverBottomPin = true;
                    break;
                }
            }

            if (isOverBottomPin) drawOrthogonalToBottomPin(g2, startX, startY, targetWorldX, targetWorldY, wireColor);
            else drawOrthogonalToLeftPin(g2, startX, startY, targetWorldX, targetWorldY, wireColor);
        }

        for (EditorBlock b : blocks.values()) drawBlockCard(g2, b);

        g2.setTransform(defaultTx);

        if (hoveredBlock != null) drawParamTooltip(g2, hoveredBlock, mouseScreenX + 12, mouseScreenY + 12);
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
                drawParamPin(g2, px, py, isConnected);
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

    private void drawParamPin(Graphics2D g2, int cx, int cy, boolean isConnected) {
        int r = 4;
        g2.setColor(isConnected ? new Color(255, 200, 50) : Color.BLACK);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(Color.WHITE);
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
        g2.drawString("req: " + (b.getAllParams().isEmpty() ? "none" : String.join(", ", b.getAllParams())), tx + 8, ty + 36);

        g2.setColor(new Color(170, 170, 255));
        g2.drawString("editable: " + (b.editable ? "Yes (F2 - " + b.editableType + ")" : "No"), tx + 8, ty + 56);
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

    private int toWorldX(int screenX) { return (int) ((screenX - panX) / zoom); }
    private int toWorldY(int screenY) { return (int) ((screenY - panY) / zoom); }
    private List<EditorBlock> getBlocksReversed() {
        List<EditorBlock> list = new ArrayList<>(blocks.values());
        Collections.reverse(list);
        return list;
    }
    private boolean isInsideCircle(int px, int py, int cx, int cy, int r) {
        return (px - cx) * (px - cx) + (py - cy) * (py - cy) <= r * r;
    }

}