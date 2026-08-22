![Logo](https://www.tebex.io/assets/img/logos/tebex.svg#gh-light-mode-only)
![Static Badge](https://img.shields.io/badge/spigot-1.8--1.21.x-brightgreen)
![Static Badge](https://img.shields.io/badge/paper-1.21.6+-brightgreen)
![Static Badge](https://img.shields.io/badge/folia-1.21.6+-brightgreen)
![Static Badge](https://img.shields.io/badge/fabric-1.16.5+-brightgreen)
![Static Badge](https://img.shields.io/badge/neoforge-26.1+-brightgreen)
![Static Badge](https://img.shields.io/badge/bungeecord-1.18+-brightgreen)
![Static Badge](https://img.shields.io/badge/velocity-1.16.5+-brightgreen)

## About This Fork

This is a fork of the official [Tebex-Minecraft](https://github.com/tebexio/Tebex-Minecraft) plugin that replaces the chest GUI shop with Minecraft's native **Dialog API** (1.21.6+). Instead of opening inventory-based menus, players see a proper dialog window with clickable buttons for categories and packages.

### Fork Features

- **Dialog-based shop UI** using the Minecraft 1.21.6+ Dialog API (`/dialog show`)
- **Sprite icons** on buttons for item/block textures (MC 1.21.9+, auto-detected)
- **Folia and Canvas support** with reflection-based scheduler detection
- **MiniMessage formatting** in all dialog text strings (e.g. `<red>`, `<bold>`, `<gradient:red:blue>`)
- **Sale display** with configurable sale suffix, color, and strikethrough pricing
- **Free package cooldowns** configured per package ID, tracked in a local SQLite database
- **Free package reminders** that periodically tell players when they have something to claim
- **Package descriptions** shown as tooltips on hover (fetched from the full `/packages` API)
- **Fully configurable** text, button widths, columns, price formats, and free/sale markers via `config.yml`
- Falls back to the original chest GUI on servers below 1.21.6

### How It Works

On servers running MC 1.21.6+, the `/buy` command opens a dialog window instead of a chest inventory. The dialog displays:
- **Category view**: Lists all store categories as buttons. Subcategories are shown inline with a configurable prefix.
- **Package view**: Lists packages in a category with prices, sale indicators, and optional sprite icons. Clicking a package opens the Tebex checkout link.
- **Tooltips**: Hovering over a package button shows its description (if available from the store).

Purchase commands are fulfilled automatically every 3 seconds by default (configurable via `check-interval` in `config.yml`). Store listings refresh every 5 minutes.

## Dialog Configuration

All dialog settings are under `gui.dialog` in `config.yml`:

```yaml
gui:
  dialog:
    enabled: true          # Use dialogs instead of chest GUI (1.21.6+ only)
    sprites: true          # Show item/block icons on buttons (1.21.9+ only)
    button-width: 200      # Button width in pixels
    columns: 1             # Number of button columns (1 or 2)
    body-text: "Please select a category:"
    category-body-text: "Select a package to purchase:"
    close-button: "Close"
    back-button: "« Back"
    subcategory-prefix: "[+] "
    free-marker: "<red>[FREE]<reset> "
    free-text: "Free"
    sale-suffix: "(Sale)"
    sale-color: "<yellow>"
    price-format: "%currency%%price%"
    free-on-cooldown-format: "%currency%0"
    tooltips:
      enabled: true
      categories: {}
      packages: {}
```

### Tooltips

Hovering a button shows the store description for that category or package. The Tebex API does
not return descriptions for every store, so you can supply the text yourself, keyed by ID:

```yaml
gui:
  dialog:
    tooltips:
      enabled: true
      categories:
        98765: "Our best deals"
      packages:
        1234567: "Claimable once every 12 hours"
```

Configured text wins over the store description, and supports MiniMessage tags. Run
`/tebex debug true` then `/tebex refresh` to log which packages arrived without a description.

### Sprite icons

Sprites are native atlas icons and need no resource pack, but they require **MC 1.21.9+** —
on 1.21.6–1.21.8 buttons fall back to plain text. Sprite paths resolve as `block/<material>` or
`item/<material>`, so a material whose texture is not a flat atlas entry (chests, beds, banners
and other block-entity models) has no sprite to draw. Pick a material with a normal texture for
those categories, or set `gui.dialog.sprites: false`.

Text values support MiniMessage tags (`<red>`, `<bold>`, `<italic>`, `<gradient:red:blue>`, etc.) which are converted to Minecraft's section-sign color codes.

## Free Packages

A package counts as free when its price is 0 after any sale is applied. By default free packages
can be claimed as often as a player likes; a cooldown makes them claimable once per period.

```yaml
free-packages:
  # Cooldown in seconds for every free package with no entry under 'cooldowns'.
  # 0 means free packages can always be claimed.
  default-cooldown: 0

  # Per-package cooldowns in seconds, keyed by Tebex package ID.
  cooldowns:
    1234567: 43200   # this package is claimable once every 12 hours

  # Periodically reminds players who have something free waiting.
  reminder:
    enabled: true
    interval-minutes: 10
    message: "<green>You have free items waiting! Use <yellow>/buy<green> to claim them."
```

Find a package's ID in the URL when you edit it in the Tebex creator panel. Useful values:
`3600` (1 hour), `43200` (12 hours), `86400` (24 hours).

Cooldowns are resolved in this order, so config always wins:

1. `free-packages.cooldowns.<packageId>`
2. `meta.cooldown_seconds` from the Tebex API, on stores that expose package meta
3. `free-packages.default-cooldown`

The API meta path is a convenience for stores that return it — the Tebex plugin API does not
expose package meta for every store, so the config is the reliable place to set a cooldown.

Claims are recorded in `plugins/Tebex/data.db` (SQLite) per player and package, and expired
entries are pruned every 10 minutes.

## Messages

```yaml
messages:
  checkout: "<green>Checkout started! Complete payment here: <yellow>%url%"
  # Sent instead to Bedrock players, who cannot click links in chat.
  checkout-bedrock: "<green>Checkout started! Type this link into your browser: <yellow>%url%"
```

`%url%` is the checkout link and `%player%` is the player's name. Setting a message to an empty
string suppresses it. These apply to the Bukkit/Paper/Folia plugin; Fabric, NeoForge, and the
proxy platforms keep their built-in checkout text.

New options are added to an existing `config.yml` automatically on startup, so upgrading does not
require deleting the file. Values you have already changed are left alone.

## Installation and Setup

1. Create a free webstore at https://tebex.io/
2. Download the latest version of the plugin from the Releases tab of this repository.
3. Place the downloaded Tebex `.jar` in the `plugins` folder for plugin platforms, or the `mods` folder for Fabric and NeoForge.
4. Restart your server / reload your plugins
5. Run `tebex.secret your-key-here` as a server admin to connect the server to Tebex.

Your secret key can always be found at: https://creator.tebex.io/game-servers. Click Connect Game Server, and then choose Plugin to view your secret key.

## Usage and Commands

Note: Not all commands are available on all platforms. Proxy servers may have a reduced set of commands. Use `tebex.help` to get the relevant list of commands on any platform.

### Permissions
All commands have a permission node which matches with the exact command name. For example a player must have `tebex.help` as a permission in order to view available
commands.

### User Commands
```
tebex.help                          Shows available commands
tebex.secret <key>                  Sets your store's secret key
tebex.info                          Shows store information
tebex.checkout <packId> <username>  Creates payment link for a package
```

### Administrator Commands
```
tebex.sendlink <name> <package>     Sends payment link to player
tebex.report <message>              Reports a problem to Tebex
tebex.ban <name>                    Bans a user from the webstore
tebex.lookup <name>                 Looks up user transaction info
```

### Debug Commands
```
tebex.debug <true/false>    Enables debug logging
tebex.forcecheck            Force runs all time-based events
tebex.refresh               Reloads store and package info
```

## Compatibility

| Platform | Versions | Dialog UI | Notes |
|----------|----------|-----------|-------|
| Spigot | 1.8 - 1.21.x | 1.21.6+ | Falls back to chest GUI on older versions |
| Paper | 1.21.6+ | Yes | Recommended for best performance |
| Folia | 1.21.6+ | Yes | Full support via reflection-based schedulers |
| Canvas | 26.x+ | Yes | Uses `getMinecraftVersion()` for accurate version detection |
| Fabric | 1.16.5+ | No | Uses original implementation |
| NeoForge | 26.1+ | No | Uses original implementation |
| BungeeCord | 1.18+ | N/A | Proxy platform |
| Velocity | 1.16.5+ | N/A | Proxy platform |

## Resources

- [Frequently Asked Questions](https://docs.tebex.io/creators/faq)
- [Tebex Academy](https://www.youtube.com/@tebex/videos) - Video tutorials
- [Technical Support](mailto:support@tebex.io) - Email support@tebex.io
- [Developer Documentation](https://docs.tebex.io/developers/)

## Building

### Requirements
- JDK 8+
- Gradle (wrapper included)

### Build
Run `./build.sh` or `./gradlew collectBuilds`. Built jars are placed in the `builds/` directory.
