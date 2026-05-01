<div align="center">
  <img src="src/main/resources/assets/modrinth-direct/icon.png" alt="Modrinth Direct Icon" width="128"/>

  # Modrinth Direct

  **Browse and install mods from Modrinth — without ever leaving Minecraft.**

  ![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen?logo=minecraft)
  ![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
  ![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin)
  ![License](https://img.shields.io/badge/License-MIT-lightgrey)
</div>

---

## What is this?

Modrinth Direct is a client-side Fabric mod that replaces the vanilla **Mods** button
(added by ModMenu) with a fully functional mod browser powered by the
[Modrinth API](https://docs.modrinth.com). Search, browse, and install mods into your
`mods/` folder — all without switching to a browser or launcher.

## Features

- 🔍 **In-game mod search** — query the entire Modrinth library without leaving the game
- 📦 **One-click install** — download and save the `.jar` directly to your `mods/` folder
- 🔗 **Automatic dependency resolution** — required dependencies are fetched and installed automatically
- 🗂️ **Installed mods tab** — keeps track of everything you've installed through the mod
- 🖼️ **Mod icons** — loads mod artwork inline, including WebP format
- 🍞 **Toast notifications** — in-game feedback for download progress, success, and errors
- ♻️ **Non-destructive** — a simple restart is all it takes for new mods to activate

## Dependencies

| Dependency | Version | Link |
|---|---|---|
| Fabric Loader | `>= 0.19.2` | [fabricmc.net](https://fabricmc.net/use/installer/) |
| Fabric API | `*` | [Modrinth](https://modrinth.com/mod/fabric-api) |
| Fabric Language Kotlin | `*` | [Modrinth](https://modrinth.com/mod/fabric-language-kotlin) |
| oωo-lib | `>= 0.13.0` | [Modrinth](https://modrinth.com/mod/owo-lib) |
| ModMenu | `>= 17.0.0` | [Modrinth](https://modrinth.com/mod/modmenu) |

> **Java 21 or newer is required.**

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.11.
2. Download all dependencies listed above and place their `.jar` files in your `mods/` folder.
3. Download the latest `modrinth-direct-*.jar` from [Releases](../../releases) and drop it into `mods/`.
4. Launch the game. The **Mods** button on the title screen is now your in-game Modrinth browser.

## Usage

1. Open the game and click **Mods** on the title screen.
2. Use the **search bar** to find any mod available on Modrinth.
3. Click a mod card to select it, then click **Install**.
4. A toast notification will confirm when the download is complete.
5. **Restart** Minecraft to load the newly installed mods.

Mods you've installed via Modrinth Direct appear in the **Installed** tab for easy reference.

## Building from Source

```bash
git clone https://github.com/GAZIZlikesLollipop/modrinth-direct.git
cd modrinth-direct
./gradlew build
```

The output `.jar` will be in `build/libs/`.

## License

This project is licensed under the [MIT License](LICENSE).
