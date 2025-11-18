/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.util.DefFile.DefFileConfig
import org.jetbrains.kotlin.library.KlibMockDSL
import java.util.Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.collections.orEmpty

class DefFileConfigTest {

    @Test
    fun testStringListPropertiesReading() {
        listOf(
            "headers" to DefFileConfig::headers,
            "modules" to DefFileConfig::modules,
            "compilerOpts" to DefFileConfig::compilerOpts,
            "entryPoints" to DefFileConfig::entryPoints,
            "linkerOpts" to DefFileConfig::linkerOpts,
            "excludedFunctions" to DefFileConfig::excludedFunctions,
            "excludedMacros" to DefFileConfig::excludedMacros,
            "staticLibraries" to DefFileConfig::staticLibraries,
            "libraryPaths" to DefFileConfig::libraryPaths,
            "headerFilter" to DefFileConfig::headerFilter,
            "excludeFilter" to DefFileConfig::excludeFilter,
            "strictEnums" to DefFileConfig::strictEnums,
            "nonStrictEnums" to DefFileConfig::nonStrictEnums,
            "noStringConversion" to DefFileConfig::noStringConversion,
            "depends" to DefFileConfig::depends,
            "exportForwardDeclarations" to DefFileConfig::exportForwardDeclarations,
            "allowedOverloadsForCFunctions" to DefFileConfig::allowedOverloadsForCFunctions,
            "objcClassesIncludingCategories" to DefFileConfig::objcClassesIncludingCategories,
        ).forEach { (propertyName, readProperty: DefFileConfig.() -> List<String>) ->
            val stringListValues: List<List<String>?> = listOf<List<String>?>(null) + buildList {
                repeat(5) { listSize ->
                    this + List(listSize) { KlibMockDSL.generateRandomName(20) }
                }
            }

            for (expected: List<String>? in stringListValues) {
                val properties = Properties()
                expected?.let { properties.setProperty(propertyName, it.joinToString(separator = " ")) }

                val actual: List<String> = DefFileConfig(properties).readProperty()
                assertEquals(expected.orEmpty(), actual, "Incorrect value for \"$propertyName\" property: $expected vs $actual")
            }
        }
    }

    @Test
    fun testNullableStringPropertiesReading() {
        listOf(
            Triple("language", DefFileConfig::language, null),
            Triple("linker", DefFileConfig::linker, "clang"),
            Triple("package", DefFileConfig::packageName, null),
            Triple("foreignExceptionMode", DefFileConfig::foreignExceptionMode, null),
            Triple("userSetupHint", DefFileConfig::userSetupHint, null),
        ).forEach { (propertyName, readProperty: DefFileConfig.() -> String?, defaultValue: String?) ->
            val stringValues: List<String?> = listOf(null) + buildList {
                repeat(5) { valueLength ->
                    this += if (valueLength == 0) "" else KlibMockDSL.generateRandomName(valueLength)
                }
            }

            for (expected: String? in stringValues) {
                val properties = Properties()
                expected?.let { properties.setProperty(propertyName, it) }

                val actual: String? = DefFileConfig(properties).readProperty()
                assertEquals(expected ?: defaultValue, actual, "Incorrect value for \"$propertyName\" property: $expected vs $actual")
            }
        }
    }

    @Test
    fun testBooleanPropertiesReading() {
        listOf(
            "excludeSystemLibs" to DefFileConfig::excludeSystemLibs,
            "excludeDependentModules" to DefFileConfig::excludeDependentModules,
            "disableDesignatedInitializerChecks" to DefFileConfig::disableDesignatedInitializerChecks,
            "allowIncludingObjCCategoriesFromDefFile" to DefFileConfig::allowIncludingObjCCategoriesFromDefFile,
        ).forEach { (propertyName, readProperty: DefFileConfig.() -> Boolean) ->
            val booleanValues: List<Boolean?> = listOf(null, true, false)

            for (expected: Boolean? in booleanValues) {
                val properties = Properties()
                expected?.let { properties.setProperty(propertyName, it.toString()) }

                val actual: Boolean = DefFileConfig(properties).readProperty()
                assertEquals(expected ?: false, actual, "Incorrect value for \"$propertyName\" property: $expected vs $actual")
            }
        }
    }
}
