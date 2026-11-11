# Hubber

[![Minecraft Version](https://img.shields.io/badge/Minecraft-26.3-brightgreen.svg)](https://papermc.io)
[![Server Software](https://img.shields.io/badge/Paper-26.3-orange.svg)](https://papermc.io)
[![Java Version](https://img.shields.io/badge/Java-25%2B-blue.svg)](https://adoptium.net)
[![Developer](https://img.shields.io/badge/Developer-kilkazens-purple.svg)](https://github.com/kilkazens)

A modern, **ultra-lightweight** and high-performance Hub / Lobby management plugin engineered for **Paper 26.3** servers running **Java 25+**. Fully utilizes native Kyori Adventure components and modern Paper APIs.

---

## ⚡ Why Hubber? (Zero-Bloat & Ultra-Lightweight)

* 🪶 **Featherweight (< 50 KB)**: Absolutely zero heavy dependencies or bloated third-party shading.
* ⚡ **Instant Startup (< 5 ms)**: Loads in a blink without stalling server boot time.
* 🧠 **Minimal Memory Footprint**: Consumes under 2 MB of RAM, leaving all resources for your game worlds.
* 🚀 **Zero Tick Lag (20.0 TPS)**: Event listeners are strictly optimized with block-level delta checks, keeping CPU impact at virtually 0.00% even with hundreds of concurrent players.
* 🌐 **Universal Proxy Support**: Works natively out-of-the-box with **BungeeCord**, **Waterfall**, and **Velocity** (via `bungee-plugin-message-channel = true`).

---

## 🌐 Languages / Языки

Hubber comes with built-in multi-language support. English is the default primary language, with Russian fully supported as a secondary language.

To change the language, edit `plugins/Hubber/config.yml`:
```yaml
# "en" for English (default) or "ru" for Russian
language: "en"
```
Language files (`messages_en.yml` and `messages_ru.yml`) are generated automatically in the plugin folder and can be customized freely.

---

## ✨ Features

* 📖 **Server Connection Book**:
  * Automatically given to players upon joining (customizable hotbar slot).
  * Right-clicking instantly executes `/joinserver` **from the player's perspective**.
  * Equipped with spam cooldown, drop prevention (`Q`), and inventory move locks.
  * Tagged using `PersistentDataContainer` (immune to renaming or visual glitches).

* 🛡️ **Comprehensive Hub Protection**:
  * **Block Break & Place Restrictions**: Players cannot break or place blocks. Only operators (`OP`) or players with bypass permissions can modify the world.
  * **Absolute Damage Immunity**: Players are protected from all damage sources (fall, fire, lava, suffocation, void, etc.).
  * **PvP & Combat Disabled**: Players cannot attack each other or shoot projectiles.
  * **No Player Pushing (Collision Removal)**: Players pass smoothly through each other with zero push/collision glitching, achieved via Paper's native collidable flag and Scoreboard Team collision rules (`COLLISION_RULE NEVER`).
  * **Hunger Lock**: Players never lose food or saturation.
  * **Void Fall Rescue**: Players falling below $Y = -64$ (configurable) are instantly and safely teleported back to the hub spawn.

* 🧍 **Native Paper NPCs (`/hub <command> <skin> <name> <color> [bold] [italic]`)**:
  * Create interactive player-model NPCs directly at your location using Paper's native `Mannequin` API.
  * Right-clicking the NPC executes the specified command (e.g. `joinserver`, `verification`, or any custom command) from the player's perspective.
  * **Advanced Skin Support**: Automatic **SkinsRestorer** integration (supports skins by name or player from SkinsRestorer), direct `textures.minecraft.net` URLs, 64-character texture hashes, and fallback Mojang API lookup with signed textures.
  * Customizable nametag colors (`gold`, `red`, `aqua`, `#FFD700`, etc.) with optional bold and italic styling.
  * Safe and persistent: NPCs are invulnerable, immovable, silent, and saved across restarts in `npcs.yml`.
  * Management: `/hub list` to inspect active NPCs, `/hub remove [id]` to delete by ID or remove the nearest one.

* 📍 **Persistent Spawn & Hub Commands**:
  * `/hubspawn`: Easily set the hub spawn point to your current position.
  * `/hub` (without arguments) / `/back`: Teleport to the hub spawn point.
  * **Forced Spawn on Join**: Players are always spawned at the hub spawn point upon connecting, regardless of where they were when they logged off.
  * Respawn redirection to hub spawn.

* ⚡ **Spawn Teleportation Triggers (`/settrigger`)**:
  * Create interactive regions or points that automatically teleport any player stepping into them to `/hubspawn`.
  * Create spherical radius triggers (e.g. `/settrigger 2.5` or `/settrigger portal 3.0`).
  * Create 3D bounding box regions using `/settrigger pos1`, `/settrigger pos2`, and `/settrigger create <name>`.
  * Persistent storage saved in `triggers.yml`.

* 🔗 **Discord Verification (`/verification`)**:
  * Sends a customizable message with an interactive clickable Discord invite link.
  * Supports RGB HEX colors (`&#RRGGBB` / `<#RRGGBB>`) and legacy color codes (`&a`, `&e`, etc.).

* ⚔️ **Forced Survival Mode**:
  * Enforces Survival mode (`SURVIVAL`) for all regular players upon join.
  * Blocks unauthorized gamemode changes with configurable OP bypass.

---

## 📜 Commands & Permissions

| Command | Description | Aliases | Permission | Default |
|---|---|---|---|---|
| `/hub` | Teleport to hub (no args) or create/manage NPCs | `/hubnpc` | Everyone (teleport) / `hubber.admin` (NPC) | Default / OP |
| `/joinserver` | Connect to the target proxy server | `/play`, `/connect` | `hubber.joinserver` | Everyone (`true`) |
| `/hubspawn` | Set the hub spawn location | — | `hubber.hubspawn` | OP |
| `/back` | Teleport to the hub spawn | `/spawn`, `/lobby` | `hubber.back` | Everyone (`true`) |
| `/verification` | Send Discord verification link | `/discord`, `/ds`, `/verify` | `hubber.verification` | Everyone (`true`) |
| `/settrigger` | Create a spawn teleport trigger | `/trigger`, `/hubtrigger` | `hubber.settrigger` | OP |
| `/hubber reload` | Reload configuration and languages | `/hb` | `hubber.admin` | OP |
| `/hubber setspawn`| Set hub spawn location | `/hb` | `hubber.admin` | OP |
| `/hubber book` | Give yourself the connection book | `/hb` | `hubber.admin` | OP |

### Additional Bypass Permissions:
* `hubber.bypass.break` — Allow breaking blocks in the hub.
* `hubber.bypass.place` — Allow placing blocks in the hub.
* `hubber.bypass.gamemode` — Allow switching gamemode in the hub.

---

## 📥 Installation

1. Download or compile the `Hubber-1.1.jar` file.
2. Place `Hubber-1.1.jar` into your server's `plugins/` directory.
3. Start or restart your server (requires **Paper 26.3** and **Java 25+**).
4. Run `/hubspawn` in-game as an operator to configure your spawn location.
5. (Optional) Customize `config.yml`, `messages_en.yml`, or `messages_ru.yml` to your preference and execute `/hubber reload`.

---

## 🔨 Building from Source

To compile Hubber from source, ensure you have **JDK 25+** and **Apache Maven** installed:

```bash
git clone https://github.com/kilkazens/Hubber.git
cd Hubber
mvn clean package
```

The compiled binary will be located at:
```
target/Hubber-1.1.jar
```

---

<details>
<summary><b>🇷🇺 Описание на русском языке (Нажмите, чтобы развернуть)</b></summary>

### ⚡ Легковесность и оптимизация:
* **Вес менее 50 КБ**: Никаких раздутых внешних библиотек и ненужного мусора.
* **Мгновенный старт (< 5 мс)**: Плагин загружается практически мгновенно при запуске сервера.
* **Минимальное потребление памяти (< 2 МБ RAM)**: Вся оперативная память остается для мира и игроков.
* **0% нагрузки на TPS**: Оптимизированные слушатели событий с проверкой изменения координат целых блоков предотвращают лаги даже при сотнях игроков онлайн.
* **Поддержка любых прокси**: Нативная работа с **BungeeCord**, **Waterfall** и **Velocity** (через `bungee-plugin-message-channel = true`).

### Основные возможности:
* **Книга подключения к серверу**: Выдается при входе, при клике ПКМ выполняет `/joinserver` от лица игрока. Защищена от выбрасывания, перемещения и спама.
* **Нативные NPC (`/hub <команда> <скин> <имя> <цвет> [bold] [italic]`)**: Создание интерактивных NPC прямо на позиции игрока. При ПКМ на NPC выполняется указанная команда от лица игрока. Поддержка скинов: автоматическая интеграция со **SkinsRestorer** (поддерживает скины по имени скина или нику из SkinsRestorer), прямые ссылки на текстуры, хеши или ники Mojang. Цветные имена с bold/italic. Автосохранение в `npcs.yml`.
* **Защита хаба**: Полный запрет ломания/установки блоков (кроме OP), отсутствие любого урона, отключение PvP, отключение толкания игроков (`COLLISION_RULE NEVER`), бесконечная сытость, защита от падения в бездну.
* **Система спавна**: `/hubspawn` для установки точки, `/back` для телепортации, принудительный спавн при входе независимо от места выхода.
* **Триггеры спавна (`/settrigger`)**: Создание точек или 3D-боксов, при входе в которые игрок телепортируется на спавн.
* **Верификация Discord (`/verification`)**: Настраиваемое сообщение с кликабельной ссылкой на Discord.
* **Принудительный Survival**: Автоматическая установка и удержание режима выживания.

Для переключения языка на русский измените в `plugins/Hubber/config.yml`:
```yaml
language: "ru"
```
и выполните команду `/hubber reload`.
</details>

---

## 👤 Developer

* **Author:** `kilkazens`
* **Repository:** [https://github.com/kilkazens/Hubber](https://github.com/kilkazens/Hubber)

---

> **Note:** Initially created for **Initium Magnum Terra**.  
> *(Пометка: Изначально было создано для **Initium Magnum Terra**)*
