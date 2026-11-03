# CCraft — Agent Notes

Minecraft Fabric client mod, package `de.consolecrafter.ccraft`.
Project root: `CCraft/7zE82263292/`.

## Build
No system Java. Use the portable JDK 21:
```bash
export JAVA_HOME=/tmp/jdk21 && export PATH=$JAVA_HOME/bin:$PATH
cd CCraft/7zE82263292
./gradlew build --no-daemon      # jar -> build/libs/ccraft-0.5.jar
```
Gradle daemon is flaky in this sandbox; prefer `--no-daemon`.

### Build + direkt herunterladbar
`scripts/build-and-serve.sh` baut das Jar und liefert es per HTTP aus
(Port 12000, `scripts/serve-jar.py`). Download-Link:
`https://work-1-fhidvskyyfswgxbp.prod-runtime.all-hands.dev/ccraft-0.5.jar`
Nach jedem Build einfach dieses Skript ausfuehren. Achtung: `/tmp/jdk21` und
der laufende Server leben nur innerhalb der Session; nach einem Container-
Restart laedt das Skript das JDK neu und startet den Server erneut.

## Toolchain
MC 1.21.10, yarn 1.21.10+build.1, loader 0.17.2, fabric-api 0.138.0+1.21.10,
loom 1.15.4, `mod_version` 0.5.

## Design system
- All colors/geometry live in `util/Theme.java` (red/black brand).
- `Theme.button(...)` draws the shared rounded/translucent/red button look.
- `mixins/ButtonMixin` targets `PressableWidget.renderWidget` and restyles
  **every** button globally (incl. mod buttons like ViaFabricPlus).
  Do NOT write custom `renderWidget` overrides in screens.
- `mixins/ScreenMixin` replaces `renderBackground` outside a world with the
  red gradient.
- Screens must call `super.render(...)` **after** drawing their own panel,
  otherwise the panel covers widgets (this was the login overflow bug).

## Renaming third-party buttons
- ViaFabricPlus adds a `"ViaFabricPlus"` button in
  `MultiplayerScreen.repositionElements()`, which `MultiplayerScreen.init()`
  calls. Do NOT mixin into VFP for this.
- Instead use Fabric API: `ScreenEvents.AFTER_INIT` (fires after the screen's
  `init()`), check `screen instanceof MultiplayerScreen`, then walk
  `Screens.getButtons(screen)` and `widget.setMessage(Text.literal("Version"))`
  when the label matches. Implemented in `CCraftMod.onInitializeClient`.

## Startup / packs
- Shaders are NEVER auto-installed or auto-activated anymore
  (`CCraftMod` no longer calls `copyShaderPack()`/`activateShaderPack()`; those
  methods were deleted). The shader zip is still bundled but optional.
- The resource pack is still copied and activated on startup
  (`copyResourcePack()` + `activateResourcePack()`).

## HUD editor + config
- `gui/AppearanceScreen` (Reiter "HUD", erreichbar aus beiden Menues) schaltet
  jede HUD-Zeile einzeln (`CraftConfig.hudLines[0..9]` = XYZ, IGN, FPS, IP, TPS,
  Software, Ping, Memory, Uhr, Online), Stil, Textfarbe, Schatten und die
  Keystrokes-Optionen. Die Keystrokes-Vorschau wird per Ziehen positioniert
  (`CraftConfig.keystrokesX/keystrokesY`, 0..1 relativ).
- The CCraft logo ALWAYS stays fixed at the top-left of the HUD — never tied to
  `hudX/hudY` (styling variants only move the info block below it).
- `CustomHud.renderHud` draws the logo via `drawLogo(2,2)` first, then the info
  block; all three styles take `(x, y)`.

## Screenshots
- `gui/ScreenshotScreen` lists `run/screenshots/*.png`. Tiles are registered as
  textures at runtime with `NativeImageBackedTexture` + `TextureManager`
  (`Identifier.of("ccraft", "screenshot/" + n)`), cached in static maps so they
  survive screen switches. Click = full view, plus delete and "open folder".

## Chat Logger
- `ChatLoggerScreen` shows search, source filter (Alle/Chat/System), timestamp
  toggle, export to `ccraft_chat_export_*.txt`, clear, wheel scroll. Entries are
  `Entry(time, text, chat)`; persistence format is `time \t C|S \t text` and
  legacy lines are parsed heuristically.

