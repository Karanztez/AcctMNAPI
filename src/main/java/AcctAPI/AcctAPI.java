package AcctAPI;

import AcctAPI.api.ApiHandler;
import AcctAPI.api.PlayerAccount;
import AcctAPI.handler.ProxyApiHandler;
import AcctAPI.handler.ReflectionApiHandler;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * API for external developers to access AcctMN data.
 */
@SuppressWarnings("unused")
public final class AcctAPI extends JavaPlugin {

    private static ApiHandler handler;
    private static AcctAPI instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        String mode = getConfig().getString("integration-mode", "reflection");

        if (mode.equalsIgnoreCase("proxy")) {
            handler = new ProxyApiHandler(this);
            getLogger().info("AcctAPI is running in PROXY mode.");
        } else {
            handler = new ReflectionApiHandler(this);
            getLogger().info("AcctAPI is running in REFLECTION mode.");
        }
    }

    @Override
    public void onDisable() {
        if (handler != null) {
            handler.shutdown();
        }
        handler = null;
        instance = null;
    }

    public static AcctAPI getInstance() {
        return instance;
    }

    public static CompletableFuture<Boolean> isRegistered(String playerName) {
        return handler.isRegistered(playerName);
    }

    public static CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(String playerName) {
        return handler.getPlayerAccount(playerName);
    }

    public static CompletableFuture<Boolean> isAuthenticated(UUID uuid) {
        return handler.isAuthenticated(uuid);
    }

    public static CompletableFuture<Void> forceChangePassword(String playerName, String newPassword) {
        return handler.forceChangePassword(playerName, newPassword);
    }

    public static CompletableFuture<Void> forceDeleteAccount(String playerName) {
        return handler.forceDeleteAccount(playerName);
    }

    public static CompletableFuture<Optional<UUID>> getUuidByDiscordId(String discordId) {
        return handler.getUuidByDiscordId(discordId);
    }

    public static CompletableFuture<Optional<String>> getDiscordId(String playerName) {
        return handler.getDiscordId(playerName);
    }

    public static CompletableFuture<Optional<String>> getDiscordId(UUID playerUUID) {
        return handler.getDiscordId(playerUUID);
    }
}
