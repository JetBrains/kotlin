/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.uklibs.setupMavenPublication
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@MppGradlePluginTests
class MppCrossCompilationPublicationIT : KGPBaseTest() {

    @DisplayName("Cross compilation enabled, no cinterops, publish library to mavenLocal")
    @GradleTest
    fun testCrossCompilationPublicationWithoutCinterops(
        gradleVersion: GradleVersion,
    ) {
        val multiplatformLibrary = publishMultiplatformLibrary(gradleVersion) {
            assertNoDiagnostic(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
        }

        val mavenUrl = multiplatformLibrary.projectPath.resolve("build/repo")
        val libraryRoot = mavenUrl.resolve("com/jetbrains/library")

        // Verify that module directories exist for each target
        val expectedModules = setOf(
            "multiplatformLibrary",
            "multiplatformLibrary-iosarm64",
            "multiplatformLibrary-jvm",
            "multiplatformLibrary-linuxx64",
            "multiplatformLibrary-macosarm64",
            "multiplatformLibrary-mingwx64"
        )

        // Check that expected modules exist
        val actualModules = getActualModules(libraryRoot)
        assertEquals(expectedModules, actualModules, "Published modules don't match expected set")

        // Verify module contents
        verifyModuleContents(libraryRoot, expectedModules)
    }

    @DisplayName("Cross compilation disabled, no cinterops, publish library to mavenLocal")
    @GradleTest
    @OsCondition(supportedOn = [OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.WINDOWS])
    fun testDisabledCrossCompilationPublicationWithoutCinterops(
        gradleVersion: GradleVersion,
    ) {
        val multiplatformLibrary = publishMultiplatformLibrary(
            gradleVersion,
            defaultBuildOptions.disableKlibsCrossCompilation()
        ) {
            assertNoDiagnostic(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
        }

        val mavenUrl = multiplatformLibrary.projectPath.resolve("build/repo")
        val libraryRoot = mavenUrl.resolve("com/jetbrains/library")

        // Verify that only the common and JVM module directories exist
        val expectedModules = setOf(
            "multiplatformLibrary",
            "multiplatformLibrary-jvm",
            "multiplatformLibrary-linuxx64",
            "multiplatformLibrary-mingwx64"
        )

        // Check exact match of published modules
        val actualModules = getActualModules(libraryRoot)
        assertEquals(expectedModules, actualModules, "Published modules don't match expected set when cross-compilation is disabled")

        // Verify module contents
        verifyModuleContents(libraryRoot, expectedModules)
    }

    @DisplayName("Cross compilation enabled, with Linux, Windows and macOS cinterops, publish library to mavenLocal")
    @GradleTest
    fun testCrossCompilationPublicationWithCinterops(
        gradleVersion: GradleVersion,
    ) {
        val multiplatformLibrary = publishMultiplatformLibrary(
            gradleVersion,
            projectSetup = {
                embedDirectoryFromTestData("cinterop-lib/cinterop", "src/nativeInterop/cinterop")
                embedDirectoryFromTestData("cinterop-lib/src", "include")
                embedDirectoryFromTestData("cinterop-lib/libs", "libs")
            },
            kmpSetup = {
                macosArm64()
                mingwX64()
                linuxX64()

                setupCInteropForTarget("mylib", KonanTarget.MACOS_ARM64, "mylib_macos.def")
                setupCInteropForTarget("mylib", KonanTarget.LINUX_X64, "mylib_linux.def")
                setupCInteropForTarget("mylib", KonanTarget.MINGW_X64, "mylib_windows.def")
            },
            assertions = {
                if (HostManager.hostIsMac) {
                    assertNoDiagnostic(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
                } else {
                    assertHasDiagnostic(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
                }
            }
        )

        val mavenUrl = multiplatformLibrary.projectPath.resolve("build/repo")
        val libraryRoot = mavenUrl.resolve("com/jetbrains/library")

        val expectedModules = buildSet {
            add("multiplatformLibrary")
            add("multiplatformLibrary-linuxx64")
            add("multiplatformLibrary-mingwx64")
            if (HostManager.hostIsMac) {
                add("multiplatformLibrary-macosarm64")
            }
        }

        // Check exact match of published modules
        val actualModules = getActualModules(libraryRoot)
        assertEquals(expectedModules, actualModules, "Published modules don't match expected set with cinterops")

        // Verify module contents
        verifyModuleContents(libraryRoot, expectedModules)

        // Verify cinterop artifacts existence with equality checks
        val cinteropTargets = mutableListOf(
            "linuxx64",
            "mingwx64"
        )
        if (HostManager.hostIsMac) {
            cinteropTargets.add("macosarm64")
        }

        val expectedCinteropFiles = cinteropTargets.map { target ->
            "multiplatformLibrary-$target-1.0-cinterop-mylib.klib"
        }.toSet()

        val actualCinteropFiles = cinteropTargets.mapNotNull { target ->
            val cinteropKlib = libraryRoot
                .resolve("multiplatformLibrary-$target")
                .resolve("1.0")
                .resolve("multiplatformLibrary-$target-1.0-cinterop-mylib.klib")

            if (cinteropKlib.exists()) {
                "multiplatformLibrary-$target-1.0-cinterop-mylib.klib"
            } else {
                null
            }
        }.toSet()

        assertEquals(expectedCinteropFiles, actualCinteropFiles, "Cinterop klib files don't match expected set")
    }

    @DisplayName("Check FUS event for cross compilation enabled/disabled")
    @GradleTest
    fun testCrossCompilationDisabledFusEvent(
        gradleVersion: GradleVersion,
    ) {
        project(
            "empty",
            gradleVersion,
        ) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxX64()
                    mingwX64()
                    macosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }

            val eventPrefix = "${BooleanMetrics.KOTLIN_CROSS_COMPILATION_DISABLED.name}="
            var events =
                collectFusEvents(
                    ":compileKotlinLinuxX64",
                    ":compileKotlinMingwX64",
                    ":compileKotlinMacosArm64",
                ).count {
                    it.startsWith(eventPrefix)
                }

            // Cross compilation enabled by default, so we expect 0 events
            assertEquals(
                0,
                events
            )

            events =
                collectFusEvents(
                    ":compileKotlinLinuxX64",
                    ":compileKotlinMingwX64",
                    ":compileKotlinMacosArm64",
                    "-Pkotlin.native.enableKlibsCrossCompilation=false"
                ).count {
                    it.startsWith(eventPrefix)
                }

            // Cross compilation disabled by user, so we expect 1 event
            assertEquals(
                1,
                events
            )
        }
    }

    @DisplayName("Check FUS event for cross compilation not supported")
    @GradleTest
    fun testCrossCompilationNotSupportedFusEvent(
        gradleVersion: GradleVersion,
    ) {
        project(
            "empty",
            gradleVersion,
        ) {
            embedDirectoryFromTestData("cinterop-lib/cinterop", "src/nativeInterop/cinterop")
            embedDirectoryFromTestData("cinterop-lib/src", "include")
            embedDirectoryFromTestData("cinterop-lib/libs", "libs")

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxX64()
                    mingwX64()
                    macosArm64()
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()

                    setupCInteropForTarget("mylib", KonanTarget.IOS_ARM64, "mylib_ios_arm64.def")
                    setupCInteropForTarget("mylib", KonanTarget.MACOS_ARM64, "mylib_macos.def")
                    setupCInteropForTarget("mylib", KonanTarget.LINUX_X64, "mylib_linux.def")
                    setupCInteropForTarget("mylib", KonanTarget.MINGW_X64, "mylib_windows.def")
                }
            }

            val eventPrefix = "${BooleanMetrics.KOTLIN_CROSS_COMPILATION_NOT_SUPPORTED.name}="
            val notSupportedEventEvents =
                collectFusEvents(
                    ":compileKotlinLinuxX64",
                    ":compileKotlinMingwX64",
                    ":compileKotlinMacosArm64",
                    ":compileKotlinIosArm64"
                ).count {
                    it.startsWith(eventPrefix)
                }

            if (HostManager.hostIsMac) {
                assertEquals(
                    0,
                    notSupportedEventEvents
                )
            } else {
                assertEquals(
                    1,
                    notSupportedEventEvents
                )
            }
        }
    }

