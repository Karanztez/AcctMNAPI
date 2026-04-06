package AcctAPI.api;

import java.util.UUID;

/**
 * คลาสสำหรับเก็บข้อมูลสรุปเกี่ยวกับบัญชีของผู้เล่น เพื่อใช้ใน API
 * (ใช้ Record เพื่อความกระชับของโค้ด)
 */
public record PlayerAccount(
        UUID uuid,
        String username,
        String realName,
        String ipAddress,
        long registrationDate,
        long lastLoginDate,
        String discordId, // เพิ่มคอลัมน์ discordId
        String skinName // เพิ่มคอลัมน์ skinName
) {
    // รองรับ Constructor เก่าในกรณีที่บางที่ยังไม่ได้ใส่ discordId
    public PlayerAccount(UUID uuid, String username, String realName, String ipAddress, long registrationDate, long lastLoginDate) {
        this(uuid, username, realName, ipAddress, registrationDate, lastLoginDate, "", "");
    }

    public PlayerAccount(UUID uuid, String username, String realName, String ipAddress, long registrationDate, long lastLoginDate, String discordId) {
        this(uuid, username, realName, ipAddress, registrationDate, lastLoginDate, discordId, "");
    }
}
