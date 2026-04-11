package AcctAPI;

import AcctAPI.api.ApiHandler;
import AcctAPI.api.PlayerAccount;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap; // 🌟 เพิ่ม

@SuppressWarnings("unused")
public final class AcctAPI {

    private static ApiHandler handler;

    // 🌟 ระบบ Local Cache สำหรับ XUID เพื่อให้ Proxy แสดงรูปได้ทันที
    private static final ConcurrentHashMap<UUID, String> xuidCache = new ConcurrentHashMap<>();

    private AcctAPI() {}

    /**
     * บันทึก XUID เข้า Cache (เรียกใช้โดย JoinListener เมื่อเจอผู้เล่น Bedrock)
     */
    public static void setCacheXuid(UUID uuid, String xuid) {
        if (uuid != null && xuid != null && !xuid.isEmpty()) {
            xuidCache.put(uuid, xuid);
        }
    }

    /**
     * ตั้งค่า Handler สำหรับ API
     */
    public static void setHandler(ApiHandler apiHandler) {
        if (handler != null && handler != apiHandler) {
            System.out.println("[AcctAPI] Warning: AcctAPI handler is being replaced.");
        }
        handler = apiHandler;
    }

    public static void shutdown() {
        xuidCache.clear(); // 🌟 เคลียร์ Cache เมื่อปิดระบบ
        if (handler != null) {
            handler.shutdown();
            handler = null;
        }
    }

    private static ApiHandler getHandler() {
        if (handler == null) {
            throw new IllegalStateException("AcctAPI handler has not been initialized.");
        }
        return handler;
    }

    // --- 🌟 Methods ปรับปรุงใหม่สำหรับระบบ Bedrock/XUID 🌟 ---

    /**
     * ดึงค่า XUID ผ่าน UUID (เช็ค Cache ก่อน ถ้าเจอจะคืนค่าทันที)
     */
    public static CompletableFuture<Optional<String>> getXuid(UUID uuid) {
        // 🛡️ เช็คใน Cache ของ Proxy ก่อน (Instant Speed)
        String cachedXuid = xuidCache.get(uuid);
        if (cachedXuid != null) {
            return CompletableFuture.completedFuture(Optional.of(cachedXuid));
        }

        // 📡 ถ้าไม่มีใน Cache ค่อยไปถาม Handler (Spigot)
        return getHandler().getXuid(uuid);
    }

    /**
     * ดึงค่า XUID ผ่านชื่อผู้เล่น
     */
    public static CompletableFuture<Optional<String>> getXuid(String playerName) {
        return getHandler().getXuid(playerName);
    }

    // --- Methods อื่นๆ คงเดิม ---

    public static CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(String playerName) { return getHandler().getPlayerAccount(playerName); }
    public static CompletableFuture<Optional<PlayerAccount>> getPlayerAccount(UUID uuid) { return getHandler().getPlayerAccount(uuid); }

    public static CompletableFuture<Boolean> isRegistered(String playerName) { return getHandler().isRegistered(playerName); }
    public static CompletableFuture<Boolean> isRegistered(UUID uuid) { return getHandler().isRegistered(uuid); }

    public static CompletableFuture<Boolean> isAuthenticated(UUID uuid) { return getHandler().isAuthenticated(uuid); }

    public static CompletableFuture<Optional<UUID>> getUuidByDiscordId(String discordId) { return getHandler().getUuidByDiscordId(discordId); }
    public static CompletableFuture<Optional<String>> getDiscordId(String playerName) { return getHandler().getDiscordId(playerName); }
    public static CompletableFuture<Optional<String>> getDiscordId(UUID playerUUID) { return getHandler().getDiscordId(playerUUID); }

    public static CompletableFuture<Void> forceChangePassword(String playerName, String newPassword) { return getHandler().forceChangePassword(playerName, newPassword); }
    public static CompletableFuture<Void> forceChangePassword(UUID uuid, String newPassword) { return getHandler().forceChangePassword(uuid, newPassword); }

    public static CompletableFuture<Void> forceDeleteAccount(String playerName) { return getHandler().forceDeleteAccount(playerName); }
    public static CompletableFuture<Void> forceDeleteAccount(UUID uuid) { return getHandler().forceDeleteAccount(uuid); }
}