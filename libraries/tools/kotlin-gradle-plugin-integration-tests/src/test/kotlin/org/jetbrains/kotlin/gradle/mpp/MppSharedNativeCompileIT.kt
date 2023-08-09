/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.properties.hasProperty
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.fail

@MppGradlePluginTests
@DisplayName("Tests for multiplatform | shared native compilations")
class MppSharedNativeCompileIT : KGPBaseTest() {
    /**
     * https://youtrack.jetbrains.com/issue/KT-56205/Shared-Native-Compilation-False-positive-w-Could-not-find-warnings-on-metadata-klibs
     * metadata klib should not contain any dependsOn= in their klib manifest.
     */
    @GradleTest
    fun `test - shared native klib - does not contain 'depends=' manifest property`(gradleVersion: GradleVersion) {
        project("kt-54995-compileSharedNative-with-okio", gradleVersion) {
            build("compileNativeMainKotlinMetadata") {
                val nativeMainKlib = projectPath.resolve("build/classes/kotlin/metadata/nativeMain/klib/test-project_nativeMain.klib")
                assertFileExists(nativeMainKlib)

                val libraryFile = org.jetbrains.kotlin.library.resolveSingleFileKlib(
                    org.jetbrains.kotlin.konan.file.File(nativeMainKlib),
                    strategy = ToolingSingleFileKlibResolveStrategy
                )

                if (libraryFile.unresolvedDependencies.isNotEmpty()) {
                    fail("Expected metadata klib to not list dependencies. Found ${libraryFile.unresolvedDependencies}")
                }

                if (libraryFile.manifestProperties.hasProperty(KLIB_PROPERTY_DEPENDS)) {
                    fail(
                        "Expected metadata klib to not contain $KLIB_PROPERTY_DEPENDS. " +
                                "Value: ${libraryFile.manifestProperties.getProperty(KLIB_PROPERTY_DEPENDS)}"
                    )
                }
            }
        }
    }

    /**
     *
     */
    @GradleTest
    fun `test - K2 - shared native compilation - assemble`(gradleVersion: GradleVersion) {
        project("kt-57944-k2-native-compilation", gradleVersion, buildOptions = defaultBuildOptions.copy(languageVersion = "2.0")) {
            build("assemble") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
                assertTasksExecuted(":compileNativeMainKotlinMetadata")
                assertTasksExecuted(":compileKotlinLinuxX64")
            }
        }
    }
}
