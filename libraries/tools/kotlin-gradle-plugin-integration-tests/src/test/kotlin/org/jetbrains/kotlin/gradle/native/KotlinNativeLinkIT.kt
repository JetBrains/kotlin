/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.writeText

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

    @DisplayName("KT-81359 IncompatibleBinaryConfiguration diagnostic is not triggered for tasks configuration")
    @GradleTest
    fun testIncompatibleBinaryConfigurationDiagnostic(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxX64 {
                        binaries.staticLib()
                    }

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }

                project.tasks.withType<KotlinNativeLink>().configureEach { link ->
                    link.binary.debuggable = true
                    link.binary.optimized = true
                }
            }

            build(":linkDebugStaticLinuxX64") {
                assertHasDiagnostic(KotlinToolingDiagnostics.IncompatibleBinaryTaskConfiguration)
                assertNoDiagnostic(KotlinToolingDiagnostics.IncompatibleBinaryConfiguration)
                assertOutputContains("w: Unsupported combination of flags: -opt and -g. Please pick one.")
            }
        }
    }

    @DisplayName("Test link task is restored from the build cache")
    @GradleTest
    fun testLinkIsRestoredFromBuildCache(gradleVersion: GradleVersion, @TempDir localBuildCacheDir: Path) {
        linkTestProject(gradleVersion, localBuildCacheDir) {
            build(":linkDebugTestLinuxX64")
            build("clean")
            build(":linkDebugTestLinuxX64") {
                assertTasksFromCache(":linkDebugTestLinuxX64")
                assertFileInProjectExists("build/bin/linuxX64/debugTest/test.kexe")
            }
        }
    }

    @DisplayName("Shared library link task is restored from the build cache together with its C header")
    @GradleTest
    fun testSharedLibraryLinkIsRestoredFromBuildCache(gradleVersion: GradleVersion, @TempDir localBuildCacheDir: Path) {
        linkTestProject(gradleVersion, localBuildCacheDir, targets = { linuxX64().binaries.sharedLib() }) {
            build(":linkDebugSharedLinuxX64")
            build("clean")
            build(":linkDebugSharedLinuxX64") {
                assertTasksFromCache(":linkDebugSharedLinuxX64")
                assertFileInProjectExists("build/bin/linuxX64/debugShared/libempty.so")
                assertFileInProjectExists("build/bin/linuxX64/debugShared/libempty_api.h")
            }
        }
    }

    @DisplayName("Apple link task is restored from the build cache together with its dSYM bundle")
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    fun testAppleLinkIsRestoredFromBuildCacheWithDsym(gradleVersion: GradleVersion, @TempDir localBuildCacheDir: Path) {
        val targets: KotlinMultiplatformExtension.() -> Unit = {
            macosArm64().binaries.executable()
            sourceSets.commonMain.get().compileSource("fun main() {}")
        }
        linkTestProject(gradleVersion, localBuildCacheDir, targets = targets) {
            // Release, so the dSYM comes from light debug info rather than from -g
            build(":linkReleaseExecutableMacosArm64") {
                assertDirectoryExists(projectPath.resolve("build/bin/macosArm64/releaseExecutable/empty.kexe.dSYM"))
            }
            build("clean")
            build(":linkReleaseExecutableMacosArm64") {
                assertTasksFromCache(":linkReleaseExecutableMacosArm64")
                assertFileInProjectExists("build/bin/macosArm64/releaseExecutable/empty.kexe")
                assertDirectoryExists(projectPath.resolve("build/bin/macosArm64/releaseExecutable/empty.kexe.dSYM"))
            }
        }
    }

    @DisplayName("KT-89241: link task is restored from the build cache when another task writes into its destination directory")
    @GradleTest
    fun testLinkIsRestoredFromBuildCacheWithForeignFilesInDestinationDirectory(
        gradleVersion: GradleVersion,
        @TempDir localBuildCacheDir: Path,
    ) {
        linkTestProject(gradleVersion, localBuildCacheDir, copyResourcesIntoLinkDir = true) {
            build(":linkDebugTestLinuxX64")
            build("clean")
            build(":linkDebugTestLinuxX64") {
                assertTasksExecuted(":copyTestResourcesForLinuxX64")
                assertTasksFromCache(":linkDebugTestLinuxX64")
                // Files other tasks put next to the binary survive the restore
                assertFileInProjectExists("build/bin/linuxX64/debugTest/test.kexe")
                assertFileInProjectExists("build/bin/linuxX64/debugTest/test-resources/sample.txt")
            }
            // Nothing changed, so neither task should rerun
            build(":linkDebugTestLinuxX64") {
                assertTasksUpToDate(":copyTestResourcesForLinuxX64", ":linkDebugTestLinuxX64")
            }
        }
    }

    // With copyResourcesIntoLinkDir this is the same wiring as Compose's IosResources.kt: a Copy
    // task writes into the test executable's output directory and the link task depends on it.
    private fun linkTestProject(
        gradleVersion: GradleVersion,
        localBuildCacheDir: Path,
        copyResourcesIntoLinkDir: Boolean = false,
        targets: KotlinMultiplatformExtension.() -> Unit = { linuxX64() },
        test: TestProject.() -> Unit,
    ) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true),
        ) {
            plugins {
                kotlin("multiplatform")
            }
            enableLocalBuildCache(localBuildCacheDir)
            if (copyResourcesIntoLinkDir) {
                projectPath.resolve("resources").toFile().mkdirs()
                projectPath.resolve("resources/sample.txt").writeText("sample")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    targets()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    sourceSets.commonTest.get().compileStubSourceWithSourceSetName()

                    if (copyResourcesIntoLinkDir) {
                        linuxX64().binaries.withType(TestExecutable::class.java).configureEach { testExecutable ->
                            val copyTask = project.tasks.register("copyTestResourcesForLinuxX64", Copy::class.java) {
                                it.from(project.layout.projectDirectory.dir("resources"))
                                it.into(testExecutable.outputDirectory.resolve("test-resources"))
                            }
                            testExecutable.linkTaskProvider.configure { it.dependsOn(copyTask) }
                        }
                    }
                }
            }

            test()
        }
    }
}
