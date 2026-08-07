package io.tebex.plugin.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.plugin.manager.CooldownManager;
import io.tebex.plugin.util.MaterialUtil;
import io.tebex.plugin.util.SpriteUtil;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.obj.CategoryPackage;
import io.tebex.sdk.obj.ICategory;
import io.tebex.sdk.obj.SubCategory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;

public class DialogGUI {
    private final BukkitPluginPlatform platform;

    public DialogGUI(BukkitPluginPlatform platform) {
        this.platform = platform;
    }

    public void open(Player player) {
        List<Category> categories = platform.getStoreCategories();
        if (categories == null) {
            player.sendMessage(ChatColor.RED + "Failed to get listing. Please contact an administrator.");
            return;
        }

        JsonObject dialog = new JsonObject();
        dialog.addProperty("type", "minecraft:multi_action");

        JsonObject title = new JsonObject();
        String titleStr = getConfigString("gui.menu.home.title", "Server Shop");
        title.addProperty("text", remapLegacyFormatSeparator(titleStr));
        dialog.add("title", title);

        dialog.add("body", buildBody("Please select a category:"));
        dialog.addProperty("columns", 1);

        String freeMarker = getConfigString("gui.dialog.free-marker", "&c[FREE]&r ");
        int buttonWidth = getConfigInt("gui.dialog.button-width", 200);
        String playerName = player.getName();

        JsonArray actions = new JsonArray();

        categories.sort(Comparator.comparingInt(Category::getOrder));
        for (Category category : categories) {
            String displayName = category.getName();
            if (categoryHasFreeForPlayer(category, playerName)) {
                displayName = freeMarker + displayName;
            }
            JsonObject label = buildLabel(displayName, category.getGuiItem());
            JsonObject action = runCommandAction(label, "buy category " + category.getId());
            action.addProperty("width", buttonWidth);
            addTooltip(action, category.getDescription());
            actions.add(action);
        }

        dialog.add("actions", actions);
        dialog.add("exit_action", closeAction());

        dispatchDialog(player, dialog);
    }

