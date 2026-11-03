package de.consolecrafter.ccraft.cheats.impl;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.CheatManager;
import de.consolecrafter.ccraft.cheats.Option;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

/**
 * NameProtect — ersetzt den eigenen Namen in Chat und Tabliste durch einen
 * Alias, damit der echte Accountname nicht im Stream auftaucht.
 *
 * <p>Die Ersetzung passiert erst beim Zeichnen bzw. beim Einfuegen in die
 * Chatliste. Die echten Pakete bleiben unveraendert, es wird also nur die
 * lokale Anzeige umgeschrieben — nichts davon geht an den Server.</p>
 *
 * <p>Der Baum eines {@link Text} wird rekursiv umgebaut, damit Farben und
 * uebersetzte Bestandteile (z. B. {@code <Spieler> Nachricht}) erhalten
 * bleiben. Ein stumpfes Ersetzen des fertigen Strings wuerde die
 * Formatierung verlieren.</p>
 */
public class NameProtect extends Cheat {

    private final Option alias = text("alias", "Ersatzname", "Spieler");
    private final Option chat = bool("chat", "Im Chat ersetzen", true);
    private final Option tabList = bool("tabList", "In der Tabliste ersetzen", true);

    public NameProtect() {
        super("nameprotect", "NameProtect",
                "Ersetzt den eigenen Namen in Chat und Tabliste (Anti Stream-Snipe).");
    }

    public boolean coversChat() {
        return chat.asBoolean();
    }

    public boolean coversTabList() {
        return tabList.asBoolean();
    }

    /** Ersetzt den eigenen Namen in einem Chat-Text. */
    public static Text protectChat(Text text) {
        NameProtect cheat = active();
        if (cheat == null || !cheat.coversChat()) return text;
        return cheat.transform(text);
    }

    /** Ersetzt den eigenen Namen im Namen eines Tablisten-Eintrags. */
    public static Text protectTabList(PlayerListEntry entry, Text text) {
        NameProtect cheat = active();
        if (cheat == null || !cheat.coversTabList()) return text;
        if (entry == null || !cheat.isSelf(entry)) return text;
        return cheat.transform(text);
    }

    private static NameProtect active() {
        Cheat cheat = CheatManager.byId("nameprotect");
        return cheat instanceof NameProtect np && np.isEnabled() ? np : null;
    }

    /** Ob der Eintrag der lokale Spieler ist. */
    private boolean isSelf(PlayerListEntry entry) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && entry.getProfile().id().equals(client.player.getUuid())) {
            return true;
        }
        // Offline-Accounts haben teils keine passende UUID in der Tabliste.
        return entry.getProfile().name().equals(client.getSession().getUsername());
    }

    private Text transform(Text text) {
        if (text == null) return null;
        String from = MinecraftClient.getInstance().getSession().getUsername();
        String to = alias.asText();
        if (from == null || from.isEmpty() || to == null || to.isEmpty() || from.equals(to)) {
            return text;
        }

        MutableText result = rebuild(text, from, to);
        return result == null ? text : result;
    }

    private static MutableText rebuild(Text text, String from, String to) {
        TextContent content = text.getContent();
        MutableText result;

        if (content instanceof PlainTextContent plain) {
            String original = plain.string();
            String replaced = original.replace(from, to);
            result = MutableText.of(replaced.equals(original)
                    ? plain
                    : PlainTextContent.of(replaced));
        } else if (content instanceof TranslatableTextContent translatable) {
            Object[] args = translatable.getArgs();
            Object[] mapped = new Object[args.length];
            boolean changed = false;
            for (int i = 0; i < args.length; i++) {
                mapped[i] = mapArgument(args[i], from, to);
                if (mapped[i] != args[i]) changed = true;
            }
            result = MutableText.of(changed
                    ? new TranslatableTextContent(translatable.getKey(), translatable.getFallback(), mapped)
                    : translatable);
        } else {
            result = MutableText.of(content);
        }

        result.setStyle(text.getStyle());
        for (Text sibling : text.getSiblings()) {
            result.append(rebuild(sibling, from, to));
        }
        return result;
    }

    private static Object mapArgument(Object arg, String from, String to) {
        if (arg instanceof Text nested) return rebuild(nested, from, to);
        if (arg instanceof String value) return value.replace(from, to);
        return arg;
    }
}
