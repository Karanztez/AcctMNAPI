package AcctAPI.handler;

import AcctAPI.api.ApiHandler;
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

public class ReflectionApiHandler implements ApiHandler {

    private final JavaPlugin plugin;
    private final Plugin acctMNPlugin;

    public ReflectionApiHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.acctMNPlugin = Bukkit.getPluginManager().getPlugin("AcctMN");
        if (this.acctMNPlugin == null) {
            plugin.getLogger().severe("AcctMN plugin not found! The AcctAPI will not function in Reflection mode.");
        }
    }

    private void logReflectionError(Exception e) {
        plugin.getLogger().log(Level.SEVERE, "A reflection error occurred in AcctAPI. Please ensure AcctMN is up to date.", e);
    }

    private Object getAcctMNManager(String managerName) {
        if (acctMNPlugin == null) return null;
        try {
            Method getManagerMethod = acctMNPlugin.getClass().getMethod("get" + managerName);
            return getManagerMethod.invoke(acctMNPlugin);
        } catch (Exception e) {
            logReflectionError(e);
            return null;
        }
    }

    private Object getDatabaseManager() {
        return getAcctMNManager("DatabaseManager");
    }

    private Object getSessionManager() {
        return getAcctMNManager("SessionManager");
    }

    private Object getLangManager() {
        return getAcctMNManager("Lang");
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            Object dbManager = getDatabaseManager();
            if (dbManager == null) return false;
            try {
                Method isRegisteredMethod = dbManager.getClass().getMethod("isRegistered", String.class);
                return (boolean) isRegisteredMethod.invoke(dbManager, playerName);
            } catch (Exception e) {
                logReflectionError(e);
                return false;
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            Object dbManager = getDatabaseManager();
            if (dbManager == null) return Optional.empty();
            try {
                Method getAccountMethod = dbManager.getClass().getMethod("getPlayerAccount", String.class);
                return (Optional<PlayerAccount>) getAccountMethod.invoke(dbManager, playerName);
            } catch (Exception e) {
                logReflectionError(e);
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isAuthenticated(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
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
        });
    }

    @Override
    public CompletableFuture<Void> forceChangePassword(String playerName, String newPassword) {
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

    @Override
    public CompletableFuture<Void> forceDeleteAccount(String playerName) {
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

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Optional<UUID>> getUuidByDiscordId(String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            Object dbManager = getDatabaseManager();
            if (dbManager == null) {
                plugin.getLogger().warning("AcctMNAPI could not get DatabaseManager. Is AcctMN loaded?");
                return Optional.empty();
            }

            try {
                Class<?> dbManagerInterface;
                try {
                    dbManagerInterface = Class.forName("acct.database.DatabaseManager");
                } catch (ClassNotFoundException e) {
                    dbManagerInterface = dbManager.getClass();
                }

                Method getPlayerUuidMethod = dbManagerInterface.getMethod("getPlayerUUID", String.class);
                Object result = getPlayerUuidMethod.invoke(dbManager, discordId);

                if (result instanceof UUID) {
                    return Optional.of((UUID) result);
                } else if (result instanceof Optional) {
                    return (Optional<UUID>) result;
                }
                
                return Optional.empty();

            } catch (Exception e) {
                logReflectionError(e);
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<String>> getDiscordId(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            Object dbManager = getDatabaseManager();
            if (dbManager == null) return Optional.empty();
            try {
                Class<?> dbManagerInterface;
                try {
                    dbManagerInterface = Class.forName("acct.database.DatabaseManager");
                } catch (ClassNotFoundException e) {
                    dbManagerInterface = dbManager.getClass();
                }

                Method getDiscordIdMethod = dbManagerInterface.getMethod("getDiscordId", String.class);
                Object result = getDiscordIdMethod.invoke(dbManager, playerName);
                
                if (result instanceof String) {
                    return Optional.of((String) result);
                }
                return Optional.empty();
            } catch (Exception e) {
                logReflectionError(e);
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<String>> getDiscordId(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            Object dbManager = getDatabaseManager();
            if (dbManager == null) return Optional.empty();
            try {
                Class<?> dbManagerInterface;
                try {
                    dbManagerInterface = Class.forName("acct.database.DatabaseManager");
                } catch (ClassNotFoundException e) {
                    dbManagerInterface = dbManager.getClass();
                }

                Method getDiscordIdMethod = dbManagerInterface.getMethod("getDiscordId", UUID.class);
                Object result = getDiscordIdMethod.invoke(dbManager, playerUUID);
                
                if (result instanceof String) {
                    return Optional.of((String) result);
                }
                return Optional.empty();
            } catch (Exception e) {
                logReflectionError(e);
                return Optional.empty();
            }
        });
    }

    @Override
    public void shutdown() {
        // No specific shutdown logic needed for reflection handler
    }
}
