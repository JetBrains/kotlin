/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BuildScanUsernameGeneratorTest {
    @Test
    @DisplayName("no salt -> no username")
    fun noSaltProvided() {
        withSystemProperty("user.name", "user") {
            val generatedProperties = BuildScanUsernameGenerator().generate(SetupFile(properties = emptyMap()))
            assertMapIsEmpty(generatedProperties)
        }
    }

    @Test
    @DisplayName("username = user, salt = salt1")
    fun sample0() {
        withSystemProperty("user.name", "user") {
            val generatedProperties = BuildScanUsernameGenerator().generate(SetupFile(properties = emptyMap(), obfuscationSalt = "salt1"))
            assertContainsKey(generatedProperties, "kotlin.build.scan.username")
            val generatedUsername = generatedProperties["kotlin.build.scan.username"]
            assertEquals("8b2f06c5dabafac025ec23e5a19be6e89f796da5490d4d1b12313c332c65fbc6", generatedUsername)
        }
    }

    @Test
    @DisplayName("username = user, salt = salt2")
    fun sample1() {
        withSystemProperty("user.name", "user") {
            val generatedProperties = BuildScanUsernameGenerator().generate(SetupFile(properties = emptyMap(), obfuscationSalt = "salt2"))
            assertContainsKey(generatedProperties, "kotlin.build.scan.username")
            val generatedUsername = generatedProperties["kotlin.build.scan.username"]
            assertEquals("8f4ea78a909732281e5e26bad981592042eb669e1caaacb9811050d0d88848b7", generatedUsername)
        }
    }

    @Test
    @DisplayName("username = user2, salt = salt1")
    fun sample2() {
        withSystemProperty("user.name", "user2") {
            val generatedProperties = BuildScanUsernameGenerator().generate(SetupFile(properties = emptyMap(), obfuscationSalt = "salt1"))
            assertContainsKey(generatedProperties, "kotlin.build.scan.username")
            val generatedUsername = generatedProperties["kotlin.build.scan.username"]
            assertEquals("1f9c51bc3adb3590aabadfdfa6fa9481d3263ada751d0de7a31d9e9f489e7d31", generatedUsername)
        }
    }

    @Test
    @DisplayName("username = user2, salt = salt2")
    fun sample3() {
        withSystemProperty("user.name", "user2") {
            val generatedProperties = BuildScanUsernameGenerator().generate(SetupFile(properties = emptyMap(), obfuscationSalt = "salt2"))
            assertContainsKey(generatedProperties, "kotlin.build.scan.username")
            val generatedUsername = generatedProperties["kotlin.build.scan.username"]
            assertEquals("abf072b569bd7bfe3831b9c1bc4d6db701dd0703016970db45b839eb493002cf", generatedUsername)
        }
    }
}