/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests an Upgrade sequence for Kotlin direct integration")
@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
@NativeGradlePluginTests
class XcodeDirectIntegrationUpgradeSequenceIT : KGPBaseTest() {

    /**
     * This test works by running a sequence of "xcodebuild test" incrementally against changes of Kotlin version. The test project:
     * 1. Generates a Kotlin source with [TestVersion.generatedValue]
     * 2. Runs an XCTest linked against compiled Kotlin version [TestVersion.kotlinVersion] and checks that [TestVersion.generatedValue] matches [TestVersion.expectedValue]
     */
    @DisplayName("Test embedAndSign integration Kotlin version sequence")
    @ParameterizedTest(name = "{displayName} with Gradle: {0}, Kotlin versions: {1}")
    @ArgumentsSource(VersionSequenceProvider::class)
    fun `KT-68257`(
        gradleVersion: GradleVersion,
        versionSequence: List<TestVersion>,
    ) {
        project("xcodeDirectIntegrationVersionSequence", gradleVersion) {
            XCTestHelpers().use {
                val simulator = it.createSimulator().apply {
                    boot()
                }

                versionSequence.forEach {
                    runXcodeTestWithKotlinVersion(
                        simulatorUdid = simulator.udid,
                        expectedValue = it.expectedValue,
                        generatedValue = it.generatedValue,
                        kotlinVersion = it.kotlinVersion,
                    )
                }
            }
        }
    }

    private fun TestProject.runXcodeTestWithKotlinVersion(
        simulatorUdid: String,
        expectedValue: String,
        generatedValue: String,
        kotlinVersion: String,
    ) {
        buildXcodeProject(
            xcodeproj = projectPath.resolve("iosApp/iosApp.xcodeproj"),
            scheme = "iosAppTests",
            destination = "id=${simulatorUdid}",
            buildMode = XcodeBuildMode.TEST,
            testRunEnvironment = mapOf(
                "EXPECTED_TEST_VALUE" to expectedValue
            ),
            buildSettingOverrides = mapOf(
                "FRAMEWORK_SEARCH_PATHS" to "\$(SRCROOT)/../shared/build/xcode-frameworks/\$(CONFIGURATION)/\$(SDK_NAME)",
                "EMBED_AND_SIGN_KOTLIN_VERSION" to kotlinVersion,
                "EMBED_AND_SIGN_GENERATED_TEST_VALUE" to generatedValue,
            ),
        )
    }

    data class TestVersion(
        val expectedValue: String,
        val generatedValue: String,
        val kotlinVersion: String,
    )

    internal class VersionSequenceProvider : GradleArgumentsProvider() {
        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            return super.provideArguments(context).flatMap { arguments ->
                val gradleVersion = arguments.get().first()
                Stream.of(
                    // Reproduce KT-68257
                    listOf(
                        TestVersion("1", "1", "1.9.23"),
                        TestVersion("1", "2", "2.0.0"),
                        TestVersion("3", "3", KOTLIN_VERSION),
                    ),
                    // Make sure going from 1.9.23 to current version works
                    listOf(
                        TestVersion("1", "1", "1.9.23"),
                        TestVersion("2", "2", KOTLIN_VERSION),
                    ),
                    // Make sure going from 2.0.0 to current version works since we need to remove a directory in BUILT_PRODUCTS_DIR
                    listOf(
                        TestVersion("1", "1", "2.0.0"),
                        TestVersion("2", "2", KOTLIN_VERSION),
                    ),
                    // Make sure going backwards also works
                    listOf(
                        TestVersion("1", "1", KOTLIN_VERSION),
                        TestVersion("2", "2", "2.0.0"),
                    ),
                    listOf(
                        TestVersion("1", "1", KOTLIN_VERSION),
                        TestVersion("2", "2", "1.9.23"),
                    ),
                ).map { versions ->
                    Arguments.of(gradleVersion, versions)
                }
            }
        }
    }

}