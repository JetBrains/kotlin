package org.jetbrains.teamcity

import java.util.Properties

object NonVolatileBuildPropertiesFilter {
    private val nonVolatileTeamCityPrefixes = listOf(
        "teamcity.build.",
        "teamcity.buildConfName",
        "teamcity.buildType.",
        "teamcity.configuration.",
        "teamcity.runner.",
        "teamcity.projectName",
        "teamcity.tests."
    )

    fun filterNonVolatileProperties(properties: Properties): Map<Any, Any> {
        val result = mutableMapOf<Any, Any>()
        for ((key, value) in properties) {
            if (isANonVolatileProperty(key)) {
                if (value != null) {
                    result[key] = value
                }
            }
        }
        return result
    }

    private fun isANonVolatileProperty(key: Any): Boolean {
        var propertyKey = key.toString()

        if (propertyKey.startsWith("dep.")) {
            val propertyNameIndex = propertyKey.indexOf('.', "dep.".length) + 1
            if (propertyNameIndex in 1..propertyKey.length) {
                propertyKey = propertyKey.substring(propertyNameIndex)
            }
        }

        if (propertyKey.startsWith("build.")) return false
        if (propertyKey.startsWith("teamcity.build.id")) return false
        if (nonVolatileTeamCityPrefixes.any { prefix -> propertyKey.startsWith(prefix) }) return true

        return !propertyKey.startsWith("teamcity.")
    }
}
