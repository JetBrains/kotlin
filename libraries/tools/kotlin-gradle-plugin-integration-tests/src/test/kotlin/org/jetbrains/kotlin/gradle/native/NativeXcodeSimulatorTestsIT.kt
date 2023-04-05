/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

@DisplayName("tests for the K/N simulator test infrastructure")
@NativeGradlePluginTests
@EnabledOnOs(OS.MAC)
class NativeXcodeSimulatorTestsIT : KGPBaseTest() {
    @DisplayName("iOS simulator test with an https request fails with default settings")
    @GradleTest
    fun checkSimulatorTestFailsInStandaloneMode(gradleVersion: GradleVersion) {
        project("native-test-ios-https-request", gradleVersion) {
            buildAndFail("check") {
                // that's an Apple's issue that we expect to fail the build
                assertOutputContains("The certificate for this server is invalid. You might be connecting to a server that is pretending to be ")
            }
        }
    }
}