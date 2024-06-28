/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Xcode <-> Kotlin direct integration")
@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
@NativeGradlePluginTests
class XcodeDirectIntegrationIT : KGPBaseTest() {

    @DisplayName("Xcode direct integration")
    @ParameterizedTest(name = "{displayName} with {1}, {0} and isStatic={2}")
    @ArgumentsSource(XcodeArgumentsProvider::class)
    fun test(
        gradleVersion: GradleVersion,
        iosApp: String,
        isStatic: Boolean,
    ) {

        project("xcodeDirectIntegration", gradleVersion) {

            projectPath.resolve("shared/build.gradle.kts")
                .modify { it.replace(".framework {", ".framework {\n     isStatic = $isStatic") }

            buildXcodeProject(xcodeproj = projectPath.resolve("iosApp$iosApp/iosApp.xcodeproj"))
        }
    }


    internal class XcodeArgumentsProvider : GradleArgumentsProvider() {
        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
            return super.provideArguments(context).flatMap { arguments ->
                val gradleVersion = arguments.get().first()
                Stream.of("BuildPhase", "SchemePreAction", "SchemePreActionSpm").flatMap { iosApp ->
                    Stream.of(true, false).map { isStatic ->
                        Arguments.of(gradleVersion, iosApp, isStatic)
                    }
                }
            }
        }
    }
}
