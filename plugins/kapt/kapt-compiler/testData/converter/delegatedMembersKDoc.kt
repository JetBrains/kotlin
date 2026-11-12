interface Config {
    val fetchSize: Int?
    fun timeout(): Int
}

/** @suppress */
class ConfigImpl(delegate: Config) : Config by delegate

/**
 * Auto-increment column type.
 *
 * @property delegate the wrapped config
 */
class AutoInc(private val delegate: Config) : Config by delegate
