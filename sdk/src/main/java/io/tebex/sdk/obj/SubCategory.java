package io.tebex.sdk.obj;

import com.google.gson.JsonObject;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class SubCategory implements ICategory {
    private final int id;
    private final int order;
    private final String name;
    private final String description;
    private final String guiItem;
    private final Category parentCategory;
    private final List<CategoryPackage> categoryPackages;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getGuiItem() {
        return guiItem;
    }

    public Category getParent() {
        return parentCategory;
    }

    public List<CategoryPackage> getPackages() {
        return categoryPackages;
    }

    public boolean hasFreePackage() {
        for (CategoryPackage pkg : categoryPackages) {
            if (pkg.getEffectivePrice() <= 0) return true;
        }
        return false;
    }

    public static SubCategory fromJsonObject(JsonObject jsonObject, Category category) {
        String description = jsonObject.has("description") && !jsonObject.get("description").isJsonNull()
                ? jsonObject.get("description").getAsString() : "";

        return new SubCategory(
                jsonObject.get("id").getAsInt(),
                jsonObject.get("order").getAsInt(),
                jsonObject.get("name").getAsString(),
                description,
                jsonObject.get("gui_item").getAsString(),
                category,
                jsonObject.getAsJsonArray("packages").asList().stream().map(item -> CategoryPackage.fromJsonObject(item.getAsJsonObject())).collect(Collectors.toList())
        );
    }
}
