package btxds.mmide.api;

import java.util.Collections;
import java.util.List;

public interface Block {

    String getId();
    String getDisplayName();
    String getDescription();

    default String getCategory() {
        return "General";
    }

    default List<String> getRequiredParams() {
        return Collections.emptyList();
    }

    default List<String> getOptionalParams() {
        return Collections.emptyList();
    }

    default List<String> getAllowedTargets() {
        return Collections.emptyList();
    }

    default boolean hasInput() {
        return true;
    }

    default boolean hasOutput() {
        return true;
    }

}