/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import java.io.File
import java.io.StringReader
import java.util.Properties

private const val SYNCED_PROPERTIES_START_LINE = "# Automatically configured by the `internal-gradle-setup` plugin"

internal val SYNCED_PROPERTIES_START_LINES = """
    $SYNCED_PROPERTIES_START_LINE
    # Please do not edit these properties manually, the changes will be lost
    # If you want to override some values, put them before this section and remove from this section
""".trimIndent()

internal const val SYNCED_PROPERTIES_END_LINE = "# the end of automatically configured properties"

internal class LocalPropertiesModifier(private val localProperties: File) {
    private val manuallyConfiguredPropertiesContent by lazy {
        if (!localProperties.exists()) return@lazy ""
        var insideAutomaticallyConfiguredSection = false
        // filter out the automatically configured lines
        localProperties.readLines().filter { line ->
            if (line == SYNCED_PROPERTIES_START_LINE) {
                insideAutomaticallyConfiguredSection = true
            }
            val shouldIncludeThisLine = !insideAutomaticallyConfiguredSection
            if (line == SYNCED_PROPERTIES_END_LINE) {
                insideAutomaticallyConfiguredSection = false
            }
            shouldIncludeThisLine
        }.joinToString("\n")
    }

    fun applySetup(setupFile: SetupFile) {
        localProperties.parentFile.apply {
            if (!exists()) {
                mkdirs()
            }
        }
        if (localProperties.exists() && !localProperties.isFile) {
            error("$localProperties is not a file!")
        }
        val manuallyConfiguredProperties = Properties().apply {
            StringReader(manuallyConfiguredPropertiesContent).use {
                load(it)
            }
        }
        val propertiesToSetup = setupFile.properties.mapValues { PropertyValue(it.value, manuallyConfiguredProperties.containsKey(it.key)) }
        localProperties.writeText(
            """
            |${manuallyConfiguredPropertiesContent.addSuffix("\n")}
            |$SYNCED_PROPERTIES_START_LINES
            |${propertiesToSetup.asPropertiesLines}
            |$SYNCED_PROPERTIES_END_LINE
            |
            """.trimMargin()
        )
    }
}

private fun String.addSuffix(suffix: String): String {
    if (this.endsWith(suffix)) return this
    return "$this$suffix"
}

internal data class PropertyValue(
    val value: String,
    val isOverridden: Boolean = false,
)

internal val Map<String, PropertyValue>.asPropertiesLines: String
    get() = map { (key, value) ->
        when (value.isOverridden) {
            true -> """
                #$key=${value.value} the property is overridden
            """.trimIndent()
            false -> "$key=${value.value}"
        }
    }.joinToString("\n")