## Login
- `LoginScreen` has animated accent particles + panel glow, and a "Merken"
  toggle (`CraftConfig.saveLogin`/`savedUser`) that pre-fills the name.
- Accounts stay bcrypt-hashed in `ACCOUNTS`; verification via BCrypt.

## Tic Tac Toe
- Opponent is "Craftie" (the client mascot). It "thinks" for ~1.2–2.1 s
  (`Game.tick()`, driven by `Screen.tick()`), shows "Craftie überlegt…", then
  plays. AI is minimax with `MISTAKE_CHANCE` (0.12) so no fixed pattern wins.

## Verbindungs- und Fehler-Screens
- `gui/ConnectingOverlay` ersetzt den Vanilla-"Joining Server"-Screen
  (`ConnectScreenMixin`). Zeigt Ziel (`host:port`), die **zur IP aufgelöste**
  Adresse (`InetAddress.getByName` asynchron, nicht die Domain), die aktuelle
  Phase (DNS/Handshake/Login/Welt), einen Punkte-Fortschritt sowie ein kleines
  Live-Log. Der Mixin implementiert `ServerAddressHolder`, damit die Adresse
  übergeben wird (via `serverAddress` getaggt, wird von `mixins.ServerAddressHolder`
  gelesen).
- `mixins/DisconnectedScreenMixin` ersetzt das Vanilla-"Verbindung verloren"-
  Fenster durch eine eigene Fehlerbox (Grund vom Server + Timeout/Hinweis),
  inkl. des Vanilla-Zurück-Buttons (`buttonLabel`).

## In-Game-Menü (Rechter Shift)
- `CommandMenuScreen`: Kategorien als **Sidebar links** (140px), Inhalt rechts.
  Das ist die Original-Optik — NICHT wieder auf horizontale Tabs ändern.
  Utility = Chat Logger / Taschenrechner / *Train-Commands, Games = Spiele,
  Settings = Darstellung / Screenshots / Einstellungen / Info.
  (Die CCraft-Einstellungen bleiben eine eigene Seite, nicht in dieses Menü
  gespiegelt.)

## Games
- Every game is a self-contained `Screen` in `gui/` (Tetris, Snake, Flappy Bird,
  Doodle Jump, Block Blast, Tic Tac Toe). Add a new one by creating the
  screen and registering it in BOTH places:
  - `CommandMenuScreen` "Games" tab, and
  - `CCraftSettingsScreen.groups()` "Games" group.
- `CCraftSettingsScreen` is a scrollable grid (mouse wheel). It culls buttons
  offscreen (`button.visible = false`) and draws group headers from
  `headerDraws`. Games/utility live in dense grids, so adding entries needs no
  layout changes.
- `CommandMenuScreen` (right-shift menu) has horizontal category tabs at the top
  and a grid of entries below; right-shift closes it again
  (`CommandMenuScreen.keyPressed` -> `CCraftMod.closeCommandMenu()`).

## Design system (Theme + CraftConfig)
- `util/Theme` fields are now MUTABLE (`public static int`, not `final`).
  Never read a Theme color into a `static final` of another class — read it at
  draw time. After changing any value call `Theme.apply()`.
- `util/CraftConfig` persists design settings to
  `config/ccraft-design.properties` (`dark`, `accent`, `preset`, `hudStyle`,
  `hudText`, `hudShadow`). `CCraftMod.onInitializeClient` calls
  `CraftConfig.load(); Theme.apply();` on startup.
- `util/Themes.PRESETS` holds named accent palettes; apply with `Themes.apply(p)`
  then `Theme.apply()`.
- `gui/AppearanceScreen` (Reiter "Theme", erreichbar aus beiden Menues) waehlt
  Dark/Light, ein Preset oder eine eigene RGB-Farbe. Theme und HUD liegen in
  EINEM Screen mit zwei Reitern; vorher waren es zwei getrennte Screens. Das
  Layout wird in `layout()` einmal berechnet und von `init()` und `render()`
  geteilt; `SECTION_GAP`/`LABEL_GAP` verhindern Ueberlappungen, der Inhalt ist
  bei kurzem Fenster per Mausrad scrollbar.
