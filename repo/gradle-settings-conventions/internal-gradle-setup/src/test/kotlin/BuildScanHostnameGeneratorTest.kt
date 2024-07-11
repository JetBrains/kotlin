/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BuildScanHostnameGeneratorTest {
    @Test
    @DisplayName("no salt -> no hostname")
    fun noSaltProvided() {
        withSystemProperty("kotlin.build.hostname.for.tests", "host") {
            val generatedProperties = BuildScanHostnameGenerator().generate(SetupFile(properties = emptyMap()))
            assertMapIsEmpty(generatedProperties)
        }
    }

    @Test
    @DisplayName("hostname = host1, salt = salt1")
    fun sample0() {
        withSystemProperty("kotlin.build.hostname.for.tests", "host1") {
            val generatedProperties = BuildScanHostnameGenerator().generate(SetupFile(properties = emptyMap(), obfuscationSalt = "salt1"))
            assertContainsKey(generatedProperties, "kotlin.build.scan.hostname")
            val generatedUsername = generatedProperties["kotlin.build.scan.hostname"]
            assertEquals("54f75119a4461f4b2e8928d0ef22215cd27c53d83e1dfcaadedf2e99f1911079", generatedUsername)
        }
    }

    @Test
    @DisplayName("hostname = host1, salt = salt2")
    fun sample1() {
        withSystemProperty("kotlin.build.hostname.for.tests", "host1") {
            val generatedProperties = BuildScanHostnameGenerator().generate(SetupFile(properties = emptyMap(), obfuscationSalt = "salt2"))
            assertContainsKey(generatedProperties, "kotlin.build.scan.hostname")
            val generatedUsername = generatedProperties["kotlin.build.scan.hostname"]
            assertEquals("2be2478031efb385d467a6f264e85bcff0b4bb44fc2f3a3665ef3e4e911528f5", generatedUsername)
        }
    }

    @Test
    @DisplayName("hostname = host2, salt = salt1")
    fun sample02() {
        withSystemProperty("kotlin.build.hostname.for.tests", "host2") {
            val generatedProperties = BuildScanHostnameGenerator().generate(SetupFile(properties = emptyMap(), obfuscationSalt = "salt1"))
            assertContainsKey(generatedProperties, "kotlin.build.scan.hostname")
            val generatedUsername = generatedProperties["kotlin.build.scan.hostname"]
            assertEquals("1ba56ceedffeaa4782a4f1ec5f224136179053b86c4247a970a2b2d698e355b8", generatedUsername)
        }
    }

    @Test
    @DisplayName("hostname = host2, salt = salt2")
    fun sample3() {
        withSystemProperty("kotlin.build.hostname.for.tests", "host2") {
            val generatedProperties = BuildScanHostnameGenerator().generate(SetupFile(properties = emptyMap(), obfuscationSalt = "salt2"))
            assertContainsKey(generatedProperties, "kotlin.build.scan.hostname")
            val generatedUsername = generatedProperties["kotlin.build.scan.hostname"]
            assertEquals("f779296f8ed623d212d5e91bc14f00ca2262ed3a91dedbb53d2c269483f4aaf9", generatedUsername)
        }
    }
}