    @DisplayName("Cross compilation enabled, with Linux, Windows and macOS cinterops in nested project, publish library to mavenLocal")
    @GradleTest
    fun testCrossCompilationPublicationWithNestedCinterops(
        gradleVersion: GradleVersion,
    ) {
        val multiplatformLibrary = publishMultiplatformLibrary(
            gradleVersion,
            test = {
                val subprojectWithCinterops = project("empty", gradleVersion) {
                    embedDirectoryFromTestData("cinterop-lib/cinterop", "src/nativeInterop/cinterop")
                    embedDirectoryFromTestData("cinterop-lib/src", "include")
                    embedDirectoryFromTestData("cinterop-lib/libs", "libs")

                    buildScriptInjection {
                        project.applyMultiplatform {
                            macosArm64()
                            mingwX64()
                            linuxX64()

                            setupCInteropForTarget("mylib", KonanTarget.MACOS_ARM64, "mylib_macos.def")
                            setupCInteropForTarget("mylib", KonanTarget.LINUX_X64, "mylib_linux.def")
                            setupCInteropForTarget("mylib", KonanTarget.MINGW_X64, "mylib_windows.def")

                            sourceSets.commonMain.get().compileSource(
                                """
                                package org.foo
                                class One
                                """.trimIndent()
                            )

                            sourceSets.macosMain.get().compileSource(
                                """
                                package org.foo
                                import kotlinx.cinterop.*
                                import mylib_macos.hello
                                class OneMac
                                """.trimIndent()
                            )
                        }
                    }
                }

                include(subprojectWithCinterops, "cinteropdependency")
            },
            kmpSetup = {
                macosArm64()
                mingwX64()
                linuxX64()

                sourceSets.commonMain {
                    compileSource(
                        """
                            import org.foo.One
                            fun foo(): One = One()
                            """.trimIndent()
                    )

                    dependencies {
                        implementation(project(":cinteropdependency"))
                    }
                }
            },

            assertions = {
                if (HostManager.hostIsMac) {
                    assertNoDiagnostic(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
                } else {
                    assertHasDiagnostic(KotlinToolingDiagnostics.CrossCompilationWithCinterops)
                }
            }
        )

        val mavenUrl = multiplatformLibrary.projectPath.resolve("build/repo")
        val libraryRoot = mavenUrl.resolve("com/jetbrains/library")

        val expectedModules = buildSet {
            add("multiplatformLibrary")
            add("multiplatformLibrary-linuxx64")
            add("multiplatformLibrary-mingwx64")
            if (HostManager.hostIsMac) {
                add("multiplatformLibrary-macosarm64")
            }
        }

        // Check exact match of published modules
        val actualModules = getActualModules(libraryRoot)
        assertEquals(expectedModules, actualModules, "Published modules don't match expected set with cinterops")

        // Verify module contents
        verifyModuleContents(libraryRoot, expectedModules)
    }
}

private fun KGPBaseTest.publishMultiplatformLibrary(
    gradleVersion: GradleVersion,
    buildOptions: BuildOptions = defaultBuildOptions,
    projectSetup: TestProject.() -> Unit = {},
    kmpSetup: KotlinMultiplatformExtension.() -> Unit = {
        jvm()
        iosArm64()
        macosArm64()
        mingwX64()
        linuxX64()
    },
    builder: (TestProject, String, BuildResult.() -> Unit) -> Unit = { project, taskName, assertions ->
        project.build(taskName, assertions = assertions)
    },
    assertions: BuildResult.() -> Unit = {},
) = project(
    "empty",
    gradleVersion,
    buildOptions = buildOptions
) {
    projectSetup()
    plugins {
        kotlin("multiplatform")
    }
    settingsBuildScriptInjection {
        settings.rootProject.name = "multiplatformLibrary"
    }
    buildScriptInjection {
        project.applyMultiplatform {
            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
            kmpSetup()
        }

        project.setupMavenPublication(
            "CrossTest",
            PublisherConfiguration(
                "com.jetbrains.library",
                "1.0",
                "build/repo"
            )
        )
    }

    builder(this, "publishAllPublicationsToCrossTestRepository", assertions)
}

private fun KotlinMultiplatformExtension.setupCInteropForTarget(
    name: String,
    konanTarget: KonanTarget,
    defFileName: String,
) {
    targets.withType(KotlinNativeTarget::class.java)
        .matching { it.konanTarget == konanTarget }
        .configureEach { target ->
            target.compilations
                .getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                .cinterops
                .create(name) { interop ->
                    interop.defFile(project.file("src/nativeInterop/cinterop/$defFileName"))
                    interop.includeDirs(project.file("include"))
                }
        }
}

private fun getActualModules(libraryRoot: Path): Set<String> {
    return if (libraryRoot.exists()) {
        libraryRoot.listDirectoryEntries()
            .filter { it.toFile().isDirectory }
            .map { it.name }
            .toSet()
    } else {
        emptySet()
    }
}

private fun verifyModuleContents(libraryRoot: Path, expectedModules: Set<String>) {
    for (module in expectedModules) {
        val moduleDir = libraryRoot.resolve(module)
        assertTrue(moduleDir.exists(), "Module directory should exist: $module")

        // Verify version directory exists
        val versionDir = moduleDir.resolve("1.0")
        assertTrue(versionDir.exists(), "Version directory should exist for $module")

        // Get actual files in version directory
        val actualFiles = if (versionDir.exists()) {
            versionDir.listDirectoryEntries().map { it.name }.toSet()
        } else {
            emptySet()
        }

        // Define expected files for each module type
        val expectedFiles = mutableSetOf<String>()

        // All modules should have these files
        expectedFiles.add("$module-1.0.module")
        expectedFiles.add("$module-1.0.pom")

        // Native targets should have KLIB files
        if (module != "multiplatformLibrary" && module != "multiplatformLibrary-jvm") {
            expectedFiles.add("$module-1.0.klib")
        }

        // JVM target should have JAR file
        if (module == "multiplatformLibrary-jvm") {
            expectedFiles.add("$module-1.0.jar")
        }

        // Check that all expected files exist (subset check)
        val missingFiles = expectedFiles - actualFiles
        assertTrue(missingFiles.isEmpty(), "Missing expected files for $module: $missingFiles. Actual files: $actualFiles")
    }
}