- HUD styles (`CraftConfig.hudStyle`): 0 Classic (logo + ▶ list), 2 Boxed
  (Theme panel). Legacy value 1 is normalized to Classic when loading config.
  Boxed sits slightly lower than Classic; text color/shadow come from CraftConfig.

## Screens/background
- `ScreenMixin` calls `RenderUtil.drawBackground` -> `Theme.background`, so the
  whole menu background follows the selected theme. In-world screens skip it.

## Trains (HUD)
- `CustomHud.Train` scrolls across the screen; speed is the constant
  `Train.TRAIN_SPEED` (pixels per frame). It was creeping at 1px/4 ticks;
  now 6px/frame. Tune only that constant.

## Mixin rules (learned the hard way)
- **Never `@Shadow` inherited members.** Mixin resolves `@Shadow` against the
  *target class body*, not its superclasses. `isHovered()` (method_49606) is
  declared on `ClickableWidget`, so a `@Shadow` in a `PressableWidget` mixin
  fails at load: "was not located in the target class ... No refMap loaded".
  Instead cast: `((ClickableWidget) (Object) this).getMessage()` etc.
- Loom remaps mod classes to **intermediary** names at build time. Mixin
  targets must therefore use intermediary names (`method_48579` for
  `renderWidget`). No refmap is produced/needed; do not enable
  `useLegacyMixinAp` or add a refmap.
- Verify every injected method really exists on the target class:
  `javap -p` the intermediary jar, and confirm the method is concrete (not
  inherited-abstract). `PressableWidget.renderWidget` is concrete, so injecting
  into it works.
- Do not add global side effects (cursor changes, etc.) in the button mixin —
  vanilla sets the cursor elsewhere; extra calls cause flicker/regressions.

## Verify mixin targets
Built mixin classes reference intermediary names directly. Cross-check against
yarn mappings (`method_48579` = `renderWidget`, `method_25394` = `render`,
`method_25440` = `getTitle`, `method_25420` = `renderBackground`).

## Mixin-Regeln (aus Crash gelernt)
- **Statische Vanilla-Methode ⇒ statischer Callback mit EXAKTER Parameterliste.**
  `ConnectScreen.connect` ist `static` mit Signatur
  `(Screen, MinecraftClient, ServerAddress, ServerInfo, boolean, CookieStorage)`.
  Typische Fehler:
  - nicht-statischer Handler → `non-static callback ... targets a static method`
  - falsche/zu wenige Parameter → `InvalidInjectionException: Invalid descriptor`
    (Mixin nennt die *expected* Signatur wörtlich — die exakt übernehmen).
  Handler `static` machen und den Zustand in einer statischen Hilfsklasse
  ablegen (hier `gui.ConnectingOverlay`). Verifiziert im Build: Handler ist
  `private static (class_437, class_310, class_639, class_642, boolean, class_9112, CallbackInfo)`.
- Bei mehrdeutigen Methodennamen immer die **volle Deskriptor-Signatur** im
  `method = "..."` angeben.
- `DisconnectedScreen`-Felder sind `parent`, `info` (`DisconnectionInfo`),
  `buttonLabel`; `init` + `render` überschreibbar (extends `Screen`).
- Texture-Quellgröße prüfen: `sidtrain.png` 549×105, `devintrain` 549×111,
  `melontrain` 553×113. Als 183×(183·h/w) zeichnen, sonst verzerrt.

## UI-Konsistenz
- Jeder Screen nutzt `Theme.header(ctx, tr, name, cx)` (Zeile 8 "§lCCraft",
  Zeile 22 Name in Akzent). Inhalt beginnt erst **unter y≈34** — sonst
  überschreibt der Titel Textfelder/Inhalt.
- Buttons via `ButtonWidget.builder` (ButtonMixin rendert sie einheitlich rund).
  Positionen **relativ zu `this.width/height`** berechnen, nie mit festen
  Offsets wie `PADDING + 424` — sonst Overflow im 854er-Fenster.
- Scroll-Screens: Einheitliche Höhe **24px**, Sichtbarkeit
  `y >= TOP && y + H <= contentBottom`, und `scrollOffset` **vor** dem Layout
  clampen.

