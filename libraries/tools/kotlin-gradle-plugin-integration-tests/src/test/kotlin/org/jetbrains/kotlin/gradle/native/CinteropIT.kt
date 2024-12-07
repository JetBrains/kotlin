/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

@NativeGradlePluginTests
class CinteropIT : KGPBaseTest() {

    @GradleTest
    fun `rerun cinterop after a header changing`(gradleVersion: GradleVersion) {
        nativeProject("cinterop-kt-53191", gradleVersion = gradleVersion) {
            val headerFile = projectPath.resolve("native_lib/nlib.h").toFile()

            build(":compileKotlinLinux") {
                assertTasksExecuted(":cinteropNlibLinux")
            }
            build(":compileKotlinLinux") {
                assertTasksUpToDate(":cinteropNlibLinux", ":compileKotlinLinux")
            }

            headerFile.writeText("void foo();")
            buildAndFail(":compileKotlinLinux") {
                assertTasksExecuted(":cinteropNlibLinux")
                assertOutputContains("src/linuxMain/kotlin/org/sample/Platform.kt:3:10 Unresolved reference 'sample'")
            }

            headerFile.writeText("void sample(int i);")
            build(":compileKotlinLinux") {
                assertTasksExecuted(":cinteropNlibLinux")
            }
        }
    }

    @GradleTest
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    fun `cinterop gives hint about -fmodules`(gradleVersion: GradleVersion) {
        nativeProject("cinterop-fmodules", gradleVersion = gradleVersion) {
            val defFile = projectPath.resolve("native_lib/nlib.def").toFile()

            buildAndFail(":cinteropNlibIosX64") {
                assertOutputContains("Try adding `-compiler-option -fmodules` to cinterop.")
            }

            defFile.appendText("\ncompilerOpts = -fmodules")
            build(":cinteropNlibIosX64") {
                assertOutputDoesNotContain("Try adding `-compiler-option -fmodules` to cinterop.")
            }
        }
    }

    @DisplayName("KT-62795: checking that we can pass def file to cinterop task as a output file property from another task")
    @GradleTest
    fun cinteropWithDefFileFromTaskOutput(gradleVersion: GradleVersion) {
        nativeProject("cinterop-with-def-creation-task", gradleVersion = gradleVersion) {

            val defFilePath = projectPath.resolve("def/cinterop.def").toFile().absoluteFile.absolutePath

            build(":assemble") {
                assertTasksExecuted(":createDefFileTask")
                extractNativeTasksCommandLineArgumentsFromOutput(":cinteropCinteropNative", toolName = NativeToolKind.C_INTEROP) {
                    assertCommandLineArgumentsContainSequentially("-def", defFilePath)
                }
            }

            build(":assemble") {
                assertTasksUpToDate(":createDefFileTask")
                assertTasksUpToDate(":cinteropCinteropNative")
            }
        }
    }

    @DisplayName("KT-62800: passing header to cinterop instead of passing def file")
    @GradleTest
    fun cinteropWithExplicitPassingHeader(gradleVersion: GradleVersion) {
        nativeProject("cinterop-with-header", gradleVersion = gradleVersion) {
            val dummyHeaderPath = projectPath.resolve("libs").resolve("include").resolve("dummy.h").toFile().absolutePath
            build(":assemble") {
                assertTasksExecuted(":cinteropCinteropNative")
                extractNativeTasksCommandLineArgumentsFromOutput(":cinteropCinteropNative", toolName = NativeToolKind.C_INTEROP) {
                    assertCommandLineArgumentsContainSequentially("-header", dummyHeaderPath)
                    assertCommandLineArgumentsContainSequentially("-Ilibs/include")
                    assertCommandLineArgumentsContainSequentially("-pkg", "cinterop")
                    assertCommandLineArgumentsDoNotContain("-def")
                }
            }
        }
    }

