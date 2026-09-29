package acctMN.api

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Public API for other Bukkit/Paper plugins.
 *
 * Lookup (any of these):
 * ```
 * AcctMNApi.get()
 * AcctMNProvider.get()
 * Bukkit.getServicesManager().load(AcctMNApi.class)
 * ```
 *
 * Depend with `compileOnly` + `softdepend: [AcctMN]`. Do not shade this JAR —
 * the classes are already inside the AcctMN plugin at runtime.
 */
interface AcctMNApi {

    companion object {
        /** @see AcctMNProvider.get */
        @JvmStatic
        fun get(): AcctMNApi = AcctMNProvider.get()

        /** @see AcctMNProvider.getOrNull */
        @JvmStatic
        fun getOrNull(): AcctMNApi? = AcctMNProvider.getOrNull()

        /** @see AcctMNProvider.isAvailable */
        @JvmStatic
        fun isAvailable(): Boolean = AcctMNProvider.isAvailable()
    }

    /**
     * Current session auth state. Returns [PlayerAuthState.AUTHENTICATED] when
     * the player is not tracked (offline / not managed).
     */
    fun getPlayerAuthState(playerUniqueId: UUID): PlayerAuthState

    fun getPlayerAuthState(player: Player): PlayerAuthState =
        getPlayerAuthState(player.uniqueId)

    /**
     * True when the player has finished login/register/captcha and may play.
     */
    fun isAuthenticated(playerUniqueId: UUID): Boolean =
        getPlayerAuthState(playerUniqueId) == PlayerAuthState.AUTHENTICATED

    fun isAuthenticated(player: Player): Boolean =
        isAuthenticated(player.uniqueId)

    /**
     * True when the player is still on the login/register/captcha flow.
     */
    fun isAuthenticating(playerUniqueId: UUID): Boolean =
        getPlayerAuthState(playerUniqueId) == PlayerAuthState.AUTHENTICATING

    fun isAuthenticating(player: Player): Boolean =
        isAuthenticating(player.uniqueId)

    /**
     * Checks the database (not the current session) for a registered account.
     */
    fun isPlayerRegistered(playerUniqueId: UUID): CompletableFuture<Boolean>

    fun isPlayerRegistered(player: Player): CompletableFuture<Boolean> =
        isPlayerRegistered(player.uniqueId)

    /**
     * Synchronous name lookup. Prefer [isPlayerRegistered] with UUID when possible.
     */
    fun isPlayerRegistered(playerName: String): Boolean

    /**
     * Account snapshot from the database, or null if unregistered.
     * Does not include passwords.
     */
    fun getAccount(playerUniqueId: UUID): AcctMNAccount?

    fun getAccount(player: Player): AcctMNAccount? =
        getAccount(player.uniqueId)

    fun getAccount(playerName: String): AcctMNAccount?

    /**
     * Discord snowflake linked to this Minecraft account, or null.
     * Honors `integrations.external-discord-lookup.enabled` on this backend.
     */
    fun getDiscordId(playerUniqueId: UUID): String?

    /**
     * Minecraft UUID linked to this Discord snowflake, or null.
     * Honors `integrations.external-discord-lookup.enabled` on this backend.
     */
    fun getUuidFromDiscordId(discordId: String): UUID?

    /**
     * True when a non-blank Discord ID is stored for this account.
     */
    fun isDiscordLinked(playerUniqueId: UUID): Boolean

    fun isDiscordLinked(player: Player): Boolean =
        isDiscordLinked(player.uniqueId)

    fun getAccountByDiscordId(discordId: String): AcctMNAccount? {
        val uuid = getUuidFromDiscordId(discordId) ?: return null
        return getAccount(uuid)
    }

    /**
     * Marks [player] as logged in without a password check.
     * Fires [acctMN.api.event.PlayerAuthStateChangeEvent] when the state changes.
     *
     * @return false if the player is offline
     */
    fun forceLogin(player: Player): Boolean
}
