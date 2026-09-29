package acctMN.api

/**
 * Static holder for the running [AcctMNApi] instance.
 *
 * Prefer [AcctMNApi.get] from other plugins. This type exists so Java plugins
 * can also call `AcctMNProvider.get()` (LuckPerms-style).
 *
 * Runtime classes come from the AcctMN plugin JAR — depend on this API with
 * `compileOnly`, and do **not** shade it.
 */
object AcctMNProvider {

    @Volatile
    private var instance: AcctMNApi? = null

    /**
     * @return the live API
     * @throws IllegalStateException if AcctMN is not enabled
     */
    @JvmStatic
    fun get(): AcctMNApi {
        return instance
            ?: throw IllegalStateException(
                "AcctMN is not loaded. Add softdepend: [AcctMN] and call AcctMNApi.isAvailable() first."
            )
    }

    /** @return the live API, or null if AcctMN is missing / disabled */
    @JvmStatic
    fun getOrNull(): AcctMNApi? = instance

    /** @return true when AcctMN has registered its API */
    @JvmStatic
    fun isAvailable(): Boolean = instance != null

    /**
     * Called by AcctMN on enable. Do not call from other plugins.
     */
    @JvmStatic
    fun register(api: AcctMNApi) {
        instance = api
    }

    /**
     * Called by AcctMN on disable. Do not call from other plugins.
     */
    @JvmStatic
    fun unregister() {
        instance = null
    }
}
