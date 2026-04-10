package AcctAPI.handler;

import AcctAPI.api.ApiHandler;
import AcctAPI.api.PlayerAccount;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProxyApiHandler implements ApiHandler, PluginMessageListener {

    private final JavaPlugin plugin; // Changed from AcctAPI to JavaPlugin
    private final String CHANNEL = "acct:api";
    private final Map<String, CompletableFuture<?>> pendingRequests = new ConcurrentHashMap<>();

    public ProxyApiHandler(JavaPlugin plugin) { // Changed constructor
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (message == null) {
            return;
        }

        if (!channel.equals(CHANNEL)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        String requestId = in.readUTF();

        CompletableFuture<?> future = pendingRequests.get(requestId);
        if (future == null) return;

        switch (subChannel) {
            case "isRegistered":
                handleIsRegistered(in, (CompletableFuture<Boolean>) future);
                break;
            case "getPlayerAccount":
                handleGetPlayerAccount(in, (CompletableFuture<Optional<PlayerAccount>>) future);
                break;
            case "isAuthenticated":
                handleIsAuthenticated(in, (CompletableFuture<Boolean>) future);
                break;
            case "getUuidByDiscordId":
                handleGetUuidByDiscordId(in, (CompletableFuture<Optional<UUID>>) future);
                break;
            case "getDiscordId":
            case "getDiscordIdByUUID":
                handleGetDiscordId(in, (CompletableFuture<Optional<String>>) future);
                break;
        }
    }

    private <T> CompletableFuture<T> createAndSendRequest(String subChannel, String... args) {
        Player sender = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (sender == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Cannot send plugin message: No players online."));
        }

        final CompletableFuture<T> future = new CompletableFuture<>();
        final String requestId = UUID.randomUUID().toString();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subChannel);
        out.writeUTF(requestId);
        for (String arg : args) {
            out.writeUTF(arg);
        }

        pendingRequests.put(requestId, future);
        sender.sendPluginMessage(plugin, CHANNEL, out.toByteArray());

        return future
                .orTimeout(5, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> {
                    if (pendingRequests.remove(requestId) != null && throwable instanceof TimeoutException) {
                        plugin.getLogger().warning("AcctAPI request '" + subChannel + "' (ID: " + requestId + ") timed out after 5 seconds.");
                    }
                });
    }

    private CompletableFuture<Void> createAndSendFireAndForgetRequest(String subChannel, String... args) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Player sender = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (sender == null) {
            future.completeExceptionally(new IllegalStateException("Cannot send plugin message: No players online."));
            return future;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subChannel);
        for (String arg : args) {
            out.writeUTF(arg);
        }

        sender.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
        future.complete(null);
        return future;
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(String playerName) {
        return createAndSendRequest("isRegistered", playerName);
    }

    @Override
    public CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(String playerName) {
        return createAndSendRequest("getPlayerAccount", playerName);
    }

    @Override
    public CompletableFuture<Boolean> isAuthenticated(UUID uuid) {
        return createAndSendRequest("isAuthenticated", uuid.toString());
    }

    @Override
    public CompletableFuture<Void> forceChangePassword(String playerName, String newPassword) {
        return createAndSendFireAndForgetRequest("forceChangePassword", playerName, newPassword);
    }

    @Override
    public CompletableFuture<Void> forceDeleteAccount(String playerName) {
        return createAndSendFireAndForgetRequest("forceDeleteAccount", playerName);
    }

    @Override
    public CompletableFuture<Optional<UUID>> getUuidByDiscordId(String discordId) {
        return createAndSendRequest("getUuidByDiscordId", discordId);
    }

    @Override
    public CompletableFuture<Optional<String>> getDiscordId(String playerName) {
        return createAndSendRequest("getDiscordId", playerName);
    }

    @Override
    public CompletableFuture<Optional<String>> getDiscordId(UUID playerUUID) {
        return createAndSendRequest("getDiscordIdByUUID", playerUUID.toString());
    }

    private void handleIsRegistered(ByteArrayDataInput in, CompletableFuture<Boolean> future) {
        future.complete(in.readBoolean());
    }

    private void handleGetPlayerAccount(ByteArrayDataInput in, CompletableFuture<Optional<PlayerAccount>> future) {
        boolean present = in.readBoolean();
        if (present) {
            UUID uuid = UUID.fromString(in.readUTF());
            String username = in.readUTF();
            String realName = in.readUTF();
            String ipAddress = in.readUTF();
            long regDate = in.readLong();
            long lastLogin = in.readLong();
            String discordId = in.readUTF();
            String skinName = in.readUTF();
            future.complete(Optional.of(new PlayerAccount(uuid, username, realName, ipAddress, regDate, lastLogin, discordId, skinName)));
        } else {
            future.complete(Optional.empty());
        }
    }

    private void handleIsAuthenticated(ByteArrayDataInput in, CompletableFuture<Boolean> future) {
        future.complete(in.readBoolean());
    }

    private void handleGetUuidByDiscordId(ByteArrayDataInput in, CompletableFuture<Optional<UUID>> future) {
        boolean present = in.readBoolean();
        if (present) {
            future.complete(Optional.of(UUID.fromString(in.readUTF())));
        } else {
            future.complete(Optional.empty());
        }
    }

    private void handleGetDiscordId(ByteArrayDataInput in, CompletableFuture<Optional<String>> future) {
        boolean present = in.readBoolean();
        if (present) {
            future.complete(Optional.of(in.readUTF()));
        } else {
            future.complete(Optional.empty());
        }
    }

    @Override
    public void shutdown() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        pendingRequests.forEach((id, future) -> future.completeExceptionally(new IllegalStateException("AcctAPI is shutting down.")));
        pendingRequests.clear();
    }
}
