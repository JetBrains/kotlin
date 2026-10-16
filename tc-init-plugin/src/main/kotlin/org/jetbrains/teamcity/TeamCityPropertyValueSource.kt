package org.jetbrains.teamcity

import org.gradle.api.Describable
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.IOException

abstract class TeamCityPropertyValueSource : ValueSource<String, TeamCityPropertyValueSource.Parameters>, Describable {
    interface Parameters : ValueSourceParameters {
        @get:org.gradle.api.tasks.Input
        val propertyKey: Property<String>
    }

    override fun obtain(): String? {
        return try {
            val properties = PropertiesFileReader.loadPropertiesFromFile()
            val key = parameters.propertyKey.orNull ?: return null
            properties.getProperty(key)
        } catch (ignored: IOException) {
            null
        }
    }

    override fun getDisplayName(): String {
        return "TeamCity build property '${parameters.propertyKey.orNull}'"
    }
}