## LoginScreen (Original-Accounts, nicht ändern!)
- 6 Accounts, bcrypt-Hashes in `LoginScreen.ACCOUNTS`. Zuordnung
  (Name → Hash steht direkt daneben im Bytecode): consolecrafter, aaronha3112,
  home, Linusti, EinLuis142, M3l0nByte. Vor Änderungen die Hashes aus dem ALTEN
  Jar extrahieren: `javap -v -p .../gui/LoginScreen.class | grep '$2a$'`.
- Layout responsiv auf Panel-Mitte; `draftName`/`draftPass` sichern die Eingabe
  über `init()` (Fenster-/Vollbild-Wechsel) hinweg. Die Fehlermeldung hat mit
  `errY` reservierten Platz (kein Abschneiden im 854x480-Fenster).
- Passwort maskieren über
  `addFormatter((s,i) -> OrderedText.styledForwardsVisitedString(...))`.

## Keystrokes-Overlay
- `hud/KeystrokesRenderer`, an/aus via `CraftConfig.keystrokes`, Optionen im
  Darstellung -> HUD (Groesse, Maus, Space, CPS-Text). Zeigt WASD, Space, LMB, RMB, MMB;
  gedrückt wird per `GLFW.glfwGetKey`/`glfwGetMouseButton` geprüft. CPS zählt
  Links-/Rechtsklicks der letzten Sekunde (Flankenerkennung). `CraftConfig.accent`
  ist 0xRRGGBB → mit `0xFF000000 |` Alpha ergänzen.
- FPS-Zeile nutzt eine Farbampel (grün ≥120, gelb ≥60, sonst rot).

## Serverliste
- `MultiplayerServerEntryMixin` cancelt `ServerEntry.mouseClicked` bei
  `doubled == true` (Vanilla verbindet sonst bei Doppelklick sofort; der erste
  Klick hat den Eintrag bereits markiert). Verbinden läuft bewusst über den
  "Beitreten"-Button. Intermediary: Ziel `class_4267$class_4270`,
  Methode `method_25402`, Deskriptor `(class_11909, Z, CallbackInfoReturnable)V`
  → **Rückgabemethode braucht `CallbackInfoReturnable<Boolean>`, nicht
  `CallbackInfo`** (sonst `InvalidInjectionException` beim Öffnen des
  Multiplayer-Screens = Crash).

## Client-Brand ("CCraft" erzwingen)
- `ClientBrandMixin` setzt via `@ModifyVariable` im Konstruktor
  `BrandCustomPayload(String)` den Brand bedingungslos auf "CCraft".
- **Wichtig 1.21.10:** Die Referenz-Mod `CustomClientBrandMod` (Mixin auf
  `ServerboundCustomPayloadPacket.write`) funktioniert hier NICHT — in 1.21.10
  gibt es keinen `write`-Pfad mehr, die Brand läuft über `BrandCustomPayload`
  (Record + `CustomPayload.CODEC`). Konstruktor-Interception ist der einzige
  gemeinsame Punkt von Login UND Fremd-Mods (Meteor etc.).
- Konstruktor-Descriptor `(Ljava/lang/String;)V` ist stabil (nur `String`).

## Mixin-Fallgrößen (Lehren aus Crashes)
- Callback-Typ MUSS zum Rückgabetyp der Zielmethode passen:
  `void` → `CallbackInfo`, `boolean/int/...` → `CallbackInfoReturnable<T>`.
- `@Inject`-Handler-Signatur = Ziel-Parameter + CallbackInfo(Returnable),
  statische Ziele → statischer Handler.
- Bei Unsicherheit ZUERST `javap -p -s -c <intermediary>.class` prüfen
  (Descriptor + Methodenname), erst dann den Mixin schreiben.
- **Mixin in ein NICHT-gemapptes Mod (z.B. Sodium):** Loom remappt die
  Referenzen nicht. Zur Laufzeit (Produktion) heißen Intermediary-Methoden
  weiterhin `method_XXXX`, während Dev (Yarn) den Namen nutzt. Deshalb ZWEI
  `@Inject` (einmal Intermediary-Name, einmal Yarn-Name), jeweils
  `require = 0`. Beispiel: `SodiumOptionsEscMixin`.

