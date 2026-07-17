package tempeststudios.inventorysort;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

final class SearchHighlightSelection {
    private final Map<String, LinkedHashSet<LocationEntry>> explicitSelections = new HashMap<>();

    void selectAll(String resultId, Collection<LocationEntry> locations) {
        explicitSelections.clear();
        ChestHighlightRenderer.setHighlightedLocations(locations);
    }

    boolean selectOne(String resultId, LocationEntry location) {
        if (location == null || location.getPos() == null) {
            return false;
        }

        LinkedHashSet<LocationEntry> selected = explicitSelections.computeIfAbsent(resultId, ignored -> new LinkedHashSet<>());
        selected.add(location);
        ChestHighlightRenderer.setHighlightedLocations(selected);
        return true;
    }
}
