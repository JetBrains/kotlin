/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for running Swift Export XCTests")
@SwiftExportGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
class SwiftExportXCIT : KGPBaseTest() {

    private val simulatorName = "JBiPhone"
    private val deviceType = "iPhone 15"
    private val destination get() = "platform=iOS Simulator,name=$simulatorName"

    @BeforeAll
    fun setUp(@TempDir processDir: Path) {
        val result = runProcess(
            listOf(
                "xcrun", "simctl", "create", simulatorName,
                deviceType
            ),
            processDir.toFile()
        )

        assert(result.isSuccessful) {
            "Could not create simulator: ${result.stdErr}"
        }
    }

    @DisplayName("run XCTests for testing Swift Export")
    @GradleTest
    fun testSwiftExportXCTests(
        gradleVersion: GradleVersion
    ) {
        nativeProject("simpleSwiftExport", gradleVersion) {
            projectPath.enableSwiftExport()

            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
                destination = destination,
                test = true,
                extraArguments = listOf("VALID_ARCHS=arm64")
            )
        }
    }

    @AfterAll
    fun shutDown(@TempDir processDir: Path) {
        val result = runProcess(
            listOf(
                "xcrun", "simctl", "delete", simulatorName
            ),
            processDir.toFile()
        )

        assert(result.isSuccessful) {
            "Could not delete simulator: ${result.stdErr}"
        }
    }
}