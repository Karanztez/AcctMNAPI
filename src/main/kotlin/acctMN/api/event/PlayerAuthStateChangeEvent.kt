package acctMN.api.event

import acctMN.api.PlayerAuthState
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/**
 * Fired when a player's AcctMN session state changes.
 *
 * Typical use for other plugins: wait until [isAuthenticated] is true before
 * giving items, opening menus, or sending the player elsewhere.
 *
 * ```java
 * @EventHandler
 * public void onAuth(PlayerAuthStateChangeEvent event) {
 *     if (event.isAuthenticated()) {
 *         // player finished login / register / captcha
 *     }
 * }
 * ```
 */
class PlayerAuthStateChangeEvent(
    player: Player,
    val newState: PlayerAuthState,
    val oldState: PlayerAuthState?
) : PlayerEvent(player) {

    fun isAuthenticated(): Boolean = newState == PlayerAuthState.AUTHENTICATED

    fun isAuthenticating(): Boolean = newState == PlayerAuthState.AUTHENTICATING

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
