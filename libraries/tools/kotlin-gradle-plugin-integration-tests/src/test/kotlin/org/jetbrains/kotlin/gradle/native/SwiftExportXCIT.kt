/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for running Swift Export XCTests")
@SwiftExportGradlePluginTests
class SwiftExportXCIT : KGPBaseTest() {

    @DisplayName("run XCTests for testing Swift Export")
    @GradleTest
    fun testSwiftExportXCTests(
        gradleVersion: GradleVersion
    ) {
        XCTestHelpers().use {
            val simulator = it.createSimulator().apply {
                boot()
            }

            nativeProject(
                "simpleSwiftExport",
                gradleVersion,
                buildOptions = defaultBuildOptions.copy(
                    configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                    nativeOptions = BuildOptions.NativeOptions().copy(
                        swiftExportEnabled = true,
                    )
                )
            ) {
                buildXcodeProject(
                    xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
                    destination = "platform=iOS Simulator,id=${simulator.udid}",
                    buildMode = XcodeBuildMode.TEST,
                    appendToProperties = { "kotlin.experimental.swift-export.enabled=true" }
                )
            }
        }
    }
}