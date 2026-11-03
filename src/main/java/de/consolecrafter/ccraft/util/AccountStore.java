package de.consolecrafter.ccraft.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Lädt die bcrypt-Hashes (Name -> Hash) als JSON aus dem GitHub Gist. */
public final class AccountStore {
    // Ohne Commit-Hash im Pfad: zeigt immer auf die neueste Version des Gists.
    // Verschlüsselt hinterlegt, damit die URL nicht als Klartext im Jar steht
    // (siehe Obf.java). Mit Obf.encode(...) neu erzeugen, falls sich die
    // Gist-URL mal ändert.
    private static final String URL_TOKEN =
            "CxcGERVOAhYBWBAXXAYPAEVMBEQQBgACCRpZXAhFTQAdDEk3QlcVXg8GMRMHEllcFB4HUxZUAEwcDgdUBlFFVQdHFVoHVQFWF1IFRRUMUAgGUV0TBwMCWgVDAgUGAAUXXhcMQgwN";
    private static final URI URL = URI.create(Obf.s(URL_TOKEN));

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private AccountStore() {}

    public static CompletableFuture<Map<String, String>> fetch() {
        HttpRequest req = HttpRequest.newBuilder(URL)
                .timeout(Duration.ofSeconds(5))
                .header("Cache-Control", "no-cache")
                .GET().build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {
                    if (r.statusCode() != 200) {
                        throw new CompletionException(new IOException("HTTP " + r.statusCode()));
                    }
                    Map<String, String> map = GSON.fromJson(r.body(), MAP_TYPE);
                    if (map == null) {
                        throw new CompletionException(new IOException("Leere Antwort"));
                    }
                    return map;
                });
    }
}
