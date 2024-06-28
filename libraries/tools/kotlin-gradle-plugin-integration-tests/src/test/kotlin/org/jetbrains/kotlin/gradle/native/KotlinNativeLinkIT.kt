/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
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
            gradleVersion
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

    @DisplayName("KT-60839: should provide correct default value for -Xpartial-linkage")
    @GradleTest
    @OsCondition(
        supportedOn = [OS.LINUX, OS.MAC], // Don't run it on Windows. Caches are not supported there yet.
        enabledOnCI = [OS.LINUX]
    )
    fun defaultValueForPartialLinkage(gradleVersion: GradleVersion) {
        nativeProject(
            "kt-60839-native-link-cache-builder",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                // KT-60839 only reproduces when the build cache is enabled,
                // but we must ignore it when running this test in order to
                // ensure we actually try to pass -Xpartial-linkage to konanc.
                buildCacheEnabled = true,
                freeArgs = defaultBuildOptions.freeArgs + "--rerun-tasks",
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    cacheKind = NativeCacheKind.STATIC,
                    // Required as this only reproduces from CacheBuilder.
                    cacheOrchestration = "gradle"
                )
            ),
        ) {

            // Must be an unoptimized debug build.
            build("linkDebugTestHost") {
                extractNativeTasksCommandLineArgumentsFromOutput(":linkDebugTestHost") {
                    assertCommandLineArgumentsContain("-Xpartial-linkage=ENABLE")
                }
            }
        }
    }
}
