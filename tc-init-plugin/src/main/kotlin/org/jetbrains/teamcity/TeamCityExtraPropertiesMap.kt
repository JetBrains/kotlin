package org.jetbrains.teamcity

import org.gradle.api.provider.ProviderFactory
import java.util.Properties

class TeamCityExtraPropertiesMap(
    private val providerFactory: ProviderFactory,
    private val properties: Properties
) : Map<String, String> {

    override val size: Int
        get() = properties.size

    override fun isEmpty(): Boolean = properties.isEmpty()

    override fun containsKey(key: String): Boolean = get(key) != null

    override fun containsValue(value: String): Boolean = values.contains(value)

    override fun get(key: String): String? {
        return providerFactory.of(TeamCityPropertyValueSource::class.java) {
            it.parameters.propertyKey.set(key)
        }.orNull
    }

    override val keys: Set<String>
        get() = properties.stringPropertyNames()

    override val values: Collection<String>
        get() = keys.mapNotNull { get(it) }

    data class MapEntry(
        override val key: String,
        override val value: String
    ) : Map.Entry<String, String>

    override val entries: Set<Map.Entry<String, String>>
        get() {
            return keys.mapNotNull { key ->
                val value = get(key)
                if (value != null) MapEntry(key, value) else null
            }.toSet()
        }
}