    @DisplayName("KT-45559: cinterop checks headers content for up-to-date checks")
    @GradleTest
    fun cinteropHeadersContentUTDChecks(gradleVersion: GradleVersion) {
        nativeProject("KT-45559-cinterop-header-UTD-checks", gradleVersion = gradleVersion) {
            build(":cinteropCinteropNative") {
                assertTasksExecuted(":cinteropCinteropNative")
            }

            projectPath.resolve("compilerOptionIncludeDir").resolve("dummyFromCompilerOption.h").replaceText(
                "void dummyFromCompilerOption();", "void dummyFromCompilerOption(){};"
            )
            build(":cinteropCinteropNative") {
                assertTasksExecuted(":cinteropCinteropNative")
            }

            projectPath.resolve("includeDirs").resolve("dummyFromIncludeDirs.h").replaceText(
                "void dummyFromIncludeDirs();", "void dummyFromIncludeDirs(){};"
            )
            build(":cinteropCinteropNative") {
                assertTasksExecuted(":cinteropCinteropNative")
            }

            build(":cinteropCinteropNative") {
                assertTasksUpToDate(":cinteropCinteropNative")
            }
        }
    }

    @DisplayName("KT-62800: validation fails if neither definitionFile nor packageName was specified")
    @GradleTest
    fun cinteropWithoutDefinitionFileAndPackageName(gradleVersion: GradleVersion) {
        nativeProject("cinterop-with-header", gradleVersion = gradleVersion) {
            buildGradleKts.replaceText("packageName(\"cinterop\")", "")
            buildAndFail(":cinteropCinteropNative") {
                assertOutputContains(
                    """
                    |For the Cinterop task, either the `definitionFile` or `packageName` parameter must be specified, however, neither has been provided.
                    |
                    |More info here: https://kotlinlang.org/docs/multiplatform-dsl-reference.html#cinterops 
                    """.trimMargin()
                )
            }
        }
    }

    @DisplayName("KT-62800: check that optional .def file in cinterop works well with configuration cache")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_1) // Gradle supports checking file existence with configuration cache only since 8.1 version
    @GradleTest
    fun cinteropWithOptionalDefFileAndConfigurationCache(gradleVersion: GradleVersion) {
        nativeProject(
            "cinterop-with-header",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED
            )
        ) {
            val dummyHeaderPath = projectPath.resolve("libs").resolve("include").resolve("dummy.h").toFile().absolutePath
            // first build with non-existing .def file and configuration cache enabled
            build(":assemble") {
                assertTasksExecuted(":cinteropCinteropNative")
                extractNativeTasksCommandLineArgumentsFromOutput(":cinteropCinteropNative", toolName = NativeToolKind.C_INTEROP) {
                    assertCommandLineArgumentsContainSequentially("-header", dummyHeaderPath)
                    assertCommandLineArgumentsContainSequentially("-Ilibs/include")
                    assertCommandLineArgumentsContainSequentially("-pkg", "cinterop")
                    assertCommandLineArgumentsDoNotContain("-def")
                }
            }

            // adding cinterop.def to default path
            val cinteropDefFile = projectPath.resolve("src")
                .resolve("nativeInterop")
                .resolve("cinterop")
                .createDirectories()
                .resolve("cinterop.def")
                .createFile()

            // second build with existing .def file and configuration cache enabled
            build(":assemble") {
                assertTasksExecuted(":cinteropCinteropNative")
                extractNativeTasksCommandLineArgumentsFromOutput(":cinteropCinteropNative", toolName = NativeToolKind.C_INTEROP) {
                    assertCommandLineArgumentsContainSequentially("-header", dummyHeaderPath)
                    assertCommandLineArgumentsContainSequentially("-Ilibs/include")
                    assertCommandLineArgumentsContainSequentially("-pkg", "cinterop")
                    assertCommandLineArgumentsContainSequentially("-def", cinteropDefFile.toFile().absolutePath)
                }
            }
        }
    }
}