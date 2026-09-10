package btxds.mmide.api.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EditorBlock {

    public long id;
    public String typeId;
    public String name;
    public String category;

    public int x;
    public int y;
    public int width = 170;
    public int height = 60;

    public boolean isRoot = false;
    public boolean hasInput = true;
    public boolean hasOutput = true;
    public String customValue = "";

    public List<Long> nextBlockIds = new ArrayList<>();
    public Map<String, Long> paramConnections = new LinkedHashMap<>();

    public List<String> requiredParams = new ArrayList<>();
    public List<String> optionalParams = new ArrayList<>();
    public List<String> allowedTargets = new ArrayList<>();

    public EditorBlock() {
    }

    public EditorBlock(long id, String typeId, String name, String category, int x, int y, List<String> req, List<String> opt, List<String> allowed) {
        this.id = id;
        this.typeId = typeId;
        this.name = name;
        this.category = category;
        this.x = x;
        this.y = y;

        if (req != null) this.requiredParams.addAll(req);
        if (opt != null) this.optionalParams.addAll(opt);
        if (allowed != null) this.allowedTargets.addAll(allowed);

        int totalParams = getAllParams().size();
        if (totalParams > 3) {
            this.width = Math.max(170, (totalParams + 1) * 32);
        }
    }

    public List<String> getAllParams() {
        List<String> all = new ArrayList<>(requiredParams);
        all.addAll(optionalParams);
        return all;
    }

}