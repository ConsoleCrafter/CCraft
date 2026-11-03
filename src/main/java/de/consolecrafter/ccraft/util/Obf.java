package de.consolecrafter.ccraft.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Obf {

    private static final byte[] KEY = "ccraft-9f1".getBytes(StandardCharsets.UTF_8);

    private Obf() {}

    public static String s(String encoded) {
        return new String(xor(Base64.getDecoder().decode(encoded)), StandardCharsets.UTF_8);
    }

    public static String encode(String plain) {
        return Base64.getEncoder().encodeToString(xor(plain.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] xor(byte[] data) {
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ KEY[i % KEY.length]);
        }
        return out;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: Obf <plain-text>");
            return;
        }
        System.out.println(encode(args[0]));
    }
}
