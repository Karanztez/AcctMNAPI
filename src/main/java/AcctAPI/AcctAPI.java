package AcctAPI;

import AcctAPI.api.ApiHandler;
import AcctAPI.api.PlayerAccount;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public final class AcctAPI {

    private static ApiHandler handler;

    private AcctAPI() {}

    public static void setHandler(ApiHandler apiHandler) {
        if (handler != null && handler != apiHandler) {
            System.out.println("[AcctAPI] Warning: AcctAPI handler is being replaced.");
        }
        handler = apiHandler;
    }

    public static void shutdown() {
        if (handler != null) {
            handler.shutdown();
            handler = null;
        }
    }

    private static ApiHandler getHandler() {
        if (handler == null) {
            throw new IllegalStateException("AcctAPI handler has not been initialized. Is AcctVelocity or AcctMN running and loaded correctly?");
        }
        return handler;
    }

    public static CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(String playerName) { return getHandler().getPlayerAccount(playerName); }
    public static CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(UUID uuid) { return getHandler().getPlayerAccount(uuid); }

    public static CompletableFuture<Boolean> isRegistered(String playerName) { return getHandler().isRegistered(playerName); }
    public static CompletableFuture<Boolean> isRegistered(UUID uuid) { return getHandler().isRegistered(uuid); }

    public static CompletableFuture<Boolean> isAuthenticated(UUID uuid) { return getHandler().isAuthenticated(uuid); }

    public static CompletableFuture<Optional<UUID>> getUuidByDiscordId(String discordId) { return getHandler().getUuidByDiscordId(discordId); }
    public static CompletableFuture<Optional<String>> getDiscordId(String playerName) { return getHandler().getDiscordId(playerName); }
    public static CompletableFuture<Optional<String>> getDiscordId(UUID playerUUID) { return getHandler().getDiscordId(playerUUID); }

    public static CompletableFuture<Void> forceChangePassword(String playerName, String newPassword) { return getHandler().forceChangePassword(playerName, newPassword); }
    public static CompletableFuture<Void> forceChangePassword(UUID uuid, String newPassword) { return getHandler().forceChangePassword(uuid, newPassword); }

    public static CompletableFuture<Void> forceDeleteAccount(String playerName) { return getHandler().forceDeleteAccount(playerName); }
    public static CompletableFuture<Void> forceDeleteAccount(UUID uuid) { return getHandler().forceDeleteAccount(uuid); }
}