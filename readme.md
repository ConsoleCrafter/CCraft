# 🎮 CCraft

Eine Minecraft Fabric Mod mit Custom Commands, GUIs und HUD-Overlays.

> **Hinweis:** Diese Version ist ein aufgeräumter Neustart. Alle Cheat-,
> Crash- und Scanner-Funktionen wurden entfernt. Neue Commands werden
> Schritt für Schritt neu hinzugefügt.

---

## ✨ Features

- 🎨 **Custom Main Menu** mit eigenem Design
- 🔐 **Login-System** (bcrypt-geprüft) vor dem Hauptmenü
- 🖥️ **Custom HUD** mit Info-Anzeige (XYZ, FPS, TPS, Ping, Memory, ...)
- 🎮 **Command Menu** (Rechter Shift) mit Kategorien Utility / Games / Settings
- 🎵 **Mini-Games** (Tetris, Snake, Flappy Bird, Doodle Jump)
- 🧮 **Taschenrechner**
- 💬 **Chat Logger**
- 🔧 **Performance Mods** (Sodium, Lithium, Iris, FerriteCore, ModMenu)

---

## 🛠️ Entwicklung

### Projekt bauen
```bash
./gradlew build
```
Die fertige `.jar` findest du dann in `build/libs/`

### Mod testen
```bash
./gradlew runClient
```

---

## 🔧 Eigenen Command hinzufügen

### Schritt 1: Command-Klasse erstellen
Erstelle eine neue Datei in `commands/impl/`, z. B. `MeinCommand.java`:

```java
package de.consolecrafter.ccraft.commands.impl;

import de.consolecrafter.ccraft.commands.Command;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class MeinCommand implements Command {

    @Override
    public String getName() {
        return "meincommand";
    }

    @Override
    public String getDescription() {
        return "Beschreibung deines Commands";
    }

    @Override
    public String getUsage() {
        return "#meincommand";
    }

    @Override
    public void execute(ClientPlayerEntity player, String[] args) {
        player.sendMessage(Text.literal("§c§nCCraft§r §0» §aHallo!"), false);
    }
}
```

### Schritt 2: Command registrieren
In `CommandRegistry.java` importieren und registrieren:

```java
import de.consolecrafter.ccraft.commands.impl.MeinCommand;

// in registerCommands():
registerCommand(new MeinCommand());
```

Fertig! Der Command ist im Spiel mit `.meincommand` verfügbar.

---

## ⚙️ Keybinds

| Taste | Funktion |
|-------|----------|
| `C` | Zoom (halten) |
| `Right Shift` | Command Menu öffnen |

---

## 📦 Enthaltene Bibliotheken

- **Sodium** – Performance-Optimierung
- **Lithium** – Server/Client-Optimierung
- **Iris** – Shader Support
- **FerriteCore** – Memory-Optimierung
- **ModMenu** – Mod-Konfiguration
- **Account Switcher** – Account-Verwaltung

---

## 🏗️ Projektstruktur

```
src/main/
├── java/de/consolecrafter/ccraft/
│   ├── commands/
│   │   ├── impl/          # Command-Implementierungen
│   │   ├── Command.java
│   │   ├── CommandHandler.java
│   │   └── CommandRegistry.java
│   ├── gui/               # GUI Screens
│   ├── hud/               # HUD Overlay
│   ├── keybinds/          # Keybind-Definitionen
│   ├── listener/          # Event Listener
│   ├── mixins/            # Mixin Injections
│   └── util/              # Utility-Klassen
└── resources/
    └── assets/ccraft/
        ├── shaderpacks/
        ├── resourcepacks/
        ├── sounds/
        └── textures/
```

---

## 📝 Lizenz

Siehe `LICENSE`.
