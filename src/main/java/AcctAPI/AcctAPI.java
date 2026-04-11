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

    /**
     * ตั้งค่า Handler สำหรับ API (เรียกใช้โดย AcctVelocity หรือ AcctMN)
     */
    public static void setHandler(ApiHandler apiHandler) {
        if (handler != null && handler != apiHandler) {
            System.out.println("[AcctAPI] Warning: AcctAPI handler is being replaced.");
        }
        handler = apiHandler;
    }

    /**
     * ปิดการทำงานของ API และเคลียร์ Handler
     */
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

    // --- Methods สำหรับดึงข้อมูลบัญชี ---

    public static CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(String playerName) { return getHandler().getPlayerAccount(playerName); }
    public static CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(UUID uuid) { return getHandler().getPlayerAccount(uuid); }

    public static CompletableFuture<Boolean> isRegistered(String playerName) { return getHandler().isRegistered(playerName); }
    public static CompletableFuture<Boolean> isRegistered(UUID uuid) { return getHandler().isRegistered(uuid); }

    public static CompletableFuture<Boolean> isAuthenticated(UUID uuid) { return getHandler().isAuthenticated(uuid); }

    // --- Methods สำหรับ Discord Integration ---

    public static CompletableFuture<Optional<UUID>> getUuidByDiscordId(String discordId) { return getHandler().getUuidByDiscordId(discordId); }
    public static CompletableFuture<Optional<String>> getDiscordId(String playerName) { return getHandler().getDiscordId(playerName); }
    public static CompletableFuture<Optional<String>> getDiscordId(UUID playerUUID) { return getHandler().getDiscordId(playerUUID); }

    // --- 🌟 Methods ใหม่สำหรับระบบ Bedrock/XUID 🌟 ---

    /**
     * ดึงค่า XUID ของผู้เล่นผ่านชื่อ (คืนค่า Optional.empty() หากไม่ใช่ผู้เล่น Bedrock หรือไม่พบข้อมูล)
     */
    public static CompletableFuture<Optional<String>> getXuid(String playerName) {
        return getHandler().getXuid(playerName);
    }

    /**
     * ดึงค่า XUID ของผู้เล่นผ่าน UUID (คืนค่า Optional.empty() หากไม่ใช่ผู้เล่น Bedrock หรือไม่พบข้อมูล)
     */
    public static CompletableFuture<Optional<String>> getXuid(UUID uuid) {
        return getHandler().getXuid(uuid);
    }

    // --- Methods สำหรับการจัดการบัญชี (Force Actions) ---

    public static CompletableFuture<Void> forceChangePassword(String playerName, String newPassword) { return getHandler().forceChangePassword(playerName, newPassword); }
    public static CompletableFuture<Void> forceChangePassword(UUID uuid, String newPassword) { return getHandler().forceChangePassword(uuid, newPassword); }

    public static CompletableFuture<Void> forceDeleteAccount(String playerName) { return getHandler().forceDeleteAccount(playerName); }
    public static CompletableFuture<Void> forceDeleteAccount(UUID uuid) { return getHandler().forceDeleteAccount(uuid); }
}