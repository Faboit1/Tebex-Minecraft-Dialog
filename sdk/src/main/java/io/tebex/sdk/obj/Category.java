package io.tebex.sdk.obj;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class Category implements ICategory {
    private final int id;
    private final int order;
    private final String name;
    private final String description;
    private final String guiItem;
    private final boolean onlySubcategories;
    private final List<CategoryPackage> categoryPackages;
    private List<SubCategory> subCategories = new ArrayList<>();

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

    public List<CategoryPackage> getPackages() {
        return categoryPackages;
    }

    public boolean hasFreePackage() {
        for (CategoryPackage pkg : categoryPackages) {
            if (pkg.getEffectivePrice() <= 0) return true;
        }
        if (subCategories != null) {
            for (SubCategory sub : subCategories) {
                for (CategoryPackage pkg : sub.getPackages()) {
                    if (pkg.getEffectivePrice() <= 0) return true;
                }
            }
        }
        return false;
    }

    public static Category fromJsonObject(JsonObject jsonObject) {
        String description = jsonObject.has("description") && !jsonObject.get("description").isJsonNull()
                ? jsonObject.get("description").getAsString() : "";

        Category category = new Category(
                jsonObject.get("id").getAsInt(),
                jsonObject.get("order").getAsInt(),
                jsonObject.get("name").getAsString(),
                description,
                jsonObject.get("gui_item").getAsString(),
                jsonObject.has("only_subcategories") && jsonObject.get("only_subcategories").getAsBoolean(),
                jsonObject.getAsJsonArray("packages").asList().stream().map(item -> CategoryPackage.fromJsonObject(item.getAsJsonObject())).collect(Collectors.toList())
        );

        category.setSubCategories(jsonObject.getAsJsonArray("subcategories").asList().stream().map(item -> SubCategory.fromJsonObject(item.getAsJsonObject(), category)).collect(Collectors.toList()));
        return category;
    }
}
