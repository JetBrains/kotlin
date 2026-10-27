/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.io.ManifestProperties
import org.jetbrains.kotlin.io.createPropertiesFromBytes
import org.jetbrains.kotlin.konan.util.DefFileProperty
import org.jetbrains.kotlin.konan.util.substituteFor
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.library.KLIB_PROPERTY_ABI_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_INTEROP
import org.jetbrains.kotlin.library.KLIB_PROPERTY_IR_PROVIDER
import org.jetbrains.kotlin.library.KLIB_PROPERTY_METADATA_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.jetbrains.kotlin.library.KlibMockDSL
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue

class PropertySubstitutionTest {
    @Test
    fun `Test substitution works for the limited list of known properties`() {
        DefFileProperty.StringListProperty.entries.filter { it.isSubstitutable }.forEach { property ->
            doTestSubstitution(basePropertyName = property.propertyName, isSuccessExpected = true)
        }
    }

    @Test
    fun `Test substitution does not work for other properties`() {
        buildSet {
            DefFileProperty.StringListProperty.entries.filterNot { it.isSubstitutable }.mapTo(this) { it.propertyName }
            DefFileProperty.NullableStringProperty.entries.mapTo(this) { it.propertyName }
            DefFileProperty.BooleanProperty.entries.mapTo(this) { it.propertyName }

            this += KLIB_PROPERTY_UNIQUE_NAME
            this += KLIB_PROPERTY_METADATA_VERSION
            this += KLIB_PROPERTY_ABI_VERSION
            this += KLIB_PROPERTY_INTEROP
            this += KLIB_PROPERTY_IR_PROVIDER
        }.forEach { basePropertyName ->
            doTestSubstitution(basePropertyName, isSuccessExpected = false)
        }
    }

    @Test
    fun `Test substituteFor returns same instance without cloning when plain Properties has no dotted keys`() {
        val properties = createPlainPropertiesFromLines(
            listOf(
                "$KLIB_PROPERTY_UNIQUE_NAME=org.example.sample",
                "$KLIB_PROPERTY_ABI_VERSION=1.8.0",
                "$KLIB_PROPERTY_METADATA_VERSION=1.4.1",
                "$KLIB_PROPERTY_IR_PROVIDER=kotlin.ir.klib",
                "$KLIB_PROPERTY_INTEROP=false",
            )
        )

        for (target in KonanTarget.predefinedTargets.values) {
            val substituted = properties.substituteFor(target)
            assertSame(properties, substituted)
        }
        assertEquals("org.example.sample", properties.getProperty(KLIB_PROPERTY_UNIQUE_NAME))
        assertEquals("1.8.0", properties.getProperty(KLIB_PROPERTY_ABI_VERSION))
    }

    @Test
    fun `Test substituteFor returns same ManifestProperties instance and preserves deferred depends when no dotted keys exist`() {
        val expectedDepends = createLargeDependsValue("third_party_kotlin_dep")
        assertTrue(expectedDepends.length > 60_000)

        val manifestText = listOf(
            "$KLIB_PROPERTY_UNIQUE_NAME=org.example.klib",
            "$KLIB_PROPERTY_ABI_VERSION=1.8.0",
            "$KLIB_PROPERTY_METADATA_VERSION=1.4.1",
            "$KLIB_PROPERTY_DEPENDS=$expectedDepends",
            "$KLIB_PROPERTY_IR_PROVIDER=kotlin.ir.klib",
        ).joinToString(separator = "\n")

        val manifestProperties = createPropertiesFromBytes(manifestText.toByteArray(StandardCharsets.UTF_8)) as ManifestProperties
        assertFalse(manifestProperties.hasDottedKey)
        assertTrue(manifestProperties.hasDeferredDepends)

        for (target in listOf(KonanTarget.IOS_ARM64, KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.LINUX_X64)) {
            val substituted = manifestProperties.substituteFor(target)
            assertSame(manifestProperties, substituted)
            assertTrue(manifestProperties.hasDeferredDepends)
        }

        assertEquals("org.example.klib", manifestProperties.getProperty(KLIB_PROPERTY_UNIQUE_NAME))
        assertTrue(manifestProperties.hasDeferredDepends)
        assertEquals(expectedDepends, manifestProperties.getProperty(KLIB_PROPERTY_DEPENDS))
        assertFalse(manifestProperties.hasDeferredDepends)
    }

