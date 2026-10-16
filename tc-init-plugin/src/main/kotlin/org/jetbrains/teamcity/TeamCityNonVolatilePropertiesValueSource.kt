package org.jetbrains.teamcity

import org.gradle.api.Describable
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.IOException

abstract class TeamCityNonVolatilePropertiesValueSource :
    ValueSource<Map<Any, Any>, ValueSourceParameters.None>, Describable {
    override fun obtain(): Map<Any, Any>? {
        return try {
            val properties = PropertiesFileReader.loadPropertiesFromFile()
            NonVolatileBuildPropertiesFilter.filterNonVolatileProperties(properties)
        } catch (ignored: IOException) {
            null
        }
    }

    override fun getDisplayName(): String {
        return "TeamCity build properties collection"
    }
}
