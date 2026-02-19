/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.util.stream.Stream
import kotlin.io.path.isSymbolicLink
import kotlin.test.assertEquals

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Xcode <-> Kotlin direct integration")
@NativeGradlePluginTests
class XcodeDirectIntegrationIT : KGPBaseTest() {

    @DisplayName("Xcode direct integration")
    @ParameterizedTest(name = "{displayName} with {1}, {0} and isStatic={2}")
    @ArgumentsSource(DirectIntegrationTestArgumentsProvider::class)
    fun test(
        gradleVersion: GradleVersion,
        iosApp: String,
        isStatic: Boolean,
    ) {
        project("xcodeDirectIntegration", gradleVersion) {
            projectPath
                .resolve("shared/build.gradle.kts")
                .replaceText("<is_static>", if (isStatic) "true" else "false")
            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp$iosApp/iosApp.xcodeproj"),
            )
        }
    }

    internal class DirectIntegrationTestArgumentsProvider : GradleArgumentsProvider() {
        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            return super.provideArguments(context).flatMap { arguments ->
                val gradleVersion = arguments.get().first()
                Stream.of(SchemePreAction, SchemePreActionSpm).flatMap { iosApp ->
                    Stream.of(true, false).map { isStatic ->
                        Arguments.of(gradleVersion, iosApp, isStatic)
                    }
                }
            }
        }
    }

    @DisplayName("Xcode archiving")
    @ParameterizedTest(name = "{displayName} with {0} and isStatic={1}")
    @ArgumentsSource(ArchivingTestArgumentsProvider::class)
    fun testArchiving(
        gradleVersion: GradleVersion,
        isStatic: Boolean,
    ) {
        project("xcodeDirectIntegration", gradleVersion) {
            projectPath.resolve("shared/build.gradle.kts")
                .replaceText("<is_static>", if (isStatic) "true" else "false")

            val archivePath = projectPath.resolve("archive.xcarchive")
            buildXcodeProject(
                xcodeproj = projectPath.resolve("iosApp$BuildPhase/iosApp.xcodeproj"),
                action = XcodeBuildAction.Archive(archivePath.toFile().path),
                destination = "generic/platform=iOS",
                buildSettingOverrides = mapOf(
                    // Disable signing. We are building the device but don't have an identity in the Keychain
                    "CODE_SIGN_IDENTITY" to "",
                )
            )

            // Sanity check
            assertFileExists(archivePath.resolve("Products/Applications/iosApp.app/iosApp"))
            assertDirectoryExists(archivePath.resolve("dSYMs/iosApp.app.dSYM"))

            val frameworkPath = archivePath.resolve("Products/Applications/iosApp.app/Frameworks/shared.framework")
            val dsymPath = archivePath.resolve("dSYMs/shared.framework.dSYM")

            if (isStatic) {
                assertDirectoryDoesNotExist(frameworkPath)
                assertDirectoryDoesNotExist(dsymPath)
            } else {
                assertDirectoryExists(frameworkPath)
                assert(!frameworkPath.isSymbolicLink()) { "${frameworkPath} is a symbolic link" }
                assertDirectoryExists(dsymPath)
                assert(!dsymPath.isSymbolicLink()) { "${dsymPath} is a symbolic link" }

                fun dumpUuid(binary: File): List<String> {
                    return runProcess(listOf("otool", "-l", binary.path), projectPath.toFile())
                        .output.lines().filter { it.contains("uuid") }
                }

                // Framework and the dSYM binary must have identical UUID
                val frameworkUuids = dumpUuid(frameworkPath.resolve("shared").toFile())
                val dsymUuids = dumpUuid(dsymPath.resolve("Contents/Resources/DWARF/shared").toFile())

                assert(!frameworkUuids.isEmpty())
                assertEquals(frameworkUuids, dsymUuids)
            }
        }
    }

    internal class ArchivingTestArgumentsProvider : GradleArgumentsProvider() {
        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            return super.provideArguments(context).flatMap { arguments ->
                val gradleVersion = arguments.get().first()
                Stream.of(true, false).map { isStatic ->
                    Arguments.of(gradleVersion, isStatic)
                }
            }
        }
    }

    private companion object {
        val BuildPhase = "BuildPhase"
        val SchemePreAction = "SchemePreAction"
        val SchemePreActionSpm = "SchemePreActionSpm"
    }
}
