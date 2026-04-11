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

@SuppressWarnings("unused")
public class ProxyApiHandler implements ApiHandler, PluginMessageListener {

    private final JavaPlugin plugin;
    private final String CHANNEL = "acct:api";
    private final Map<String, CompletableFuture<?>> pendingRequests = new ConcurrentHashMap<>();

    public ProxyApiHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    @SuppressWarnings({"unchecked", "NullableProblems"})
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (message == null || !channel.equals(CHANNEL)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        String requestId = in.readUTF();

        CompletableFuture<?> future = pendingRequests.get(requestId);
        if (future == null) return;

        switch (subChannel) {
            case "isRegistered":
            case "isRegisteredUUID":
            case "isAuthenticated":
                ((CompletableFuture<Boolean>) future).complete(in.readBoolean());
                break;
            case "getPlayerAccount":
            case "getPlayerAccountUUID":
                handleGetPlayerAccount(in, (CompletableFuture<Optional<PlayerAccount>>) future);
                break;
            case "getUuidByDiscordId":
                boolean hasUuid = in.readBoolean();
                ((CompletableFuture<Optional<UUID>>) future).complete(hasUuid ? Optional.of(UUID.fromString(in.readUTF())) : Optional.empty());
                break;
            case "getDiscordId":
            case "getDiscordIdByUUID":
            case "getXuid":
            case "getXuidByUUID":
                boolean hasStr = in.readBoolean();
                ((CompletableFuture<Optional<String>>) future).complete(hasStr ? Optional.of(in.readUTF()) : Optional.empty());
                break;
        }
    }

    private <T> CompletableFuture<T> createAndSendRequest(String subChannel, String... args) {
        Player sender = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (sender == null) return CompletableFuture.failedFuture(new IllegalStateException("No players online."));

        final CompletableFuture<T> future = new CompletableFuture<>();
        final String requestId = UUID.randomUUID().toString();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subChannel);
        out.writeUTF(requestId);
        for (String arg : args) out.writeUTF(arg);

        pendingRequests.put(requestId, future);
        sender.sendPluginMessage(plugin, CHANNEL, out.toByteArray());

        return future.orTimeout(5, TimeUnit.SECONDS).whenComplete((res, ex) -> {
            pendingRequests.remove(requestId);
            if (ex instanceof TimeoutException) {
                plugin.getLogger().warning("AcctAPI: Request " + subChannel + " timed out.");
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(String n) { return createAndSendRequest("isRegistered", n); }
    @Override
    public CompletableFuture<Boolean> isRegistered(UUID u) { return createAndSendRequest("isRegisteredUUID", u.toString()); }
    @Override
    public CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(String n) { return createAndSendRequest("getPlayerAccount", n); }
    @Override
    public CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(UUID u) { return createAndSendRequest("getPlayerAccountUUID", u.toString()); }
    @Override
    public CompletableFuture<Boolean> isAuthenticated(UUID u) { return createAndSendRequest("isAuthenticated", u.toString()); }

    @Override
    public CompletableFuture<Void> forceChangePassword(String n, String p) { return sendFireForget("forceChangePassword", n, p); }
    @Override
    public CompletableFuture<Void> forceChangePassword(UUID u, String p) { return sendFireForget("forceChangePasswordUUID", u.toString(), p); }
    @Override
    public CompletableFuture<Void> forceDeleteAccount(String n) { return sendFireForget("forceDeleteAccount", n); }
    @Override
    public CompletableFuture<Void> forceDeleteAccount(UUID u) { return sendFireForget("forceDeleteAccountUUID", u.toString()); }

    private CompletableFuture<Void> sendFireForget(String sub, String... args) {
        Player p = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (p != null) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(sub);
            for (String a : args) out.writeUTF(a);
            p.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Optional<UUID>> getUuidByDiscordId(String id) { return createAndSendRequest("getUuidByDiscordId", id); }
    @Override
    public CompletableFuture<Optional<String>> getDiscordId(String n) { return createAndSendRequest("getDiscordId", n); }
    @Override
    public CompletableFuture<Optional<String>> getDiscordId(UUID u) { return createAndSendRequest("getDiscordIdByUUID", u.toString()); }
    @Override
    public CompletableFuture<Optional<String>> getXuid(String n) { return createAndSendRequest("getXuid", n); }
    @Override
    public CompletableFuture<Optional<String>> getXuid(UUID u) { return createAndSendRequest("getXuidByUUID", u.toString()); }

    private void handleGetPlayerAccount(ByteArrayDataInput in, CompletableFuture<Optional<PlayerAccount>> future) {
        if (in.readBoolean()) {
            future.complete(Optional.of(new PlayerAccount(
                    UUID.fromString(in.readUTF()), // 1. uuid
                    in.readUTF(),                   // 2. name
                    in.readUTF(),                   // 3. realName
                    in.readUTF(),                   // 4. ip
                    in.readLong(),                  // 5. regDate
                    in.readLong(),                  // 6. lastLogin
                    in.readUTF(),                   // 7. discordId
                    in.readUTF(),                   // 8. skinName
                    in.readUTF()                    // 9. xuid 🌟 (เพิ่มตัวนี้)
            )));
        } else {
            future.complete(Optional.empty());
        }
    }

    @Override
    public void shutdown() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        pendingRequests.clear();
    }
}