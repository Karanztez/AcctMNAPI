package acctMN.api

/**
 * Authentication status of a player's current session.
 *
 * Other plugins should treat [AUTHENTICATING] as "do not interact yet"
 * (login / register / captcha still in progress).
 */
enum class PlayerAuthState {
    /**
     * The player has just joined and has not finished login, register, or captcha.
     * Other plugins should wait before interacting with the player.
     */
    AUTHENTICATING,

    /**
     * The player has successfully authenticated and may use the server.
     */
    AUTHENTICATED
}
