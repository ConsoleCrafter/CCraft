package de.consolecrafter.ccraft.feature;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.consolecrafter.ccraft.util.Obf;

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

public final class AccountStore {

    private static final String URL_TOKEN =
            "CxcGERVOAhYBWBAXXAYPAEVMBEQQBgACCRpZXAhFTQAdDEk3QlcVXg8GMRMHEllcFB4HUxZUAEwcDgdUBlFFVQdHFVoHVQFWF1IFRRUMUAgGUV0TBwMCWgVDAgUGAAUXXhcMQgwN";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private AccountStore() {}

    public static CompletableFuture<Boolean> verify(String username, char[] password) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(Obf.s(URL_TOKEN)))
                .timeout(Duration.ofSeconds(5))
                .header("Cache-Control", "no-cache")
                .GET()
                .build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    if (response.statusCode() != 200) {
                        throw new CompletionException(new IOException("HTTP " + response.statusCode()));
                    }
                    Map<String, String> accounts = GSON.fromJson(response.body(), MAP_TYPE);
                    String hash = accounts == null ? null : accounts.get(username);
                    return hash != null && BCrypt.verifyer().verify(password, hash).verified;
                });
    }
}
