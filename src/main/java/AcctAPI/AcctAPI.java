package AcctAPI;

import AcctAPI.api.ApiHandler;
import AcctAPI.api.PlayerAccount;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused") // Suppress "never used" warnings as this is a library
public final class AcctAPI extends JavaPlugin {

    private static ApiHandler handler;

    @Override
    public void onEnable() {
        // This plugin is a library, its onEnable should do nothing.
        // The handler is initialized by the main plugin (e.g., AcctVelocity or AcctMN).
    }

    @Override
    public void onDisable() {
        if (handler != null) {
            handler.shutdown();
            handler = null; // Clear the handler on disable
        }
    }

    /**
     * This method should be called by the main plugin (AcctVelocity/AcctMN) to set the correct handler.
     * @param apiHandler The handler implementation.
     */
    public static void setHandler(ApiHandler apiHandler) {
        if (handler != null && handler != apiHandler) {
            // Log a warning if trying to set a different handler, but allow it.
            // This can happen during reloads.
            System.out.println("[AcctAPI] Warning: AcctAPI handler is being replaced.");
        }
        handler = apiHandler;
    }

    private static ApiHandler getHandler() {
        if (handler == null) {
            throw new IllegalStateException("AcctAPI handler has not been initialized. Is AcctVelocity or AcctMN running and loaded correctly?");
        }
        return handler;
    }

    public static CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(String playerName) {
        return getHandler().getPlayerAccount(playerName);
    }

    public static CompletableFuture<Boolean> isRegistered(String playerName) {
        return getHandler().isRegistered(playerName);
    }

    public static CompletableFuture<Boolean> isAuthenticated(UUID uuid) {
        return getHandler().isAuthenticated(uuid);
    }

    public static CompletableFuture<Optional<UUID>> getUuidByDiscordId(String discordId) {
        return getHandler().getUuidByDiscordId(discordId);
    }

    public static CompletableFuture<Optional<String>> getDiscordId(String playerName) {
        return getHandler().getDiscordId(playerName);
    }

    public static CompletableFuture<Optional<String>> getDiscordId(UUID playerUUID) {
        return getHandler().getDiscordId(playerUUID);
    }

    public static CompletableFuture<Void> forceChangePassword(String playerName, String newPassword) {
        return getHandler().forceChangePassword(playerName, newPassword);
    }

    public static CompletableFuture<Void> forceDeleteAccount(String playerName) {
        return getHandler().forceDeleteAccount(playerName);
    }
}
