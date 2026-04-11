package AcctAPI.api;

import java.util.UUID;

/**
 * คลาสสำหรับเก็บข้อมูลสรุปเกี่ยวกับบัญชีของผู้เล่น เพื่อใช้ใน API
 * (ใช้ Record เพื่อความกระชับของโค้ด)
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
        String skinName
) {
    public PlayerAccount(UUID uuid, String username, String realName, String ipAddress, long registrationDate, long lastLoginDate) {
        this(uuid, username, realName, ipAddress, registrationDate, lastLoginDate, "", "");
    }

    public PlayerAccount(UUID uuid, String username, String realName, String ipAddress, long registrationDate, long lastLoginDate, String discordId) {
        this(uuid, username, realName, ipAddress, registrationDate, lastLoginDate, discordId, "");
    }
}