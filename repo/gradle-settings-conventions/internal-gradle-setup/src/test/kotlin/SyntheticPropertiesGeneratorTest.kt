/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class SyntheticPropertiesGeneratorTest : BaseLocalPropertiesModifierTest() {
    @Test
    @DisplayName("Local properties modifier with synthetic properties generator that generates no properties")
    fun testEmptySyntheticPropertiesGenerator() {
        assertTrue(Files.notExists(localPropertiesFile))
        modifier.applySetup(setupFile, listOf(DummySyntheticPropertiesGenerator(empty = true)))
        assertTrue(Files.exists(localPropertiesFile))

        localPropertiesFile.propertiesFileContentAssertions { fileContents, properties ->
            assertContainsMarkersOnce(fileContents)
            assertEquals(setupFile.properties.size, properties.size)
            for ((key, value) in setupFile.properties) {
                assertEquals(value, properties[key])
            }
        }
    }

    @Test
    @DisplayName("Local properties modifier with a single synthetic properties generator")
    fun testSingleSyntheticPropertiesGenerator() {
        assertTrue(Files.notExists(localPropertiesFile))
        modifier.applySetup(setupFile, listOf(DummySyntheticPropertiesGenerator(properties = mapOf("syntheticProperty" to "someValue"))))
        assertTrue(Files.exists(localPropertiesFile))

        localPropertiesFile.propertiesFileContentAssertions { fileContents, properties ->
            assertContainsMarkersOnce(fileContents)
            val syntheticProperties = mapOf(
                "originalPropertyKeys" to setupFile.properties.keys.joinToString(","),
                "syntheticProperty" to "someValue",
            )
            val combinedProperties = syntheticProperties + setupFile.properties
            assertEquals(combinedProperties.size, properties.size)
            for ((key, value) in combinedProperties) {
                assertEquals(value, properties[key])
            }
        }
    }

    @Test
    @DisplayName("Local properties modifier with multiple synthetic properties generators")
    fun testMultipleSyntheticPropertiesGenerators() {
        assertTrue(Files.notExists(localPropertiesFile))
        val syntheticPropertiesGenerators = listOf(
            DummySyntheticPropertiesGenerator(properties = mapOf("syntheticProperty1" to "someValue1")),
            DummySyntheticPropertiesGenerator(
                properties = mapOf("syntheticProperty2" to "someValue2"),
                generateAggregatingProperty = false
            ),
            DummySyntheticPropertiesGenerator(
                properties = mapOf("syntheticProperty3" to "someValue3"),
                generateAggregatingProperty = false
            ),
        )
        modifier.applySetup(setupFile, syntheticPropertiesGenerators)
        assertTrue(Files.exists(localPropertiesFile))

        localPropertiesFile.propertiesFileContentAssertions { fileContents, properties ->
            assertContainsMarkersOnce(fileContents)
            val syntheticProperties = mapOf(
                "originalPropertyKeys" to setupFile.properties.keys.joinToString(","),
                "syntheticProperty1" to "someValue1",
                "syntheticProperty2" to "someValue2",
                "syntheticProperty3" to "someValue3",
            )
            val combinedProperties = syntheticProperties + setupFile.properties
            assertEquals(combinedProperties.size, properties.size)
            for ((key, value) in combinedProperties) {
                assertEquals(value, properties[key])
            }
        }
    }

    @Test
    @DisplayName("Multiple generators cannot generate the same key")
    fun testMultipleGeneratorsGeneratingTheSameProperty() {
        assertTrue(Files.notExists(localPropertiesFile))
        val syntheticPropertiesGenerators = listOf(
            DummySyntheticPropertiesGenerator(),
            DummySyntheticPropertiesGenerator(),
        )
        val exceptionMessage = assertThrows<IllegalArgumentException> {
            modifier.applySetup(setupFile, syntheticPropertiesGenerators)
        }.message
        assertEquals("These keys were defined previously: originalPropertyKeys", exceptionMessage)
    }

    @Test
    @DisplayName("Error message contains all duplicated keys from synthetic properties")
    fun testMultipleGeneratorsGeneratingMultipleSameProperties() {
        assertTrue(Files.notExists(localPropertiesFile))
        val syntheticPropertiesGenerators = listOf(
            DummySyntheticPropertiesGenerator(),
            DummySyntheticPropertiesGenerator(properties = mapOf("syntheticProperty1" to "someValue2")),
            DummySyntheticPropertiesGenerator(properties = mapOf("syntheticProperty1" to "someValue2")),
        )
        val exceptionMessage = assertThrows<IllegalArgumentException> {
            modifier.applySetup(setupFile, syntheticPropertiesGenerators)
        }.message
        assertNotNull(exceptionMessage)
        assertContains("These keys were defined previously:", exceptionMessage)
        // the order does not matter, so test them separately
        assertContains("originalPropertyKeys", exceptionMessage)
        assertContains("syntheticProperty1", exceptionMessage)
    }

    private class DummySyntheticPropertiesGenerator(
        private val empty: Boolean = false,
        private val properties: Map<String, String> = emptyMap(),
        private val generateAggregatingProperty: Boolean = true,
    ) : SyntheticPropertiesGenerator {
        override fun generate(setupFile: SetupFile): Map<String, String> {
            return if (empty) {
                emptyMap()
            } else {
                val originalPropertyKeys = setupFile.properties.keys.joinToString(",")
                if (generateAggregatingProperty) {
                    properties + ("originalPropertyKeys" to originalPropertyKeys)
                } else {
                    properties
                }
            }
        }

    }
}