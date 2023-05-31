/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import kotlin.test.*

class SetupFileParseTest {
    private fun openPropertiesJsonStream(name: String) =
        SetupFileParseTest::class.java.classLoader.getResourceAsStream("$name.json")
            ?: error("No properties.json file found in test resources")

    private fun assertSampleSetupFileIsParsedCorrectly(setupFile: SetupFile) {
        assertEquals(
            mapOf(
                "newProperty1" to "someValue",
                "newProperty2" to "someOtherValue",
                "alreadySetProperty" to "newValue",
            ),
            setupFile.properties
        )
    }

    @Test
    fun testSimpleParsing() {
        val setupFile = openPropertiesJsonStream("properties").use {
            parseSetupFile(it)
        }
        assertSampleSetupFileIsParsedCorrectly(setupFile)
        assertNull(setupFile.consentDetailsLink)
    }

    @Test
    fun testUnknownFieldsDoNotBreakParsing() {
        val setupFile = openPropertiesJsonStream("properties-with-unknown-fields").use {
            parseSetupFile(it)
        }
        assertSampleSetupFileIsParsedCorrectly(setupFile)
        assertNull(setupFile.consentDetailsLink)
    }

    @Test
    fun testParsingWithConsentDetailsLink() {
        val setupFile = openPropertiesJsonStream("properties-with-consent-details").use {
            parseSetupFile(it)
        }
        assertSampleSetupFileIsParsedCorrectly(setupFile)
        assertEquals(setupFile.consentDetailsLink, "https://example.org")
    }
}