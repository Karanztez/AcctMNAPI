================================================================
AcctAPI v1.0.8 - คู่มือสำหรับผู้พัฒนา (Developer Guide)
================================================================

ยินดีต้อนรับสู่ AcctAPI v1.0.8!

ไลบรารีนี้ได้รับการออกแบบใหม่ทั้งหมดเพื่อให้ใช้งานง่าย, มีประสิทธิภาพ, และรองรับการทำงานบนเครือข่าย Proxy (BungeeCord/Velocity) ได้อย่างสมบูรณ์

### **ปรัชญาการออกแบบใหม่**

AcctAPI ไม่ได้เป็นปลั๊กอินที่ต้องตั้งค่าเองอีกต่อไป แต่เป็นไลบรารีแบบ Static ที่พร้อมใช้งานทันที โดยอาศัยปลั๊กอินหลัก (เช่น `AcctMN` หรือ `AcctVelocity`) ในการจัดการการเชื่อมต่อทั้งหมดให้โดยอัตโนมัติ

**สิ่งที่คุณต้องทำมีเพียงอย่างเดียว: เพิ่ม `AcctAPI` เป็น dependency ในโปรเจกต์ของคุณ**

---
### **1. การตั้งค่าโปรเจกต์**

เพิ่ม `AcctAPI` เป็น dependency ใน `plugin.yml` ของคุณเพื่อให้แน่ใจว่า API จะพร้อมใช้งานก่อนที่ปลั๊กอินของคุณจะถูกโหลด

**plugin.yml:**
```yaml
# ... ข้อมูลปลั๊กอินของคุณ
name: MyAwesomePlugin
version: 2.0
main: com.myplugin.Main
api-version: 1.21

# บรรทัดที่สำคัญที่สุด:
# บอกให้เซิร์ฟเวอร์โหลด AcctAPI ให้เสร็จก่อนเสมอ
depend:
  - AcctAPI
```

---
### **2. การเรียกใช้งาน API**

ทุกเมธอดใน `AcctAPI` เป็นแบบ Asynchronous และจะคืนค่าเป็น `CompletableFuture<T>` เสมอ เพื่อป้องกันไม่ให้เซิร์ฟเวอร์ของคุณค้างขณะรอข้อมูล

**การเปลี่ยนแปลงที่สำคัญ:**
*   **เมธอดส่วนใหญ่รองรับทั้ง `String` (ชื่อผู้เล่น) และ `UUID`** เพื่อความสะดวกในการใช้งาน
*   คุณไม่จำเป็นต้องสนใจว่าเซิร์ฟเวอร์กำลังทำงานในโหมด "เดี่ยว" หรือ "Proxy" เพราะ API จะจัดการให้เอง

**ตัวอย่าง: ตรวจสอบว่าผู้เล่นลงทะเบียนแล้วหรือไม่**

```java
import AcctAPI.AcctAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    public void checkPlayerRegistration(Player player) {

        // เรียก API ด้วย UUID (แนะนำ) หรือชื่อผู้เล่น
        // การเรียกนี้จะคืนค่า CompletableFuture<Boolean> ทันทีโดยไม่ทำให้เซิร์ฟเวอร์ค้าง
        CompletableFuture<Boolean> future = AcctAPI.isRegistered(player.getUniqueId());

        // จัดการกับผลลัพธ์ที่จะได้รับในอนาคต
        future.thenAccept(isRegistered -> {

            // โค้ดในบล็อกนี้จะทำงานเมื่อได้รับคำตอบจาก AcctAPI แล้ว
            if (isRegistered) {
                // สำคัญ: หากต้องการรันโค้ดที่เกี่ยวกับ Bukkit (เช่น ส่งข้อความ, แก้ไข Block)
                // ต้องสลับกลับมาทำงานบน Thread หลักของเซิร์ฟเวอร์เสมอ!
                getServer().getScheduler().runTask(this, () -> {
                    player.sendMessage("คุณได้ลงทะเบียนเรียบร้อยแล้ว!");
                });
            } else {
                getServer().getScheduler().runTask(this, () -> {
                    player.sendMessage("คุณยังไม่ได้ลงทะเบียน");
                });
            }

        }).exceptionally(error -> {
            // จัดการกับข้อผิดพลาดที่อาจเกิดขึ้น (เช่น API Timeout)
            getLogger().warning("ไม่สามารถตรวจสอบข้อมูลผู้เล่นได้: " + error.getMessage());
            return null; // จำเป็นต้องคืนค่า null ใน exceptionally
        });
    }
}
```

---
### **3. เมธอดที่มีให้ใช้งาน**

เมธอดทั้งหมดอยู่ในคลาส `AcctAPI` และเป็นแบบ Static

*   `getPlayerAccount(String/UUID)`: ขอข้อมูลบัญชีผู้เล่น
*   `isRegistered(String/UUID)`: ตรวจสอบว่าผู้เล่นลงทะเบียนแล้วหรือไม่
*   `isAuthenticated(UUID)`: ตรวจสอบสถานะการล็อกอิน
*   `getUuidByDiscordId(String)`: ค้นหา UUID จาก Discord ID
*   `getDiscordId(String/UUID)`: ค้นหา Discord ID จากข้อมูลผู้เล่น
*   `forceChangePassword(String/UUID, newPassword)`: สั่งเปลี่ยนรหัสผ่าน
*   `forceDeleteAccount(String/UUID)`: สั่งลบบัญชี

---
### **สรุปการอัปเดตจากเวอร์ชันเก่า**

*   **ง่ายขึ้น:** ไม่ต้องตั้งค่า `config.yml` ของ `AcctAPI` อีกต่อไป
*   **สะดวกขึ้น:** เมธอดส่วนใหญ่รองรับทั้ง `UUID` และ `String`
*   **เสถียรขึ้น:** มีระบบ Timeout ในตัวสำหรับโหมด Proxy

ขอให้สนุกกับการพัฒนาครับ!