    public void openCategory(Player player, int categoryId) {
        List<Category> categories = platform.getStoreCategories();
        if (categories == null) {
            player.sendMessage(ChatColor.RED + "Failed to get listing.");
            return;
        }

        ICategory foundCategory = null;
        for (Category cat : categories) {
            if (cat.getId() == categoryId) {
                foundCategory = cat;
                break;
            }
            if (cat.getSubCategories() != null) {
                for (SubCategory subCat : cat.getSubCategories()) {
                    if (subCat.getId() == categoryId) {
                        foundCategory = subCat;
                        break;
                    }
                }
            }
            if (foundCategory != null) break;
        }

        if (foundCategory == null) {
            player.sendMessage(ChatColor.RED + "Category not found.");
            open(player);
            return;
        }

        JsonObject dialog = new JsonObject();
        dialog.addProperty("type", "minecraft:multi_action");

        JsonObject title = new JsonObject();
        String titleStr = getConfigString("gui.menu.category.title", "Category: %category%");
        titleStr = titleStr.replace("%category%", foundCategory.getName());
        title.addProperty("text", remapLegacyFormatSeparator(titleStr));
        dialog.add("title", title);

        dialog.add("body", buildBody("Select a package to purchase:"));
        dialog.addProperty("columns", 1);

        String freeMarker = getConfigString("gui.dialog.free-marker", "&c[FREE]&r ");
        String saleColor = getConfigString("gui.dialog.sale-color", "&e");
        int buttonWidth = getConfigInt("gui.dialog.button-width", 200);
        String playerName = player.getName();

        JsonArray actions = new JsonArray();

        foundCategory.getPackages().sort(Comparator.comparingInt(CategoryPackage::getOrder));

        if (foundCategory instanceof Category) {
            Category cat = (Category) foundCategory;
            if (cat.getSubCategories() != null) {
                for (SubCategory subCategory : cat.getSubCategories()) {
                    String displayName = "[+] " + subCategory.getName();
                    if (subCategoryHasFreeForPlayer(subCategory, playerName)) {
                        displayName = freeMarker + displayName;
                    }
                    JsonObject label = buildLabel(displayName, subCategory.getGuiItem());
                    JsonObject action = runCommandAction(label, "buy category " + subCategory.getId());
                    action.addProperty("width", buttonWidth);
                    addTooltip(action, subCategory.getDescription());
                    actions.add(action);
                }
            }
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        String currencySymbol = platform.getStoreInformation().getStore().getCurrency().getSymbol();

        for (CategoryPackage pkg : foundCategory.getPackages()) {
            double effectivePrice = pkg.getEffectivePrice();
            String priceStr;
            if (pkg.isFree() && isPackageFreeForPlayer(pkg, playerName)) {
                priceStr = pkg.getName() + " - " + freeMarker + "Free";
            } else if (pkg.isFree()) {
                priceStr = pkg.getName() + " - " + currencySymbol + "0";
            } else if (pkg.hasSale()) {
                priceStr = pkg.getName() + " - " + currencySymbol + decimalFormat.format(effectivePrice)
                        + " " + saleColor + "(Sale)";
            } else {
                priceStr = pkg.getName() + " - " + currencySymbol + decimalFormat.format(pkg.getPrice());
            }

            JsonObject label = buildLabel(priceStr, pkg.getItemId());
            JsonObject action = runCommandAction(label, "buy package " + pkg.getId());
            action.addProperty("width", buttonWidth);
            addTooltip(action, pkg.getDescription());
            actions.add(action);
        }

        JsonObject backLabel = new JsonObject();
        backLabel.addProperty("text", "« Back");
        String backCommand;
        if (foundCategory instanceof SubCategory) {
            backCommand = "buy category " + ((SubCategory) foundCategory).getParent().getId();
        } else {
            backCommand = "buy";
        }
        JsonObject backAction = runCommandAction(backLabel, backCommand);
        backAction.addProperty("width", buttonWidth);
        actions.add(backAction);

        dialog.add("actions", actions);
        dialog.add("exit_action", closeAction());

        dispatchDialog(player, dialog);
    }

    public void openPackage(Player player, int packageId) {
        CategoryPackage pkg = findPackageById(packageId);
        if (pkg != null && pkg.isFree() && pkg.hasCooldown()) {
            CooldownManager cm = platform.getCooldownManager();
            if (cm != null) {
                cm.recordClaim(player.getName(), packageId, pkg.getCooldownSeconds());
            }
        }

        player.closeInventory();
        platform.getSDK().createCheckoutUrl(packageId, player.getName())
                .thenAccept(checkout -> {
                    platform.sendCheckoutLink(player.getName(), checkout.getUrl());
                }).exceptionally(ex -> {
                    player.sendMessage(ChatColor.RED
                            + "Failed to create checkout URL. Please contact an administrator.");
                    return null;
                });
    }

    private boolean categoryHasFreeForPlayer(Category category, String playerName) {
        if (hasFreeAvailable(category.getPackages(), playerName)) return true;
        if (category.getSubCategories() != null) {
            for (SubCategory sub : category.getSubCategories()) {
                if (hasFreeAvailable(sub.getPackages(), playerName)) return true;
            }
        }
        return false;
    }

    private boolean subCategoryHasFreeForPlayer(SubCategory subCategory, String playerName) {
        return hasFreeAvailable(subCategory.getPackages(), playerName);
    }

    private boolean hasFreeAvailable(List<CategoryPackage> packages, String playerName) {
        for (CategoryPackage pkg : packages) {
            if (pkg.isFree() && isPackageFreeForPlayer(pkg, playerName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPackageFreeForPlayer(CategoryPackage pkg, String playerName) {
        if (!pkg.isFree()) return false;
        if (!pkg.hasCooldown()) return true;
        CooldownManager cm = platform.getCooldownManager();
        if (cm == null) return true;
        return !cm.isOnCooldown(playerName, pkg.getId(), pkg.getCooldownSeconds());
    }

    private CategoryPackage findPackageById(int packageId) {
        List<Category> categories = platform.getStoreCategories();
        if (categories == null) return null;
        for (Category cat : categories) {
            for (CategoryPackage pkg : cat.getPackages()) {
                if (pkg.getId() == packageId) return pkg;
            }
            if (cat.getSubCategories() != null) {
                for (SubCategory sub : cat.getSubCategories()) {
                    for (CategoryPackage pkg : sub.getPackages()) {
                        if (pkg.getId() == packageId) return pkg;
                    }
                }
            }
        }
        return null;
    }

    private JsonArray buildBody(String message) {
        JsonArray body = new JsonArray();
        JsonObject plainMessage = new JsonObject();
        JsonObject plainMessageText = new JsonObject();
        plainMessageText.addProperty("text", message);
        plainMessage.addProperty("type", "plain_message");
        plainMessage.add("contents", plainMessageText);
        body.add(plainMessage);
        return body;
    }

    private JsonObject buildLabel(String text, String guiItem) {
        JsonObject sprite = spriteFor(guiItem);
        JsonObject label = new JsonObject();

        if (sprite != null) {
            label.addProperty("text", "");
            JsonArray extra = new JsonArray();
            extra.add(sprite);
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", " " + remapLegacyFormatSeparator(text));
            extra.add(textPart);
            label.add("extra", extra);
        } else {
            label.addProperty("text", remapLegacyFormatSeparator(text));
        }

        return label;
    }

    private JsonObject spriteFor(String guiItem) {
        if (guiItem == null || !SpriteUtil.isSpriteSupported()) {
            return null;
        }
        return MaterialUtil.fromString(guiItem).map(SpriteUtil::spriteComponent).orElse(null);
    }

    private JsonObject runCommandAction(JsonObject label, String command) {
        JsonObject action = new JsonObject();
        action.add("label", label);

        JsonObject click = new JsonObject();
        click.addProperty("type", "run_command");
        click.addProperty("command", command);
        action.add("action", click);

        return action;
    }

    private void addTooltip(JsonObject action, String description) {
        String cleaned = stripHtml(description);
        if (cleaned.isEmpty()) return;
        JsonObject tooltip = new JsonObject();
        tooltip.addProperty("text", cleaned);
        action.add("tooltip", tooltip);
    }

    private JsonObject closeAction() {
        JsonObject exitAction = new JsonObject();
        JsonObject exitLabel = new JsonObject();
        exitLabel.addProperty("text", "Close");
        exitAction.add("label", exitLabel);
        return exitAction;
    }

    private String remapLegacyFormatSeparator(String input) {
        return input.replace("&", "§");
    }

    private String stripHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        return html.replaceAll("<[^>]*>", "").trim();
    }

    private String getConfigString(String path, String defaultValue) {
        return platform.getPlugin().getConfig().getString(path, defaultValue);
    }

    private int getConfigInt(String path, int defaultValue) {
        return platform.getPlugin().getConfig().getInt(path, defaultValue);
    }

    private void dispatchDialog(Player player, JsonObject dialogJson) {
        String json = dialogJson.toString();
        Bukkit.getScheduler().runTask(platform.getPlugin(), () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dialog show " + player.getName() + " " + json);
        });
    }
}
