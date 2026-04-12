Here is the professional English version of your **README.md**. It follows the standard industry format for high-quality GitHub repositories.

---

# **AcctMNAPI v1.0.9.2**
### *The Lightweight Authentication Bridge for Minecraft Networks*

**AcctMNAPI** is a high-performance, developer-friendly library designed to interface with the **AcctMN** (Spigot/Paper) and **AcctVelocity** ecosystem. It provides a unified API for managing player accounts across standalone servers and complex proxy networks.

---

## **📦 Integration**

AcctMNAPI is distributed via **JitPack**. Add the following to your build configuration:

### **Gradle (Kotlin DSL)**
```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("com.github.Karanztez:AcctMNAPI:1.0.8")
}
```

### **Maven**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Karanztez</groupId>
    <artifactId>AcctMNAPI</artifactId>
    <version>1.0.8</version>
    <scope>provided</scope>
</dependency>
```

---

## **🛠 Technical Specifications**

* **Architecture:** Static-access API for seamless integration.
* **Concurrency:** Fully Non-blocking/Asynchronous (`CompletableFuture`).
* **Data Model:** Immutable Java Records (v1.0.8+).
* **Identity:** UUID-First implementation with PlayerName fallback support.
* **Network:** Built-in 5-second timeout safety for Proxy-mode requests.

---

## **💡 Quick Start**

The API is accessed through the global `AcctAPI` class. Since it is asynchronous, always handle results within the future's callback.

```java
import AcctAPI.AcctAPI;

// Example: Retrieve player account details using UUID
AcctAPI.getPlayerAccount(uuid).thenAccept(optAccount -> {
    optAccount.ifPresent(account -> {
        String discordId = account.discordId();
        // Handle your logic (Remember to sync with the main thread for Bukkit calls)
    });
}).exceptionally(ex -> {
    ex.printStackTrace();
    return null;
});
```

---

## **🔍 API Reference**

All methods are available via `AcctAPI.methodName()`.

| Method | Return Type | Description |
| :--- | :--- | :--- |
| `getPlayerAccount(UUID/String)` | `Optional<PlayerAccount>` | Fetches full account profile. |
| `isRegistered(UUID/String)` | `Boolean` | Checks if a player is in the database. |
| `isAuthenticated(UUID)` | `Boolean` | Checks if the player is currently logged in. |
| `getDiscordId(UUID/String)` | `Optional<String>` | Retrieves the linked Discord ID. |
| `getUuidByDiscordId(String)` | `Optional<UUID>` | Finds a Player UUID from a Discord ID. |
| `forceChangePassword(...)` | `Void` | Update player password (Fire & Forget). |
| `forceDeleteAccount(...)` | `Void` | Unregister and kick player (Fire & Forget). |

---

## **⚖ License**
Distributed under the **GPLv3 License**. See `LICENSE` for more information.

Developed with ❤️ by **Karanztez** & the Acct Dev.
