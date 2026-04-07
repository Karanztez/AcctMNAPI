package AcctAPI;

import AcctAPI.api.PlayerAccount;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * API for external developers to access AcctMN data.
 */
@SuppressWarnings("unused")
public final class AcctAPI extends JavaPlugin {

    private static Plugin acctMNPlugin;
    private static JavaPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        acctMNPlugin = Bukkit.getPluginManager().getPlugin("AcctMN");
        if (acctMNPlugin == null) {
            getLogger().severe("AcctMN plugin not found! The AcctAPI will not function.");
        }
    }

    @Override
    public void onDisable() {
        acctMNPlugin = null;
        instance = null;
    }

    private static void logReflectionError(Exception e) {
        instance.getLogger().log(Level.SEVERE, "A reflection error occurred in AcctAPI. Please ensure AcctMN is up to date.", e);
    }

    private static Object getAcctMNManager(String managerName) {
        if (acctMNPlugin == null) return null;
        try {
            Method getManagerMethod = acctMNPlugin.getClass().getMethod("get" + managerName);
            return getManagerMethod.invoke(acctMNPlugin);
        } catch (Exception e) {
            logReflectionError(e);
            return null;
        }
    }

    private static Object getDatabaseManager() {
        return getAcctMNManager("DatabaseManager");
    }

    private static Object getSessionManager() {
        return getAcctMNManager("SessionManager");
    }

    private static Object getLangManager() {
        return getAcctMNManager("Lang");
    }

    public static boolean isRegistered(String playerName) {
        Object dbManager = getDatabaseManager();
        if (dbManager == null) return false;
        try {
            Method isRegisteredMethod = dbManager.getClass().getMethod("isRegistered", String.class);
            return (boolean) isRegisteredMethod.invoke(dbManager, playerName);
        } catch (Exception e) {
            logReflectionError(e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            Object dbManager = getDatabaseManager();
            if (dbManager == null) return Optional.empty();
            try {
                Method getAccountMethod = dbManager.getClass().getMethod("getPlayerAccount", String.class);
                // The cast is necessary due to reflection, but we are confident in the return type.
                return (Optional<PlayerAccount>) getAccountMethod.invoke(dbManager, playerName);
            } catch (Exception e) {
                logReflectionError(e);
                return Optional.empty();
            }
        });
    }

    public static boolean isAuthenticated(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return false;
        }
        Object sessionManager = getSessionManager();
        if (sessionManager == null) return false;
        try {
            Method isPendingMethod = sessionManager.getClass().getMethod("isPending", Player.class);
            return !(boolean) isPendingMethod.invoke(sessionManager, player);
        } catch (Exception e) {
            logReflectionError(e);
            return false;
        }
    }

    public static CompletableFuture<Void> forceChangePassword(String playerName, String newPassword) {
        return CompletableFuture.runAsync(() -> {
            Object dbManager = getDatabaseManager();
            if (dbManager == null) return;
            try {
                Method updatePasswordMethod = dbManager.getClass().getMethod("updatePassword", String.class, String.class);
                updatePasswordMethod.invoke(dbManager, playerName, newPassword);
            } catch (Exception e) {
                logReflectionError(e);
            }
        });
    }

    public static CompletableFuture<Void> forceDeleteAccount(String playerName) {
        return CompletableFuture.runAsync(() -> {
            Object dbManager = getDatabaseManager();
            if (dbManager == null) return;

            try {
                Method isRegisteredMethod = dbManager.getClass().getMethod("isRegistered", String.class);
                boolean registered = (boolean) isRegisteredMethod.invoke(dbManager, playerName);

                if (registered) {
                    getPlayerAccount(playerName).thenAccept(opt -> opt.ifPresent(acc -> {
                        Player player = Bukkit.getPlayer(acc.uuid());
                        if (player != null) {
                            Object langManager = getLangManager();
                            if (langManager == null) return;
                            try {
                                Method getLangMethod = langManager.getClass().getMethod("get", String.class);
                                String kickMessage = (String) getLangMethod.invoke(langManager, "unregister.kick_message");
                                final Component kickComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(kickMessage);
                                Bukkit.getScheduler().runTask(acctMNPlugin, () -> player.kick(kickComponent));
                            } catch (Exception e) {
                                logReflectionError(e);
                            }
                        }
                    }));
                    Method deleteAccountMethod = dbManager.getClass().getMethod("deleteAccount", String.class);
                    deleteAccountMethod.invoke(dbManager, playerName);
                }
            } catch (Exception e) {
                logReflectionError(e);
            }
        });
    }

    /**
     * Get player UUID from their linked Discord ID (Async)
     * @param discordId The Discord ID
     * @return CompletableFuture containing Optional with UUID if found
     */
    public static CompletableFuture<Optional<UUID>> getUuidByDiscordId(String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            Object dbManager = getDatabaseManager();
            if (dbManager == null) return Optional.empty();
            
            try {
                // acct.database.DatabaseManager Interface class approach
                Class<?> dbManagerClass = Class.forName("acct.database.DatabaseManager");
                Method getPlayerUUIDMethod = dbManagerClass.getMethod("getPlayerUUID", String.class);
                
                UUID result = (UUID) getPlayerUUIDMethod.invoke(dbManager, discordId);
                return Optional.ofNullable(result);
            } catch (Exception e) {
                logReflectionError(e);
                return Optional.empty();
            }
        });
    }

    /**
     * Get player's linked Discord ID from their username (Async)
     * @param playerName The player's username
     * @return CompletableFuture containing Optional with Discord ID string if found
     */
    public static CompletableFuture<Optional<String>> getDiscordId(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            Object dbManager = getDatabaseManager();
            if (dbManager == null) return Optional.empty();
            try {
                Class<?> dbManagerClass = Class.forName("acct.database.DatabaseManager");
                Method getDiscordIdMethod = dbManagerClass.getMethod("getDiscordId", String.class);
                
                String result = (String) getDiscordIdMethod.invoke(dbManager, playerName);
                return Optional.ofNullable(result);
            } catch (Exception e) {
                logReflectionError(e);
                return Optional.empty();
            }
        });
    }

    /**
     * Get player's linked Discord ID from their UUID (Async)
     * @param playerUUID The player's UUID
     * @return CompletableFuture containing Optional with Discord ID string if found
     */
    public static CompletableFuture<Optional<String>> getDiscordId(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            Object dbManager = getDatabaseManager();
            if (dbManager == null) return Optional.empty();
            try {
                Class<?> dbManagerClass = Class.forName("acct.database.DatabaseManager");
                Method getDiscordIdMethod = dbManagerClass.getMethod("getDiscordId", UUID.class);
                
                String result = (String) getDiscordIdMethod.invoke(dbManager, playerUUID);
                return Optional.ofNullable(result);
            } catch (Exception e) {
                logReflectionError(e);
                return Optional.empty();
            }
        });
    }
}