## Sodium-Optionen & ESC
- Sodiums `SodiumOptionsGUI` blockiert ESC ABSICHTLICH über
  `shouldCloseOnEsc() == !hasPendingChanges` (Intermediary `method_25422`).
  Solange eine Einstellung geändert und nicht angewendet wurde, kommt man
  ohne Maus nicht heraus.
- `SodiumOptionsEscMixin` erzwingt `true` (RETURN, cancellable, require=0),
  damit ESC immer schließt. Der ESC-Pfad ruft weiterhin `close()` auf
  (bevorzugt `prevScreen`), nur der Gate ist aus.

## Gefundene Bugs (behoben)
- `CustomMainMenuScreen.render`: `worldCount`/`serverCount` wurden nur in
  `init()` gesetzt, in `render()` aber gelesen → NPE pro Frame (Log-Spam +
  Heap-Druck → OOM) wenn `init()` nicht/fehlerhaft lief. Jetzt Instanzfelder
  mit Default 0.
- `SnakeScreen.spawnFood`: Endlosschleife (100% CPU-Freeze), wenn die
  Schlange das Feld füllt (kein freies Feld → `occupies` immer true). Jetzt
  Sieg-Erkennung (`gameOver`, `food = null`) + Null-Guards in Update/Render.
- Keystrokes-Overlay: oben rechts (Chat frei), CPS direkt in den LMB/RMB-
  Kästchen, MMB entfernt; W über der Mitte der ASD-Reihe, Space exakt so
  breit wie A+S+D.

## Keystrokes-Layout
- `KeystrokesRenderer`: Breite = `3*key + 2*gap`, Space-Breite = volle
  Grid-Breite, W um `key+gap` eingerückt (mittig über S). Position oben
  rechts: `x = winW - gridW - 4`, `y = 4` (+ `hudX`/`hudY`-Offsets).
  `CraftConfig.accent` ist `0xRRGGBB` → Alpha (`0xFF000000 | …`) ergänzen.

## Cheats-Framework (0.5)
Neues Package `cheats/`, Grundgerüst `Cheat` → `CheatManager` → `impl/*`.
- `Cheat`: `id`/`name`/`description`, `isEnabled()`, `setEnabled()`,
  `toggle()`, `keybind()`, `options()`. Hooks `onEnable()`, `onDisable()`,
  `onRestore()` (Zustand aus Config), `onTick()`, `onHudRender(DrawContext)`,
  `keepOnDisconnect()`, `defaultEnabled()`. Helfer `bool()`, `number()`,
  `choice()`. **Wichtig:** `setEnabled()` schreibt `enabled` in die Config und
  ruft genau dann `onEnable`/`onDisable`; `restore()` (Package-private) setzt
  das Flag beim Start und ruft nur `onRestore()`.
- `Option`: `Kind.BOOLEAN|NUMBER|CHOICE`, Werte werden **direkt** in
  `CheatConfig` gehalten (kein In-Memory-Cache) → `asBoolean/asNumber/asInt/
  asChoice`, `set/cycle/step/reset/display`. `min/max/step/allowed` sind
  Package-private; von aussen `step(int)` benutzen, nicht `min/max` lesen.
- `CheatManager`: `register()` (idempotent, lädt Config, legt Keybinds an),
  `all()`, `byId()`, `setKeybind(Cheat, InputUtil.Key)`, `resetKeybind()`,
  `tick()`, `renderHud()`, `disableAll()`. Tick/HUD fangen `Throwable` pro
  Cheat und deaktivieren den Cheat bei Fehler (verhindert Crash-Loops).
  `CheatConfig` liegt unter `config/ccraft-cheats.properties`.
- **Persistenz-Falle:** `disableAll()` (beim Welt-Verlassen) darf NICHT
  `setEnabled(false)` rufen — das schriebe `enabled=false` in die Config und ein
  Cheat koennte nie beim Start aktiv sein. Es gibt dafuer `disableInMemory()`
  (nur Speicher) und `CheatManager.restoreSaved()` (beim JOIN), das den
  gespeicherten Zustand wiederherstellt.
