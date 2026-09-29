package acctMN.api

import java.util.UUID

/**
 * Public snapshot of an AcctMN account.
 *
 * Never includes password hashes or other secrets. Blank stored fields are
 * exposed as `null`.
 */
data class AcctMNAccount(
    val uniqueId: UUID,
    val username: String,
    val lastIp: String?,
    val registrationDate: Long,
    val lastLoginDate: Long,
    val discordId: String?,
    val skinName: String?,
    val xuid: String?
) {
    fun isDiscordLinked(): Boolean = !discordId.isNullOrBlank()

    fun isBedrock(): Boolean = !xuid.isNullOrBlank()
}
