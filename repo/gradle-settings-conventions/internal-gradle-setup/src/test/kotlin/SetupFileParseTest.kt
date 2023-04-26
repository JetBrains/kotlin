/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import kotlin.test.*

class SetupFileParseTest {
    private fun openPropertiesJsonStream() =
        SetupFileParseTest::class.java.classLoader.getResourceAsStream("properties.json")
            ?: error("No properties.json file found in test resources")

    @Test
    fun testSimpleParsing() {
        val setupFile = openPropertiesJsonStream().use {
            parseSetupFile(it)
        }
        assertEquals(
            mapOf(
                "newProperty1" to "someValue",
                "newProperty2" to "someOtherValue",
                "alreadySetProperty" to "newValue",
            ),
            setupFile.properties
        )
    }
}