    @Test
    fun `Test substituteFor merges target arch and family suffixes without mutating input Properties`() {
        val lines = listOf(
            "$KLIB_PROPERTY_UNIQUE_NAME=org.example.cinterop",
            "$KLIB_PROPERTY_INTEROP=true",
            "linkerOpts=-framework Foundation",
            "linkerOpts.ios_arm64=-lsqlite3",
            "linkerOpts.linux_x64=-lpthread",
            "compilerOpts.arm64=-DARM64_ENABLED",
            "headers=base.h",
            "headers.ios=ios_specific.h",
            "custom.dotted=untouched_value",
            "$KLIB_PROPERTY_UNIQUE_NAME.ios_arm64=ignored_unique_name_suffix",
        )

        val inputs = listOf(
            createPlainPropertiesFromLines(lines),
            createPropertiesFromBytes(lines.joinToString(separator = "\n").toByteArray(StandardCharsets.UTF_8)),
        )

        for (input in inputs) {
            val snapshotBefore = mutableMapOf<String, String>()
            for (name in input.stringPropertyNames()) {
                snapshotBefore[name] = input.getProperty(name)
            }

            val substituted = input.substituteFor(KonanTarget.IOS_ARM64)
            assertNotSame(input, substituted)

            assertEquals("-framework Foundation -lsqlite3", substituted.getProperty("linkerOpts"))
            assertEquals("-DARM64_ENABLED", substituted.getProperty("compilerOpts"))
            assertEquals("base.h ios_specific.h", substituted.getProperty("headers"))
            assertEquals("untouched_value", substituted.getProperty("custom.dotted"))
            assertEquals("org.example.cinterop", substituted.getProperty(KLIB_PROPERTY_UNIQUE_NAME))
            assertEquals("-lsqlite3", substituted.getProperty("linkerOpts.ios_arm64"))
            assertEquals("-lpthread", substituted.getProperty("linkerOpts.linux_x64"))

            // Verify input Properties was not mutated in-place.
            assertEquals("-framework Foundation", input.getProperty("linkerOpts"))
            assertNull(input.getProperty("compilerOpts"))
            assertEquals("base.h", input.getProperty("headers"))
            val snapshotAfter = mutableMapOf<String, String>()
            for (name in input.stringPropertyNames()) {
                snapshotAfter[name] = input.getProperty(name)
            }
            assertEquals(snapshotBefore, snapshotAfter)
        }
    }

    private fun doTestSubstitution(basePropertyName: String, isSuccessExpected: Boolean) {
        val stringListValues: List<List<String>?> = listOf<List<String>?>(null) + buildList {
            repeat(5) { listSize ->
                this + List(listSize) { KlibMockDSL.generateRandomName(20) }
            }
        }

        for (basePropertyValue: List<String>? in stringListValues) {
            val propertiesRaw = generateProperties(basePropertyName, basePropertyValue)

            val allButBasePropertiesRaw: Map<String, String> = propertiesRaw.stringPropertyNames()
                .filterNot { it == basePropertyName }
                .toSet()
                .associateWith { propertiesRaw.getProperty(it) }

            val basePropertyRawValue: String? = propertiesRaw.getProperty(basePropertyName)

            for (target in KonanTarget.predefinedTargets.values) {
                val propertiesSubstituted = propertiesRaw.substituteFor(target)

                val allButBasePropertiesSubstituted: Map<String, String> = propertiesSubstituted.stringPropertyNames()
                    .filterNot { it == basePropertyName }
                    .toSet()
                    .associateWith { propertiesSubstituted.getProperty(it) }

                // All untouched properties should stay the same.
                assertEquals(allButBasePropertiesRaw, allButBasePropertiesSubstituted)

                val basePropertySubstitutedValue: String? = propertiesSubstituted.getProperty(basePropertyName)
                val basePropertyExpectedValue: String? = if (isSuccessExpected) {
                    buildString {
                        if (!basePropertyRawValue.isNullOrBlank()) {
                            append(basePropertyRawValue).append(' ')
                        }
                        append(target.visibleName).append(' ')
                        append(target.visibleName.uppercase()).append(' ')
                        append(target.architecture.visibleName).append(' ')
                        append(target.family.visibleName)
                    }
                } else {
                    basePropertyRawValue
                }

                assertEquals(basePropertyExpectedValue, basePropertySubstitutedValue)
            }
        }
    }

    companion object {
        private fun generateProperties(basePropertyName: String, basePropertyValue: List<String>?): Properties {
            val result = Properties()

            fun addProperty(suffix: String?, values: List<String>) {
                val propertyName = if (suffix != null) "$basePropertyName.$suffix" else basePropertyName
                result[propertyName] = values.joinToString(separator = " ")
            }

            if (basePropertyValue != null) addProperty(null, basePropertyValue)

            KonanTarget.predefinedTargets.values.forEach { target ->
                addProperty(target.visibleName, listOf(target.visibleName, target.visibleName.uppercase()))
            }

            Architecture.entries.forEach { arch ->
                addProperty(arch.visibleName, listOf(arch.visibleName))
            }

            Family.entries.forEach { family ->
                addProperty(family.visibleName, listOf(family.visibleName))
            }

            // Add some random properties.
            result["foo"] = "foo"
            result["bar"] = "foo bar"
            result["baz"] = "true"
            result["qux"] = ""

            return result
        }

        private fun createPlainPropertiesFromLines(lines: List<String>): Properties {
            val properties = Properties()
            for (line in lines) {
                val eqIndex = line.indexOf('=')
                val key = line.substring(0, eqIndex)
                val value = line.substring(eqIndex + 1)
                properties[key] = value
            }
            return properties
        }

        private fun createLargeDependsValue(prefix: String): String {
            val builder = StringBuilder(70_000)
            for (i in 0 until 1_800) {
                if (i > 0) builder.append(' ')
                builder.append(prefix).append("_module_").append(i).append("_artifact")
            }
            return builder.toString()
        }
    }
}