- **Versionsanzeige:** nie eine Version fest eintragen. Immer
  `util/ModInfo.version()` (nackt, z. B. `0.5`) bzw. `ModInfo.display()` (`c0.5`)
  benutzen; die Werte kommen aus `fabric.mod.json`, das Loom aus
  `gradle.properties` (`mod_version`) befuellt.
- `CraftConfig.load()` liest jeden Schluessel EINZELN (`readBool/readInt/
  readFloat`). Ein kaputter Wert darf nur sein eigenes Feld auf Default setzen,
  nicht die restliche Datei abbrechen — sonst ueberschreibt der naechste
  `save()` alle folgenden Felder mit Defaults.

- **Keybinds:** Tasten werden als **Translation-Key** (`key.keyboard.g`)
  gespeichert, nicht als GLFW-Code — dadurch bleiben Belegungen nach
  MC-Updates gültig. `InputUtil.fromTranslationKey()` wirft bei unbekannter
  Taste eine `IllegalArgumentException` → `resolveKey()` fängt das ab.
  Alle Cheats und das Menü nutzen die gemeinsame Kategorie
  `CheatManager.CATEGORY` (`ccraft:cheats`).
- Registrierte Cheats: `AimEr` (Spieler **immer** vor Mobs, Schläge bleiben
  manuell), `Flight` (Modi `Kreativ`/`Vanilla-Hack`, Anti-Kick, sichert und
  stellt `allowFlying/flying/flySpeed/noGravity` wieder her), `Speed`
  (skaliert `velocity` horizontal), `NoFall` (Paket- oder Boden-Flag-Modus),
  `Scaffold` (platziert unter dem Spieler, `tower`-Option), `FastPlace`,
  `Fullbright` (setzt `CraftConfig.fullbright`; Wirkung über
  `FullbrightMixin` + Nachtsicht in `CCraftMod`; `keepOnDisconnect()` = true),
  `PlayerEsp`, `ChestEsp`.

### Ein Menü statt zwei
`gui/CommandMenuScreen` (Rechter Shift) ist der **einzige** Einstieg für
Cheats. Die Kategorie `Cheats` listet jeden Cheat flach auf; `CheatsScreen`
wurde entfernt. `CCraftSettingsScreen` öffnet das Menü über
`CommandMenuScreen.onCheats()`.

- **Zeilenaufbau eines Cheats:** `▸`/`▾` (Optionen ein-/ausklappen), Name +
  `AN/AUS` (Klick schaltet um), Tasten-Schaltfläche, `↺` (Taste zurücksetzen),
  `⟲` (Optionen zurücksetzen). Eingeklappt zeigt ein Cheat nur diese eine Zeile.
  Aufgeklappte Cheats liegen in der statischen Menge `EXPANDED` und bleiben
  über Menü-Sitzungen erhalten.
- **Tastenbelegung:** Klick auf die Tasten-Schaltfläche setzt `capturing`; die
  nächste Taste (`keyPressed`, ESC bricht ab) oder der nächste Mausklick
  (`mouseClicked`) wird übernommen. `charTyped` muss währenddessen `true`
  liefern, sonst landet das Zeichen zusätzlich im Suchfeld.
- **Konflikte:** `CheatManager.hasKeybindConflict()` vergleicht die Taste mit
  den übrigen Cheats, dem Menü-Keybind und den Vanilla-Keybinds. Bei Konflikt
  wird die Schaltfläche rot (`§c`) beschriftet. **Wichtig:** Fabric hängt die
  eigenen Keybinds ebenfalls in `options.allKeys`; ohne den Filter
  `getId().startsWith("key.ccraft.")` meldet sich jeder Cheat mit sich selbst.
- **Suche:** Textfeld über der Liste filtert über `CheatManager.search()`
  (Name, Id, Beschreibung). `TextFieldWidget.setText()` löst den
  Changed-Listener **nicht** aus, deshalb ist das Wiederherstellen des Textes
  beim Neuaufbau unkritisch.
- **Scrollen:** `scrollOffset` ist statisch; Zeilen werden nur erzeugt, wenn sie
  sichtbar sind. `mouseScrolled` klemmt auf `maxScrollOffset`.
- `renderBackground` wird überschrieben (transluzenter Verlauf), damit das
  Spiel sichtbar bleibt; `shouldPause()` = false.
