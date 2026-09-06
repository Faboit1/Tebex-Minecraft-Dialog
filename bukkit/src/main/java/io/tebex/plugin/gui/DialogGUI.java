package io.tebex.plugin.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.plugin.manager.FreePackageTracker;
import io.tebex.plugin.util.CheeseCoreSprites;
import io.tebex.plugin.util.ComponentUtil;
import io.tebex.plugin.util.FoliaUtil;
import io.tebex.plugin.util.MaterialUtil;
import io.tebex.plugin.util.SpriteUtil;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.obj.CategoryPackage;
import io.tebex.sdk.obj.ICategory;
import io.tebex.sdk.obj.StoreDescriptions;
import io.tebex.sdk.obj.SubCategory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

        dialog.add("title", ComponentUtil.parse(cfg("gui.menu.home.title", "Server Shop")));

        dialog.add("body", buildBody(cfg("gui.dialog.body-text", "Please select a category:")));
        dialog.addProperty("columns", columnsFor("home-columns"));

        String freeMarker = cfg("gui.dialog.free-marker", "<red>[FREE]<reset> ");
        int buttonWidth = cfgInt("gui.dialog.button-width", 200);
        boolean spritesEnabled = cfgBool("gui.dialog.sprites", true);
        String playerName = player.getName();

        JsonArray actions = new JsonArray();

        categories.sort(Comparator.comparingInt(Category::getOrder));
        for (Category category : categories) {
            String displayName = category.getName();
            if (categoryHasFreeForPlayer(category, playerName)) {
                displayName = freeMarker + displayName;
            }
            JsonObject label = buildLabel(displayName, spritesEnabled ? category.getGuiItem() : null, player);
            JsonObject action = runCommandAction(label, "buy category " + category.getId());
            action.addProperty("width", buttonWidth);
            addTooltip(action, category.getDescription(), "categories", category.getId());
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

        String titleStr = cfg("gui.menu.category.title", "Viewing %category%")
                .replace("%category%", foundCategory.getName());
        dialog.add("title", ComponentUtil.parse(titleStr));

        dialog.add("body", buildBody(cfg("gui.dialog.category-body-text", "Select a package to purchase:")));
        dialog.addProperty("columns", columnsFor("category-columns"));

        String freeMarker = cfg("gui.dialog.free-marker", "<red>[FREE]<reset> ");
        String saleColor = cfg("gui.dialog.sale-color", "<yellow>");
        String saleSuffix = cfg("gui.dialog.sale-suffix", "(Sale)");
        String freeText = cfg("gui.dialog.free-text", "Free");
        String freeOnCooldownFmt = cfg("gui.dialog.free-on-cooldown-format", "%currency%0");
        String priceFmt = cfg("gui.dialog.price-format", "%currency%%price%");
        String subCategoryPrefix = cfg("gui.dialog.subcategory-prefix", "[+] ");
        int buttonWidth = cfgInt("gui.dialog.button-width", 200);
        boolean spritesEnabled = cfgBool("gui.dialog.sprites", true);
        String playerName = player.getName();

        JsonArray actions = new JsonArray();

        foundCategory.getPackages().sort(Comparator.comparingInt(CategoryPackage::getOrder));

        if (foundCategory instanceof Category) {
            Category cat = (Category) foundCategory;
            if (cat.getSubCategories() != null) {
                for (SubCategory subCategory : cat.getSubCategories()) {
                    String displayName = subCategoryPrefix + subCategory.getName();
                    if (subCategoryHasFreeForPlayer(subCategory, playerName)) {
                        displayName = freeMarker + displayName;
                    }
                    JsonObject label = buildLabel(displayName, spritesEnabled ? subCategory.getGuiItem() : null, player);
                    JsonObject action = runCommandAction(label, "buy category " + subCategory.getId());
                    action.addProperty("width", buttonWidth);
                    addTooltip(action, subCategory.getDescription(), "categories", subCategory.getId());
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
                priceStr = pkg.getName() + " - " + freeMarker + freeText;
            } else if (pkg.isFree()) {
                String cooldownPrice = freeOnCooldownFmt
                        .replace("%currency%", currencySymbol);
                priceStr = pkg.getName() + " - " + cooldownPrice;
            } else if (pkg.hasSale()) {
                String formattedPrice = priceFmt
                        .replace("%currency%", currencySymbol)
                        .replace("%price%", decimalFormat.format(effectivePrice));
                priceStr = pkg.getName() + " - " + formattedPrice
                        + " " + saleColor + saleSuffix;
            } else {
                String formattedPrice = priceFmt
                        .replace("%currency%", currencySymbol)
                        .replace("%price%", decimalFormat.format(pkg.getPrice()));
                priceStr = pkg.getName() + " - " + formattedPrice;
            }

            JsonObject label = buildLabel(priceStr, spritesEnabled ? pkg.getItemId() : null, player);
            JsonObject action = runCommandAction(label, "buy package " + pkg.getId());
            action.addProperty("width", buttonWidth);
            addTooltip(action, pkg.getDescription(), "packages", pkg.getId());
            actions.add(action);
        }

        String backCommand;
        if (foundCategory instanceof SubCategory) {
            backCommand = "buy category " + ((SubCategory) foundCategory).getParent().getId();
        } else {
            backCommand = "buy";
        }

        // A multi_action dialog needs at least one action, and a category with no
        // packages and no subcategories would otherwise send an empty list.
        if (actions.size() == 0) {
            actions.add(backButton(backCommand, buttonWidth));
        }

        dialog.add("actions", actions);

        // Escape runs the exit action, so pointing it at the parent level walks the
        // player back up the shop instead of dropping them out of it entirely. Only
        // the top-level menu exits outright.
        dialog.add("exit_action", backButton(backCommand, buttonWidth));

        dispatchDialog(player, dialog);
    }

    public void openPackage(Player player, int packageId) {
        CategoryPackage pkg = findPackageById(packageId);
        if (pkg != null) {
            platform.getFreePackageTracker().recordClaim(pkg, player.getName());
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
        FreePackageTracker tracker = platform.getFreePackageTracker();
        if (tracker.anyClaimable(category.getPackages(), playerName)) return true;
        if (category.getSubCategories() != null) {
            for (SubCategory sub : category.getSubCategories()) {
                if (tracker.anyClaimable(sub.getPackages(), playerName)) return true;
            }
        }
        return false;
    }

    private boolean subCategoryHasFreeForPlayer(SubCategory subCategory, String playerName) {
        return platform.getFreePackageTracker().anyClaimable(subCategory.getPackages(), playerName);
    }

    private boolean isPackageFreeForPlayer(CategoryPackage pkg, String playerName) {
        return platform.getFreePackageTracker().isClaimable(pkg, playerName);
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
        plainMessage.addProperty("type", "plain_message");
        plainMessage.add("contents", ComponentUtil.parse(message));
        body.add(plainMessage);
        return body;
    }

    private JsonObject buildLabel(String text, String guiItem, Player player) {
        JsonObject sprite = spriteFor(guiItem, player);
        JsonObject label = new JsonObject();
        label.addProperty("text", "");

        JsonArray extra = new JsonArray();
        if (sprite != null) {
            extra.add(sprite);
            JsonObject spacer = new JsonObject();
            spacer.addProperty("text", " ");
            extra.add(spacer);
        }
        extra.addAll(ComponentUtil.parseParts(text));

        if (extra.size() > 0) {
            label.add("extra", extra);
        }

        return label;
    }

    /**
     * Resolves the icon for a button.
     *
     * <p>CheeseCore is preferred when installed: it knows which atlas holds a texture on
     * the viewer's own client version and what a material's sprite is actually called,
     * neither of which can be derived from the material name alone. The built-in
     * resolver is the fallback for servers without it.</p>
     */
    private JsonObject spriteFor(String guiItem, Player player) {
        if (guiItem == null) {
            return null;
        }

        Material material = MaterialUtil.fromString(guiItem).orElse(null);
        if (material == null) {
            return null;
        }

        if (cfgBool("gui.dialog.cheesecore", true)) {
            CheeseCoreSprites.SpriteRef resolved = CheeseCoreSprites.resolve(material, player);
            if (resolved != null) {
                // Chests, banners and other block-entity items have no flat texture, so
                // all CheeseCore can offer is their break-particle colour: a solid
                // swatch. Skipping those leaves a plain text button instead.
                if (resolved.isApproximate() && !cfgBool("gui.dialog.approximate-sprites", true)) {
                    return null;
                }
                return spriteComponent(resolved.getAtlas(), resolved.getSprite());
            }
            if (CheeseCoreSprites.isAvailable()) {
                // CheeseCore is authoritative when present: it returning nothing means
                // this client cannot draw sprites, or this material has none.
                return null;
            }
        }

        if (cfgBool("gui.dialog.sprite-version-check", true) && !SpriteUtil.isSpriteSupported()) {
            return null;
        }
        return SpriteUtil.spriteComponent(material);
    }

    private JsonObject spriteComponent(String atlas, String sprite) {
        JsonObject component = new JsonObject();
        component.addProperty("atlas", atlas);
        component.addProperty("sprite", sprite);
        return component;
    }

    private JsonObject runCommandAction(JsonObject label, String command) {
        JsonObject action = new JsonObject();
        action.add("label", label);
        action.add("action", runCommandClick(command));
        return action;
    }

    private JsonObject runCommandClick(String command) {
        JsonObject click = new JsonObject();
        click.addProperty("type", "run_command");
        click.addProperty("command", command);
        return click;
    }

    /**
     * Adds the hover tooltip to a button. Text configured under
     * {@code gui.dialog.tooltips.<section>.<id>} wins over the store description, which
     * the Tebex API does not return for every store.
     */
    private boolean addTooltip(JsonObject action, String description, String section, int id) {
        if (!cfgBool("gui.dialog.tooltips.enabled", true)) return false;

        // Config first, then the Headless API, then whatever the listing carried. The
        // plugin API has no description field at all, so on most stores the listing text
        // is empty and the Headless API is the only source that returns anything.
        String configured = cfg("gui.dialog.tooltips." + section + "." + id, "");
        String text;
        if (configured != null && !configured.isEmpty()) {
            text = configured;
        } else {
            StoreDescriptions descriptions = platform.getStoreDescriptions();
            String headless = "categories".equals(section)
                    ? descriptions.forCategory(id)
                    : descriptions.forPackage(id);
            text = stripHtml(!headless.isEmpty() ? headless : description);
        }

        if (text.isEmpty()) return false;

        action.add("tooltip", ComponentUtil.parse(text));
        return true;
    }

    private JsonObject backButton(String backCommand, int buttonWidth) {
        JsonObject backAction = new JsonObject();
        backAction.add("label", ComponentUtil.parse(cfg("gui.dialog.back-button", "« Back")));
        backAction.add("action", runCommandClick(backCommand));
        backAction.addProperty("width", buttonWidth);
        return backAction;
    }

    private JsonObject closeAction() {
        JsonObject exitAction = new JsonObject();
        exitAction.add("label", ComponentUtil.parse(cfg("gui.dialog.close-button", "Close")));
        return exitAction;
    }

    /**
     * Turns a store description into tooltip text. Headless API descriptions are HTML, so
     * block breaks become newlines before tags are dropped, entities are decoded, and the
     * result is capped — a full package description can run to several paragraphs, which
     * makes for an unusable hover.
     */
    private String stripHtml(String html) {
        if (html == null || html.isEmpty()) return "";

        String text = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|li|h[1-6])>", "\n")
                .replaceAll("<[^>]*>", "");

        text = text
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");

        // Collapse the runs of blank lines that stripping block tags leaves behind.
        text = text.replaceAll("[ \\t]+", " ")
                .replaceAll(" ?\n ?", "\n")
                .replaceAll("\n{2,}", "\n")
                .trim();

        int limit = Math.max(0, cfgInt("gui.dialog.tooltips.max-length", 256));
        if (limit > 0 && text.length() > limit) {
            text = text.substring(0, limit).trim() + "...";
        }

        return text;
    }

    private String cfg(String path, String defaultValue) {
        return platform.getPlugin().getConfig().getString(path, defaultValue);
    }

    private int cfgInt(String path, int defaultValue) {
        return platform.getPlugin().getConfig().getInt(path, defaultValue);
    }

    /**
     * Column count for one view, falling back to the shared {@code gui.dialog.columns}
     * so an existing config that only sets that keeps working.
     */
    private int columnsFor(String key) {
        int shared = cfgInt("gui.dialog.columns", 1);
        return Math.max(1, cfgInt("gui.dialog." + key, shared));
    }

    private boolean cfgBool(String path, boolean defaultValue) {
        return platform.getPlugin().getConfig().getBoolean(path, defaultValue);
    }

    /**
     * Counts how many buttons actually came out with an icon and a tooltip, so the log
     * line says whether sprites and store descriptions were resolved without needing the
     * JSON itself to be read closely.
     */
    private String summarise(JsonObject dialogJson) {
        JsonArray actions = dialogJson.getAsJsonArray("actions");
        if (actions == null) return "buttons=0";

        int withSprite = 0;
        int withTooltip = 0;
        for (int i = 0; i < actions.size(); i++) {
            JsonObject action = actions.get(i).getAsJsonObject();
            if (action.has("tooltip")) withTooltip++;

            JsonObject label = action.getAsJsonObject("label");
            JsonArray extra = label == null ? null : label.getAsJsonArray("extra");
            if (extra != null && extra.size() > 0 && extra.get(0).isJsonObject()
                    && extra.get(0).getAsJsonObject().has("atlas")) {
                withSprite++;
            }
        }

        return "buttons=" + actions.size() + ", withSprite=" + withSprite
                + ", withTooltip=" + withTooltip;
    }

    private void dispatchDialog(Player player, JsonObject dialogJson) {
        String json = dialogJson.toString();

        // The purchase queue check logs on every poll, so full debug drowns this out.
        // gui.dialog.log-json surfaces it on its own without enabling debug at all.
        String report = "Dialog for " + player.getName()
                + " [cheesecore=" + CheeseCoreSprites.describe()
                + ", " + SpriteUtil.describeSupport()
                + ", " + summarise(dialogJson) + "]: " + json;
        if (cfgBool("gui.dialog.log-json", false)) {
            platform.info(report);
        } else {
            platform.debug(report);
        }
        FoliaUtil.runSync(platform.getPlugin(), () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dialog show " + player.getName() + " " + json);
        });
    }
}
