/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.test.assertEquals

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

    @DisplayName("KT-89558: cinterop checks content of embedded static libraries for up-to-date checks")
    @GradleTest
    fun cinteropStaticLibrariesUTDChecks(gradleVersion: GradleVersion) {
        nativeProject("KT-89558-cinterop-static-libraries-UTD-checks", gradleVersion = gradleVersion) {
            val withProjectDir = ":cinteropCinteropNative"
            val withoutProjectDir = ":cinteropNoProjectDirNative"
            build(withProjectDir, withoutProjectDir) {
                assertTasksExecuted(withProjectDir, withoutProjectDir)
            }
            build(withProjectDir, withoutProjectDir) {
                assertConfigurationCacheReused()
                assertTasksUpToDate(withProjectDir, withoutProjectDir)
            }

            // staticLibraries + libraryPaths from the .def file, and a comma-separated -staticLibrary from extraOpts
            for (library in listOf("libs/libA.a", "libs/libB.a")) {
                projectPath.resolve(library).toFile().appendText("v2\n")
                build(withProjectDir, withoutProjectDir) {
                    assertConfigurationCacheReused()
                    assertTasksExecuted(withProjectDir)
                    assertTasksUpToDate(withoutProjectDir)
                }
            }
            // Relative library paths without -Xproject-dir
            projectPath.resolve("libs/libC.a").toFile().appendText("v2\n")
            build(withProjectDir, withoutProjectDir) {
                assertConfigurationCacheReused()
                assertTasksUpToDate(withProjectDir)
                assertTasksExecuted(withoutProjectDir)
            }
            projectPath.resolve("defLibs/libFromDefFile.a").toFile().appendText("v2\n")
            build(withProjectDir, withoutProjectDir) {
                assertConfigurationCacheReused()
                assertTasksExecuted(withProjectDir, withoutProjectDir)
            }

            // A library added to the .def file is tracked as well
            projectPath.resolve("src/nativeInterop/cinterop/cinterop.def").replaceText(
                "staticLibraries = libFromDefFile.a",
                "staticLibraries = libFromDefFile.a libFromDefFile2.a",
            )
            build(withProjectDir) {
                assertTasksExecuted(withProjectDir)
            }
            projectPath.resolve("defLibs/libFromDefFile2.a").toFile().appendText("v2\n")
            build(withProjectDir) {
                assertTasksExecuted(withProjectDir)
            }

            build(withProjectDir, withoutProjectDir) {
                assertTasksUpToDate(withProjectDir, withoutProjectDir)
            }
        }
    }

    @DisplayName("KT-89558: link task re-runs when the static library passed in linkerOpts is rebuilt")
    @OsCondition(supportedOn = [OS.LINUX, OS.MAC], enabledOnCI = [OS.LINUX]) // disabled for Windows because it requires Unix cc and ar
    @GradleTest
    fun cinteropStaticLibraryUTDChecks(gradleVersion: GradleVersion) {
        nativeProject("KT-89558-cinterop-static-library-UTD-checks", gradleVersion = gradleVersion) {
            buildStaticLibrary(projectPath)

            build(":runDebugExecutableNative") {
                assertGlobalCounterValue(5)
            }
            build(":runDebugExecutableNative") {
                assertTasksUpToDate(":linkDebugExecutableNative")
            }

            projectPath.resolve("lib.c").replaceText("return globalCounter;", "return globalCounter + 1;")
            buildStaticLibrary(projectPath)

            build(":runDebugExecutableNative") {
                assertTasksExecuted(":linkDebugExecutableNative")
                assertGlobalCounterValue(6)
            }
        }
    }

    @DisplayName("KT-89558: track a static library created after link task initialization")
    @OsCondition(supportedOn = [OS.LINUX, OS.MAC], enabledOnCI = [OS.LINUX]) // requires Unix cc and ar
    @GradleTest
    fun cinteropStaticLibraryCreatedAfterLinkTaskInitialization(gradleVersion: GradleVersion) {
        nativeProject("KT-89558-cinterop-static-library-UTD-checks", gradleVersion = gradleVersion) {
            buildGradleKts.appendText(
                """

                // Realize the link task while lib.a does not exist yet.
                val linkTask = tasks.named("linkDebugExecutableNative").get()
                val compileStaticLibrary = tasks.register<Exec>("compileStaticLibrary") {
                    inputs.files("lib.c", "include/lib.h")
                    outputs.file("lib.o")
                    commandLine("cc", "-c", "lib.c", "-Iinclude", "-o", "lib.o")
                }
                val archiveStaticLibrary = tasks.register<Exec>("archiveStaticLibrary") {
                    dependsOn(compileStaticLibrary)
                    inputs.file("lib.o")
                    outputs.file("lib.a")
                    commandLine("ar", "rcs", "lib.a", "lib.o")
                }
                linkTask.dependsOn(archiveStaticLibrary)
                tasks.register("checkLinkerInputs") {
                    dependsOn(linkTask)
                    inputs.files(linkTask.inputs.files)
                    doLast {
                        check(inputs.files.files.any { it.name == "lib.a" }) {
                            "The generated static library must be tracked as a link task input"
                        }
                    }
                }
                """.trimIndent()
            )

            build(":checkLinkerInputs", ":runDebugExecutableNative") {
                assertTasksExecuted(":archiveStaticLibrary", ":linkDebugExecutableNative")
                assertGlobalCounterValue(5)
            }
            // Allow unrelated first incremental build setup changes to settle.
            build(":checkLinkerInputs", ":runDebugExecutableNative")
            build(":checkLinkerInputs", ":runDebugExecutableNative") {
                assertConfigurationCacheReused()
                assertTasksUpToDate(":archiveStaticLibrary", ":linkDebugExecutableNative")
            }

            projectPath.resolve("lib.c").replaceText("return globalCounter;", "return globalCounter + 1;")
            build(":checkLinkerInputs", ":runDebugExecutableNative") {
                assertConfigurationCacheReused()
                assertTasksExecuted(":compileStaticLibrary", ":archiveStaticLibrary", ":linkDebugExecutableNative")
                assertGlobalCounterValue(6)
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
                    |Please specify either the `definitionFile` or `packageName` parameter for the Cinterop task.
                    |More info here: https://kotlinlang.org/docs/multiplatform-dsl-reference.html#cinterops
                    """.trimMargin()
                )
            }
        }
    }

    @DisplayName("KT-62800: check that optional .def file in cinterop works well with configuration cache")
    @GradleTest
    fun cinteropWithOptionalDefFileAndConfigurationCache(gradleVersion: GradleVersion) {
        nativeProject("cinterop-with-header", gradleVersion = gradleVersion) {
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

private fun BuildResult.assertGlobalCounterValue(expected: Int) {
    val actual = output.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("KT89558_RESULT=") }
        .toList()
    assertEquals(listOf("KT89558_RESULT=$expected"), actual)
}

private fun buildStaticLibrary(projectPath: Path) {
    val workingDir = projectPath.toFile()
    runProcess(listOf("cc", "-c", "lib.c", "-Iinclude", "-o", "lib.o"), workingDir).also {
        check(it.isSuccessful) { "Compiling lib.c failed: $it" }
    }
    projectPath.resolve("lib.a").deleteIfExists()
    runProcess(listOf("ar", "rcs", "lib.a", "lib.o"), workingDir).also {
        check(it.isSuccessful) { "Archiving lib.a failed: $it" }
    }
}
