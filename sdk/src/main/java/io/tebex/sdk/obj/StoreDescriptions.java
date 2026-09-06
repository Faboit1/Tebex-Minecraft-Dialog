package io.tebex.sdk.obj;

import java.util.Collections;
import java.util.Map;

/**
 * Category and package descriptions fetched from the Tebex Headless API, keyed by ID.
 *
 * <p>The plugin API's {@code /packages} endpoint is deprecated and its response carries
 * no description at all, so the Headless API is the only supported source for the text
 * shown in dialog tooltips.</p>
 */
public class StoreDescriptions {
    private static final StoreDescriptions EMPTY =
            new StoreDescriptions(Collections.<Integer, String>emptyMap(), Collections.<Integer, String>emptyMap());

    private final Map<Integer, String> categories;
    private final Map<Integer, String> packages;

    public StoreDescriptions(Map<Integer, String> categories, Map<Integer, String> packages) {
        this.categories = categories;
        this.packages = packages;
    }

    public static StoreDescriptions empty() {
        return EMPTY;
    }

    public Map<Integer, String> getCategories() {
        return categories;
    }

    public Map<Integer, String> getPackages() {
        return packages;
    }

    public String forCategory(int id) {
        String value = categories.get(id);
        return value == null ? "" : value;
    }

    public String forPackage(int id) {
        String value = packages.get(id);
        return value == null ? "" : value;
    }

    public boolean isEmpty() {
        return categories.isEmpty() && packages.isEmpty();
    }

    public int size() {
        return categories.size() + packages.size();
    }
}
