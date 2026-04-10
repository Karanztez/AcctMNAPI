package AcctAPI.api;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ApiHandler {
    CompletableFuture<Boolean> isRegistered(String playerName);
    CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(String playerName);
    CompletableFuture<Boolean> isAuthenticated(UUID uuid);
    CompletableFuture<Void> forceChangePassword(String playerName, String newPassword);
    CompletableFuture<Void> forceDeleteAccount(String playerName);
    CompletableFuture<Optional<UUID>> getUuidByDiscordId(String discordId);
    CompletableFuture<Optional<String>> getDiscordId(String playerName);
    CompletableFuture<Optional<String>> getDiscordId(UUID playerUUID);
    void shutdown();
}
