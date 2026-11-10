# Hubber

[![Minecraft Version](https://img.shields.io/badge/Minecraft-26.3-brightgreen.svg)](https://papermc.io)
[![Server Software](https://img.shields.io/badge/Paper-26.3-orange.svg)](https://papermc.io)
[![Java Version](https://img.shields.io/badge/Java-25%2B-blue.svg)](https://adoptium.net)
[![Developer](https://img.shields.io/badge/Developer-kilkazens-purple.svg)](https://github.com/kilkazens)

A modern, high-performance, and feature-rich Hub / Lobby management plugin engineered for **Paper 26.3** servers running **Java 25+**. Fully utilizes native Kyori Adventure components and modern Paper APIs.

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

* 📍 **Persistent Spawn & Hub Commands**:
  * `/hubspawn`: Easily set the hub spawn point to your current position.
  * `/back` (aliases: `/hub`, `/spawn`, `/lobby`): Teleport back to the hub spawn point.
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
| `/joinserver` | Connect to the target proxy server | `/play`, `/connect` | `hubber.joinserver` | Everyone (`true`) |
| `/hubspawn` | Set the hub spawn location | — | `hubber.hubspawn` | OP |
| `/back` | Teleport to the hub spawn | `/hub`, `/spawn`, `/lobby` | `hubber.back` | Everyone (`true`) |
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

1. Download or compile the `Hubber-1.0.0.jar` file.
2. Place `Hubber-1.0.0.jar` into your server's `plugins/` directory.
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
target/Hubber-1.0.0.jar
```

---

<details>
<summary><b>🇷🇺 Описание на русском языке (Нажмите, чтобы развернуть)</b></summary>

### Основные возможности:
* **Книга подключения к серверу**: Выдается при входе, при клике ПКМ выполняет `/joinserver` от лица игрока. Защищена от выбрасывания, перемещения и спама.
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
