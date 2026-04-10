package AcctAPI.handler;

import AcctAPI.AcctAPI;
import AcctAPI.api.ApiHandler;
import AcctAPI.api.PlayerAccount;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

    private final AcctAPI plugin;
    private final String CHANNEL = "acct:api";
    private final Map<String, CompletableFuture<?>> pendingRequests = new ConcurrentHashMap<>();

    public ProxyApiHandler(AcctAPI plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    // Suppressing "unchecked" for the CompletableFuture cast, which is a valid pattern here.
    // Suppressing "ConstantConditions" or similar false positives for the @NotNull on byte[].
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals(CHANNEL)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        String requestId = in.readUTF();

        // Use .get() instead of .remove() so the whenComplete block can handle removal
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
            // forceChangePassword and forceDeleteAccount are fire-and-forget, no response needed.
        }
    }

    // --- Request Sending Methods ---

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

        // Return a new future with timeout and cleanup logic
        return future
            .orTimeout(5, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                // This block always executes, ensuring we remove the pending request
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
        // No requestId needed for fire-and-forget
        for (String arg : args) {
            out.writeUTF(arg);
        }
        
        sender.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
        future.complete(null); // Complete immediately
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

    // --- Response Handling Methods ---

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
        // Clear any pending requests and complete them exceptionally to prevent plugins from hanging
        pendingRequests.forEach((id, future) -> future.completeExceptionally(new IllegalStateException("AcctAPI is shutting down.")));
        pendingRequests.clear();
    }
}