- `close()` ruft `CCraftMod.closeCommandMenu()`, weil das statische Flag
  `menuOpen` sonst stehen bleibt und Rechts-Shift zweimal gebraucht wird.

### HUD-Liste der aktiven Cheats
`hud/ActiveCheatsHud` zeichnet die aktiven Cheats rechts oben (ohne
Hintergrund). Sie liest nur `CheatManager.all()` und verschwindet, sobald kein
Cheat läuft; bei `hudHidden` wird sie übersprungen. Cheats können über
`Cheat.statusText()` einen kurzen Zusatz in ihrer Menüzeile anzeigen.


### Cheat-Mixins
- `FastPlaceMixin` (`MinecraftClient`): `@Shadow private int itemUseCooldown;`
  + `@Inject(method="doItemUse()V", at=@At("TAIL"))` → `itemUseCooldown =
  FastPlace.cooldown(...)`. Der Name `itemUseCooldown` steht **im
  Zielklassenrumpf** → `@Shadow` ist zulässig (nicht geerbt!).
- `OutlineColorMixin` (`EntityRenderer`): `@Inject(method="updateRenderState",
  at=@At("TAIL"))`, setzt `state.outlineColor = PlayerEsp.glowColor(entity)`.
  Vanilla setzt den Umriss aus der Teamfarbe und zeichnet ihn **durch Wände**
  → das ist der Wallhack-Pfad. `EntityRenderState.outlineColor` ist public.
- `PlayerEsp` ruft zusätzlich `entity.setGlowing(true)` und nimmt das beim
  Deaktivieren/Disconnect wieder zurück (`onDisable`).

### ESP-Rendering
- Kein eigener Render-Pipeline-Import nötig: 2D-Kästen werden über
  `HudRenderCallback` + `GameRenderer.project(Vec3d)` gezeichnet (NDC → Bild-
  schirmkoordinaten; `ndc.z` ausserhalb ±1 ⇒ hinter der Kamera ⇒ skippen).
  Helfer in `impl/EspRender` (`project`, `projectBox`, `drawBoxOutline`,
  `withAlpha`).
- **Kein `RenderSystem.disableDepthTest()` in 1.21.10** — RenderSystem hat
  keine Depth-Toggles mehr; durch Wände geht nur der Glow-Umriss
  (`outlineColor`), nicht ein selbstgebauter Layer.

### Cheats im UI
- `gui/CommandMenuScreen` (Kategorie `Cheats`): pro Cheat eine Zeile mit
  An/Aus, Taste und Aufklapp-Pfeil; die Optionen erscheinen darunter als
  automatisch generierte Widgets (BOOLEAN/CHOICE als Toggle-Button, NUMBER als
  `-`/Wert/`+`). Neue Cheats brauchen hier **keine** Anpassung. Ein separates
  `CheatsScreen` gibt es nicht mehr.

### AntiParticleCrash
- `ParticlePacketMixin` (`ClientPlayNetworkHandler`): `@Inject` auf
  `onParticle(Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;)V` mit
  `at=@At("HEAD")` und `cancellable = true`. Der Kopf ist wichtig: Wird das
  Paket verworfen, **bevor** Partikel entstehen, ist der Speicher nie belegt.
  Ein Aufräumen nachträglich wäre zu spät.
- Der Mixin holt den Cheat über `CheatManager.antiParticleCrash()`, nicht per
  Feld — der Netzwerk-Handler kann vor `CheatManager.register()` laufen.
- **Schwellwerte:** nicht zu streng setzen. Ein Paket mit vielen Partikeln ist
  normal (Feuerwerk: ~100 Partikel, Streuung ~0.5); gefährlich ist erst die
  Kombination aus >2000 Partikeln (Ash-Crash schickt Millionen) oder >100
  Partikeln mit >16 Blöcken Streuung.
- `defaultEnabled()` = true und `keepOnDisconnect()` = true: der Cheat schützt
  nur, und `disableAll()` würde ihn sonst nach dem ersten Serverwechsel
  dauerhaft ausschalten.

- `CCraftMod` registriert `CheatManager.register()` beim Start,
  `CheatManager.tick()` im `END_CLIENT_TICK`, `CheatManager.renderHud()` und
  `ActiveCheatsHud.render()` im `HudRenderCallback` und `disableAll()` bei
  `DISCONNECT`.
