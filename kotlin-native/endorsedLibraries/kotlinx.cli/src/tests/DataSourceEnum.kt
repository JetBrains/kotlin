package kotlinx.cli

import java.util.*

enum class DataSourceEnum {
    LOCAL,
    STAGING,
    PRODUCTION;

    override fun toString(): String = name.lowercase(Locale.getDefault())
}
