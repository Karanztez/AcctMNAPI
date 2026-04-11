package AcctAPI.api;

import java.util.UUID;

/**
 * คลาสสำหรับเก็บข้อมูลสรุปเกี่ยวกับบัญชีของผู้เล่น เพื่อใช้ใน API
 * อัปเดต: รองรับ XUID สำหรับผู้เล่น Minecraft Bedrock (Floodgate)
 */
@SuppressWarnings("unused")
public record PlayerAccount(
        UUID uuid,
        String username,
        String realName,
        String ipAddress,
        long registrationDate,
        long lastLoginDate,
        String discordId,
        String skinName,
        String xuid
) {
    // Constructor พื้นฐาน (6 พารามิเตอร์)
    public PlayerAccount(UUID uuid, String username, String realName, String ipAddress, long registrationDate, long lastLoginDate) {
        this(uuid, username, realName, ipAddress, registrationDate, lastLoginDate, "", "", "");
    }

    // Constructor รองรับ Discord (7 พารามิเตอร์)
    public PlayerAccount(UUID uuid, String username, String realName, String ipAddress, long registrationDate, long lastLoginDate, String discordId) {
        this(uuid, username, realName, ipAddress, registrationDate, lastLoginDate, discordId, "", "");
    }

    // Constructor รองรับ Discord และ Skin (8 พารามิเตอร์)
    public PlayerAccount(UUID uuid, String username, String realName, String ipAddress, long registrationDate, long lastLoginDate, String discordId, String skinName) {
        this(uuid, username, realName, ipAddress, registrationDate, lastLoginDate, discordId, skinName, "");
    }
}