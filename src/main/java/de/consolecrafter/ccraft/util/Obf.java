package de.consolecrafter.ccraft.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Sehr einfache Laufzeit-Entschlüsselung für Strings, die nicht als
 * Klartext im Bytecode stehen sollen (z.B. die Gist-URL).
 *
 * WICHTIG: Das ist Verschleierung, keine echte Verschlüsselung.
 * Der Schlüssel liegt im selben Jar, wer den Code deobfuskiert, kommt
 * an den Klartext. Es verhindert nur das stumpfe Auffinden per
 * "strings mod.jar" oder beim flüchtigen Blick in JD-GUI.
 *
 * Benutzung:
 *   1. encode("https://...") einmalig offline aufrufen (siehe main())
 *   2. Das Ergebnis als Konstante im Code hinterlegen
 *   3. Im Code mit Obf.s("...") wieder entschlüsseln
 */
public final class Obf {
    // Schlüssel beliebig änderbar, muss nur zu encode() passen, mit dem
    // du deine Konstanten erzeugt hast.
    private static final byte[] KEY = "ccraft-9f1".getBytes(StandardCharsets.UTF_8);

    private Obf() {}

    /** Entschlüsselt einen mit encode() erzeugten String. */
    public static String s(String encoded) {
        byte[] data = Base64.getDecoder().decode(encoded);
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ KEY[i % KEY.length]);
        }
        return new String(out, StandardCharsets.UTF_8);
    }

    /** Verschlüsselt einen Klartext-String zu einem Obf.s()-kompatiblen Token. */
    public static String encode(String plain) {
        byte[] data = plain.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ KEY[i % KEY.length]);
        }
        return Base64.getEncoder().encodeToString(out);
    }

    /**
     * Nur zum lokalen Erzeugen der verschlüsselten Konstanten, z.B.:
     *   java -cp build/classes/java/main de.consolecrafter.ccraft.util.Obf "https://..."
     *
     * <p>WICHTIG: Die URL darf hier NICHT als Literal stehen. Java-String-
     * Literale landen im Konstantenpool des Bytecode und sind damit per
     * {@code strings mod.jar} lesbar — genau das, was diese Klasse verhindern
     * soll. Die URL wird deshalb als Argument uebergeben und das Ergebnis
     * zurueck in {@link AccountStore} eingetragen.</p>
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Aufruf: Obf <klartext-url>");
            System.err.println("Das Ergebnis in AccountStore.URL_TOKEN eintragen.");
            return;
        }
        System.out.println(encode(args[0]));
    }
}
