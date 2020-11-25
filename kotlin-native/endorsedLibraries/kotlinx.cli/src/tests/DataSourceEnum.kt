package kotlinx.cli

enum class DataSourceEnum {
    LOCAL,
    STAGING,
    PRODUCTION;

    override fun toString(): String = name.toLowerCase()
}
