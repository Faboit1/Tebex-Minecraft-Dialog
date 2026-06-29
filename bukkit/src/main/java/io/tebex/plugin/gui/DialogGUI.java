package io.tebex.plugin.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.tebex.plugin.BukkitPluginPlatform;
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
        String titleStr = platform.getPlugin().getConfig().getString("gui.menu.home.title", "Server Shop");
        title.addProperty("text", remapLegacyFormatSeparator(titleStr));
        dialog.add("title", title);

        JsonArray body = new JsonArray();
        JsonObject plainMessage = new JsonObject();
        JsonObject plainMessageText = new JsonObject();
        plainMessageText.addProperty("text", "Please select a category:");
        plainMessage.add("plain_message", plainMessageText);
        body.add(plainMessage);
        dialog.add("body", body);

        JsonArray actions = new JsonArray();
        
        categories.sort(Comparator.comparingInt(Category::getOrder));
        for (Category category : categories) {
            JsonObject action = new JsonObject();
            JsonObject label = new JsonObject();
            label.addProperty("text", remapLegacyFormatSeparator(category.getName()));
            action.add("label", label);
            action.addProperty("type", "run_command");
            action.addProperty("command", "buy category " + category.getId());
            actions.add(action);
        }

        dialog.add("actions", actions);

        JsonArray footer = new JsonArray();
        JsonObject closeAction = new JsonObject();
        JsonObject closeLabel = new JsonObject();
        closeLabel.addProperty("text", "Close");
        closeAction.add("label", closeLabel);
        closeAction.addProperty("type", "close");
        footer.add(closeAction);
        dialog.add("footer", footer);

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
        String titleStr = platform.getPlugin().getConfig().getString("gui.menu.category.title", "Category: %category%");
        titleStr = titleStr.replace("%category%", foundCategory.getName());
        title.addProperty("text", remapLegacyFormatSeparator(titleStr));
        dialog.add("title", title);

        JsonArray body = new JsonArray();
        JsonObject plainMessage = new JsonObject();
        JsonObject plainMessageText = new JsonObject();
        plainMessageText.addProperty("text", "Select a package to purchase:");
        plainMessage.add("plain_message", plainMessageText);
        body.add(plainMessage);
        dialog.add("body", body);

        JsonArray actions = new JsonArray();

        foundCategory.getPackages().sort(Comparator.comparingInt(CategoryPackage::getOrder));

        if (foundCategory instanceof Category) {
            Category cat = (Category) foundCategory;
            if (cat.getSubCategories() != null) {
                for (SubCategory subCategory : cat.getSubCategories()) {
                    JsonObject action = new JsonObject();
                    JsonObject label = new JsonObject();
                    label.addProperty("text", "[+] " + remapLegacyFormatSeparator(subCategory.getName()));
                    action.add("label", label);
                    action.addProperty("type", "run_command");
                    action.addProperty("command", "buy category " + subCategory.getId());
                    actions.add(action);
                }
            }
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        String currencySymbol = platform.getStoreInformation().getStore().getCurrency().getSymbol();

        for (CategoryPackage pkg : foundCategory.getPackages()) {
            JsonObject action = new JsonObject();
            JsonObject label = new JsonObject();
            
            String priceStr = currencySymbol + decimalFormat.format(pkg.getPrice());
            if (pkg.hasSale()) {
                priceStr = currencySymbol + decimalFormat.format(pkg.getPrice() - pkg.getSale().getDiscount()) + " (Sale)";
            }
            
            label.addProperty("text", remapLegacyFormatSeparator(pkg.getName() + " - " + priceStr));
            action.add("label", label);
            action.addProperty("type", "run_command");
            action.addProperty("command", "buy package " + pkg.getId());
            actions.add(action);
        }

        dialog.add("actions", actions);

        JsonArray footer = new JsonArray();
        
        JsonObject backAction = new JsonObject();
        JsonObject backLabel = new JsonObject();
        backLabel.addProperty("text", "Back");
        backAction.add("label", backLabel);
        backAction.addProperty("type", "run_command");
        
        if (foundCategory instanceof SubCategory) {
            backAction.addProperty("command", "buy category " + ((SubCategory) foundCategory).getParent().getId());
        } else {
            backAction.addProperty("command", "buy");
        }
        footer.add(backAction);

        JsonObject closeAction = new JsonObject();
        JsonObject closeLabel = new JsonObject();
        closeLabel.addProperty("text", "Close");
        closeAction.add("label", closeLabel);
        closeAction.addProperty("type", "close");
        footer.add(closeAction);

        dialog.add("footer", footer);

        dispatchDialog(player, dialog);
    }

    public void openPackage(Player player, int packageId) {
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

    private String remapLegacyFormatSeparator(String input) {
        return input.replaceAll("&", "§");
    }

    private void dispatchDialog(Player player, JsonObject dialogJson) {
        String json = dialogJson.toString();
        // Send /dialog command via console to the player
        Bukkit.getScheduler().runTask(platform.getPlugin(), () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dialog show " + player.getName() + " " + json);
        });
    }
}