- Keybinds: `key.ccraft.cheat.<id>` in der eigenen Kategorie
  `CheatManager.CATEGORY` (`ccraft:cheats`); Übersetzungen in
  `assets/ccraft/lang/en_us.json` + `de_de.json`. Belegung geht sowohl in den
  normalen Minecraft-Steuerungseinstellungen als auch direkt im Menü.


## Keybind-Persistenz (Root Cause, gelernt)
Fabric-Entrypoint (`CCraftMod.onInitializeClient` → `CheatManager.register()`)
läuft im `MinecraftClient`-Konstruktor **vor** `new GameOptions(...)`. Vanilla
liest `options.txt` erst dort. Ablauf:

1. `CheatManager.register()` registriert die Keybinds bei Fabric
   (`KeyBindingHelper.registerKeyBinding`). Sie landen in einer Liste, die
   Fabric per `GameOptionsMixin.loadHook` (Inject `HEAD` in `GameOptions.load`,
   intermediary `method_1636`) an `GameOptions.allKeys` anhängt.
   → `allKeys` existiert erst **nach** dem Laden.
2. `GameOptions.load()` → `accept(Visitor)`: pro `allKeys`-Eintrag
   `visitString("key_<id>", getBoundKeyTranslationKey())`, bei Abweichung
   `setBoundKey(...)`.
3. `MinecraftClient.tick()` schreibt am Ende `options.write()`.

Konsequenz: Ein nur in `ccraft-cheats.properties` gespeicherter Keybind ist nach
dem Neustart weg, weil Vanilla ihn beim Laden aus `options.txt` überschreibt.
Fix (in `CheatManager`): Belegung als **Translation-Key** speichern
(`InputUtil.Key.getTranslationKey()`, z. B. `key.keyboard.g` — stabil über
MC-Updates), bei `setKeybind()` sofort `KeyBinding.setBoundKey()` +
`KeyBinding.updateKeysByCode()` + `options.write()`, und einmalig nach dem
Options-Load `restoreKeybinds()` (in `CCraftMod`, erster `END_CLIENT_TICK`).
`restoreKeybinds()` fasst nur **unbelegte** Keybinds an — eine bewusst im
Vanilla-Menü gesetzte Taste bleibt unangetastet.

## NameProtect (Chat + Tabliste)
- Nicht den fertigen String ersetzen — das killt die Formatierung. Stattdessen
  den `Text`-Baum rekursiv über `getContent()`/`getSiblings()` neu aufbauen
  (`MutableText.of(...)`, `setStyle(...)`, `append(...)`), `PlainTextContent`
  und `TranslatableTextContent` (Args können `Text` oder `String` sein) einzeln
  behandeln.
- **Chat:** Der Ein-Arg-`ChatHud.addMessage(Text)` delegiert nur an die 3-Arg-
  Überladung. Spieler-Chat aus dem Netz läuft über `MessageHandler` →
  `addMessage(Text, MessageSignatureData, MessageIndicator)` (intermediary
  `method_44811`). Genau dort `@ModifyVariable(HEAD, argsOnly)` — sonst greift
  der Hook nicht.
- **Tabliste:** `PlayerListHud.getPlayerName(PlayerListEntry)` (intermediary
  `method_1918`), `@Inject(RETURN, cancellable)`; nur den eigenen Eintrag ändern
  (UUID-Vergleich, Fallback Name).
- `GameProfile` ist in authlib 7.x ein **Record**: `id()` / `name()`, nicht
  `getId()` / `getName()`.

## AutoTotem
- Inventar nur über `ClientPlayerInteractionManager.clickSlot(...)` mit
  `SlotActionType.SWAP` und Button `PlayerInventory.OFF_HAND_SLOT` (40) anfassen
  — direkte Schreibzugriffe springen beim nächsten Server-Sync zurück.
- `clickSlot` prüft den `syncId` gegen `player.currentScreenHandler` und
  ignoriert sonst stumm. Deshalb die Slot-Id immer mit
  `handler.getSlotIndex(player.getInventory(), invIndex)` holen, nicht die
  Screen-Slot-Konstanten hart verdrahten.
- `getHealth()` liefert HP 0..20 (nicht Herzen).

