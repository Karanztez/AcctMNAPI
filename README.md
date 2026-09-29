# AcctMN API

[![GitHub Release](https://img.shields.io/github/v/release/Karanztez/AcctMNAPI?include_prereleases&color=blue)](https://github.com/Karanztez/AcctMNAPI/releases)
[![JitPack](https://jitpack.io/v/Karanztez/AcctMNAPI.svg)](https://jitpack.io/#Karanztez/AcctMNAPI)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

Official Public Developer API for **AcctMN** (Minecraft Authentication System).  
Allows third-party plugins (Paper/Purpur) to check player authentication states, lookup registered accounts, link Discord accounts, and listen to login events without depending on internal implementation details.

---

## 📦 How to Integrate into Your Plugin

> ⚠️ **Important:** Always use `compileOnly`. Do **NOT** shade this API into your JAR. The classes are provided at runtime by the AcctMN plugin on the server.

### 1. Add Softdepend in `plugin.yml`

```yaml
name: YourPlugin
version: 1.0.0
main: com.example.yourplugin.YourPlugin
softdepend: [AcctMN]
```

---

### 2. Gradle Integration

#### Option A: JitPack (Recommended — No Token Required)

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.Karanztez:AcctMNAPI:v1.0.6")
}
```

#### Option B: GitHub Packages

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/Karanztez/AcctMNAPI")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compileOnly("com.github.Karanztez:AcctMNAPI:1.0.6")
}
```

---

## 💻 API Usage Examples

### 1. Getting the API Instance

**Kotlin:**
```kotlin
import acctMN.api.AcctMNApi

if (AcctMNApi.isAvailable()) {
    val api = AcctMNApi.get()
    // ...
}
```

**Java:**
```java
import acctMN.api.AcctMNApi;
import acctMN.api.AcctMNProvider;

if (AcctMNProvider.isAvailable()) {
    AcctMNApi api = AcctMNProvider.get();
    // ...
}
```

---

### 2. Check if Player is Authenticated

```java
// Synchronous check (checks session state)
boolean loggedIn = api.isAuthenticated(player);
if (!loggedIn) {
    player.sendMessage("Please log in first!");
    return;
}
```

---

### 3. Check if Player is Registered in Database

```java
// Asynchronous database check
api.isPlayerRegistered(player.getUniqueId()).thenAccept(isRegistered -> {
    if (isRegistered) {
        getLogger().info(player.getName() + " is registered in AcctMN!");
    } else {
        getLogger().info(player.getName() + " is a new player!");
    }
});
```

---

### 4. Get Player Account Snapshot & Discord Info

```java
import acctMN.api.AcctMNAccount;

AcctMNAccount account = api.getAccount(player.getUniqueId());
if (account != null) {
    String discordId = account.getDiscordId();
    if (discordId != null) {
        getLogger().info("Player's Discord ID: " + discordId);
    }
}
```

---

### 5. Listen to Auth State Changes

```java
import acctMN.api.event.PlayerAuthStateChangeEvent;
import acctMN.api.PlayerAuthState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyAuthListener implements Listener {
    @EventHandler
    public void onAuthStateChange(PlayerAuthStateChangeEvent event) {
        if (event.getNewState() == PlayerAuthState.AUTHENTICATED) {
            event.getPlayer().sendMessage("Welcome back! Your rewards have been given.");
        }
    }
}
```

---

## 📄 License
This API is distributed under the MIT License.
