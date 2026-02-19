// CORRECT_ERROR_TYPES

@Suppress("UNRESOLVED_REFERENCE")
class Application {
    lateinit var _preferencesDataStore: DataStore<Preferences>

    companion object {
        @JvmStatic
        fun get(): Application = error()

        @JvmStatic
        fun getPreferencesDataStore() = get()._preferencesDataStore
    }
}
