/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.junit.jupiter.api.DisplayName
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class LocalPropertiesModifierTest : BaseLocalPropertiesModifierTest() {
    @Test
    @DisplayName("sync is able to create local.properties file")
    fun testSyncingWithAbsentFile() {
        assertTrue(Files.notExists(localPropertiesFile))
        modifier.applySetup(setupFile)
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
    @DisplayName("sync populates empty local.properties file")
    fun testSyncingIntoEmptyFile() {
        Files.createFile(localPropertiesFile)
        modifier.applySetup(setupFile)
        assertTrue(Files.exists(localPropertiesFile))

        localPropertiesFile.propertiesFileContentAssertions { fileContents, properties ->
            assertContainsMarkersOnce(fileContents)
            assertEquals(setupFile.properties.size, properties.size)
            for ((key, value) in setupFile.properties) {
                assertEquals(value, properties[key])
            }
        }
    }

    /**
     * Checks that a file like
     * ```
     * a=1
     * b=2
     * c=3
     * ```
     * is being transformed into
     * ```
     * a=1
     * b=2
     * c=3
     * #header
     * d=4
     * f=5
     * #footer
     * ```
     */
    @Test
    @DisplayName("sync shouldn't remove any existing properties not managed by the sync")
    fun testSyncingIntoNonEmptyFile() {
        val initialContent = mapOf(
            "oldProperty1" to PropertyValue.Configured("oldValue1"),
            "oldProperty2" to PropertyValue.Configured("oldValue2"),
        )
        fillInitialLocalPropertiesFile(initialContent)

        modifier.applySetup(setupFile)

        localPropertiesFile.propertiesFileContentAssertions { fileContents, properties ->
            assertContainsMarkersOnce(fileContents)
            val expectedProperties = setupFile.properties + initialContent.mapValues { it.value.value }
            assertEquals(expectedProperties.size, properties.size)
            for ((key, value) in expectedProperties) {
                assertEquals(value, properties[key])
            }
        }
    }

    /**
     * Checks that a file like
     * ```
     * a=1
     * b=2
     * f=3
     * ```
     * is being transformed into
     * ```
     * a=1
     * b=2
     * c=3
     * #header
     * d=4
     * #footer
     * ```
     */
    @Test
    @DisplayName("sync shouldn't override properties if they already manually set")
    fun testSyncingDoesNotOverrideValues() {
        val initialContent = mapOf(
            "oldProperty1" to PropertyValue.Configured("oldValue1"),
            "oldProperty2" to PropertyValue.Configured("oldValue2"),
            "alreadySetProperty" to PropertyValue.Configured("oldValue3"),
        )
        fillInitialLocalPropertiesFile(initialContent)

        modifier.applySetup(setupFile)

        localPropertiesFile.propertiesFileContentAssertions { fileContents, properties ->
            assertContainsMarkersOnce(fileContents)
            val expectedProperties = setupFile.properties + initialContent.mapValues { it.value.value }
            assertEquals(expectedProperties.size, properties.size)
            for ((key, value) in expectedProperties) {
                assertEquals(value, properties[key])
            }
            assertContainsExactTimes(fileContents, "#alreadySetProperty=newValue the property is overridden by 'oldValue3'", 1)
        }
    }

    /**
     * Checks that a file like
     * ```
     * a=1
     * b=2
     * c=3
     * #header
     * d=4
     * #footer
     * e=5
     * ```
     * is being transformed into
     * ```
     * a=1
     * b=2
     * c=3
     * e=5
     * #header
     * d=10
     * #footer
     * ```
     */
    @Test
    @DisplayName("sync should override automatically set properties")
    fun testSyncingOverrideAutomaticallySetValues() {
        val initialContent = mapOf(
            "oldProperty1" to PropertyValue.Configured("oldValue1"),
            "oldProperty2" to PropertyValue.Configured("oldValue2"),
            "alreadySetProperty" to PropertyValue.Configured("oldValue3"),
        )
        fillInitialLocalPropertiesFile(initialContent)

        modifier.applySetup(setupFile)

        val newProperties = mapOf(
            "newManualProperty" to PropertyValue.Configured("5"),
            "otherAlreadySetProperty" to PropertyValue.Configured("5"),
        )
        fillInitialLocalPropertiesFile(newProperties)

        val anotherSetupFile = SetupFile(
            mapOf(
                "newProperty2" to "other", // a new value
                "newProperty3" to "someOtherValue", // a new record
                "otherAlreadySetProperty" to "someOtherValue",
            )
        )

        modifier.applySetup(anotherSetupFile)

        localPropertiesFile.propertiesFileContentAssertions { fileContents, properties ->
            assertContainsMarkersOnce(fileContents)
            val expectedProperties =
                anotherSetupFile.properties + initialContent.mapValues { it.value.value } + newProperties.mapValues { it.value.value }
            assertEquals(expectedProperties.size, properties.size)
            for ((key, value) in expectedProperties) {
                assertEquals(value, properties[key])
            }
        }
    }

    private fun fillInitialLocalPropertiesFile(content: Map<String, PropertyValue>) {
        val localPropertiesFile = localPropertiesFile.toFile()
        localPropertiesFile.appendText(
            """
            |${content.asPropertiesLines}
            """.trimMargin()
        )
    }
}