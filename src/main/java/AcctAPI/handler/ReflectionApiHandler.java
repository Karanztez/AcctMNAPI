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

@SuppressWarnings("unused")
public class ReflectionApiHandler implements ApiHandler {

    private final JavaPlugin plugin;
    private final Plugin acctMNPlugin;

    public ReflectionApiHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.acctMNPlugin = Bukkit.getPluginManager().getPlugin("AcctMN");
        if (this.acctMNPlugin == null) {
            plugin.getLogger().severe("AcctMN plugin not found! Reflection mode disabled.");
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

    private Object getDatabaseManager() { return getAcctMNManager("DatabaseManager"); }
    private Object getSessionManager() { return getAcctMNManager("SessionManager"); }
    private Object getLangManager() { return getAcctMNManager("Lang"); }

    @Override
    public CompletableFuture<Boolean> isRegistered(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return false;
            try {
                Method m = db.getClass().getMethod("isRegistered", String.class);
                return (boolean) m.invoke(db, playerName);
            } catch (Exception e) { return false; }
        });
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return false;
            try {
                Method m = db.getClass().getMethod("isRegistered", UUID.class);
                return (boolean) m.invoke(db, uuid);
            } catch (Exception e) { return false; }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return Optional.empty();
            try {
                Method m = db.getClass().getMethod("getPlayerAccount", String.class);
                return (Optional<PlayerAccount>) m.invoke(db, playerName);
            } catch (Exception e) { return Optional.empty(); }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return Optional.empty();
            try {
                Method m = db.getClass().getMethod("getPlayerAccount", UUID.class);
                return (Optional<PlayerAccount>) m.invoke(db, uuid);
            } catch (Exception e) { return Optional.empty(); }
        });
    }

    @Override
    public CompletableFuture<Boolean> isAuthenticated(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) return false;
            Object sm = getSessionManager();
            if (sm == null) return false;
            try {
                Method m = sm.getClass().getMethod("isPending", Player.class);
                return !(boolean) m.invoke(sm, player);
            } catch (Exception e) { return false; }
        });
    }

    @Override
    public CompletableFuture<Void> forceChangePassword(String playerName, String newPassword) {
        return CompletableFuture.runAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return;
            try {
                Method m = db.getClass().getMethod("updatePassword", String.class, String.class);
                m.invoke(db, playerName, newPassword);
            } catch (Exception e) { logReflectionError(e); }
        });
    }

    @Override
    public CompletableFuture<Void> forceChangePassword(UUID uuid, String newPassword) {
        return CompletableFuture.runAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return;
            try {
                Method m = db.getClass().getMethod("updatePassword", UUID.class, String.class);
                m.invoke(db, uuid, newPassword);
            } catch (Exception e) { logReflectionError(e); }
        });
    }

    @Override
    public CompletableFuture<Void> forceDeleteAccount(String playerName) {
        return CompletableFuture.runAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return;
            try {
                Method isReg = db.getClass().getMethod("isRegistered", String.class);
                if ((boolean) isReg.invoke(db, playerName)) {
                    kickPlayerIfOnline(playerName);
                    Method del = db.getClass().getMethod("deleteAccount", String.class);
                    del.invoke(db, playerName);
                }
            } catch (Exception e) { logReflectionError(e); }
        });
    }

    @Override
    public CompletableFuture<Void> forceDeleteAccount(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return;
            try {
                Method isReg = db.getClass().getMethod("isRegistered", UUID.class);
                if ((boolean) isReg.invoke(db, uuid)) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) kickPlayerIfOnline(p.getName());
                    Method del = db.getClass().getMethod("deleteAccount", UUID.class);
                    del.invoke(db, uuid);
                }
            } catch (Exception e) { logReflectionError(e); }
        });
    }

    private void kickPlayerIfOnline(String playerName) {
        getPlayerAccount(playerName).thenAccept(opt -> opt.ifPresent(acc -> {
            Player player = Bukkit.getPlayer(acc.uuid());
            if (player != null) {
                Object lang = getLangManager();
                if (lang == null) return;
                try {
                    Method getMsg = lang.getClass().getMethod("get", String.class);
                    String msg = (String) getMsg.invoke(lang, "unregister.kick_message");
                    final Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
                    Bukkit.getScheduler().runTask(acctMNPlugin, () -> player.kick(component));
                } catch (Exception e) { logReflectionError(e); }
            }
        }));
    }

    @Override
    public CompletableFuture<Optional<UUID>> getUuidByDiscordId(String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return Optional.empty();
            try {
                Method m = db.getClass().getMethod("getPlayerUUID", String.class);
                Object res = m.invoke(db, discordId);
                return res instanceof UUID ? Optional.of((UUID) res) : Optional.empty();
            } catch (Exception e) { return Optional.empty(); }
        });
    }

    @Override
    public CompletableFuture<Optional<String>> getDiscordId(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return Optional.empty();
            try {
                Method m = db.getClass().getMethod("getDiscordId", String.class);
                return Optional.ofNullable((String) m.invoke(db, playerName));
            } catch (Exception e) { return Optional.empty(); }
        });
    }

    @Override
    public CompletableFuture<Optional<String>> getDiscordId(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return Optional.empty();
            try {
                Method m = db.getClass().getMethod("getDiscordId", UUID.class);
                return Optional.ofNullable((String) m.invoke(db, playerUUID));
            } catch (Exception e) { return Optional.empty(); }
        });
    }

    @Override
    public CompletableFuture<Optional<String>> getXuid(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return Optional.empty();
            try {
                Method m = db.getClass().getMethod("getXuid", String.class);
                return Optional.ofNullable((String) m.invoke(db, playerName));
            } catch (Exception e) { return Optional.empty(); }
        });
    }

    @Override
    public CompletableFuture<Optional<String>> getXuid(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            Object db = getDatabaseManager();
            if (db == null) return Optional.empty();
            try {
                Method m = db.getClass().getMethod("getXuid", UUID.class);
                return Optional.ofNullable((String) m.invoke(db, playerUUID));
            } catch (Exception e) { return Optional.empty(); }
        });
    }

    @Override
    public void shutdown() {}
}