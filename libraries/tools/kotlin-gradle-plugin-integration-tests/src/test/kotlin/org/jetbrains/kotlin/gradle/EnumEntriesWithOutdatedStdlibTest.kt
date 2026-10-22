/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInFrontend
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName

@DisplayName("EnumEntries feature test when no kotlin.enums.EnumEntries found on the classpath")
@JvmGradlePluginTests
@MustRunOnChangesInGradle
@MustRunOnChangesInFrontend
class EnumEntriesWithOutdatedStdlibTest : KGPBaseTest() {
    @DisplayName("EnumEntries should not be accessible without kotlin.enums.EnumEntries")
    @GradleTest
    fun enumEntriesNotAccessible(gradleVersion: GradleVersion) {
        project(
            "enumEntriesNotAccessible",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Fail),
        ) {
            buildAndFail(":compileKotlin") {
                assertOutputContains("Main.kt:13:20 Unresolved reference 'entries'.")
                assertOutputContains("Main.kt:14:30 Unresolved reference 'entries'.")
            }
        }
    }

    @DisplayName("Code should compile normally if `entries` is never referenced")
    @GradleTest
    fun codeCompilesWithoutReferencingEntries(gradleVersion: GradleVersion) {
        project("enumEntriesNotAccessible", gradleVersion) {
            projectPath.resolve("src/main/kotlin/Main.kt")
                .replaceText("entries", "values()")
            build(":compileKotlin")
        }
    }
}
