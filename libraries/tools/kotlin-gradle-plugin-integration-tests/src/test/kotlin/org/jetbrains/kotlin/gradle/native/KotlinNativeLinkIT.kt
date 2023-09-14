/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("KotlinNativeLink task tests")
@NativeGradlePluginTests
internal class KotlinNativeLinkIT : KGPBaseTest() {

    @DisplayName("KT-54113: afterEvaluate to sync languageSettings should run out of configuration methods scope")
    @GradleTest
    fun shouldSyncLanguageSettingsSafely(gradleVersion: GradleVersion) {
        nativeProject("native-link-simple", gradleVersion) {
            build("tasks")
        }
    }

    @DisplayName("KT-56280: should propagate freeCompilerArgs from compilation")
    @GradleTest
    fun shouldUseCompilationFreeCompilerArgs(gradleVersion: GradleVersion) {
        nativeProject(
            "native-link-simple",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                """
                |
                |kotlin {
                |    targets.named("host").configure {
                |        binaries.executable()
                |    }
                |    
                |    targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.class) {
                |        compilations.main.kotlinOptions {
                |            freeCompilerArgs += ["-e", "main"]
                |        }
                |    }
                |}
                """.trimMargin()
            )

            build("linkReleaseExecutableHost") {
                extractNativeTasksCommandLineArgumentsFromOutput(":linkReleaseExecutableHost") {
                    assertCommandLineArgumentsContain("-e", "main")
                    assertNoDuplicates()
                }
            }
        }
    }
}