/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.util.DefFileProperty
import org.jetbrains.kotlin.konan.util.substituteFor
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.library.KLIB_PROPERTY_ABI_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_INTEROP
import org.jetbrains.kotlin.library.KLIB_PROPERTY_IR_PROVIDER
import org.jetbrains.kotlin.library.KLIB_PROPERTY_METADATA_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.jetbrains.kotlin.library.KlibMockDSL
import org.junit.jupiter.api.Test
import java.util.Properties
import org.junit.jupiter.api.Assertions.assertEquals

class PropertySubstitutionTest {
    @Test
    fun `Test substitution works for the limited list of known properties`() {
        DefFileProperty.StringListProperty.entries.forEach { property ->
            doTestSuccessfulSubstitution(basePropertyName = property.propertyName)
        }
    }

    @Test
    fun `Test substitution does not work for other properties`() {
        buildSet {
            DefFileProperty.NullableStringProperty.entries.mapTo(this) { it.propertyName }
            DefFileProperty.BooleanProperty.entries.mapTo(this) { it.propertyName }

            this += KLIB_PROPERTY_UNIQUE_NAME
            this += KLIB_PROPERTY_METADATA_VERSION
            this += KLIB_PROPERTY_ABI_VERSION
            this += KLIB_PROPERTY_INTEROP
            this += KLIB_PROPERTY_IR_PROVIDER
        }.forEach { basePropertyName ->
            // TODO: there should be no substitution at all
            doTestSuccessfulSubstitution(basePropertyName)
        }
    }

    private fun doTestSuccessfulSubstitution(basePropertyName: String) {
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

                val basePropertySubstitutedValue: String = propertiesSubstituted.getProperty(basePropertyName)
                val basePropertyExpectedValue: String = buildString {
                    if (!basePropertyRawValue.isNullOrBlank()) {
                        append(basePropertyRawValue).append(' ')
                    }
                    append(target.visibleName).append(' ')
                    append(target.visibleName.uppercase()).append(' ')
                    append(target.architecture.visibleName).append(' ')
                    append(target.family.visibleName)
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
    }
}
