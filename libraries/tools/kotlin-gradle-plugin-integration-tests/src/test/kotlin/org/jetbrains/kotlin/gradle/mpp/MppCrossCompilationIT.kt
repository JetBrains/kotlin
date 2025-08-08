/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.assertTasksSkipped
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.compileSource
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.util.setupCInteropForTarget
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.io.File

@MppGradlePluginTests
class MppCrossCompilationIT : KGPBaseTest() {

    @DisplayName("Cross compilation enabled with dependecy, dependency with cinterops")
    @GradleTest
    @GradleTestVersions(additionalVersions = [TestVersions.Gradle.G_8_14])
    @OsCondition(supportedOn = [OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.WINDOWS])
    fun testCrossCompilationWithCinterops(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                with(project) {
                    applyMultiplatform {
                        macosArm64()
                        @Suppress("DEPRECATION")
                        macosX64()
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
                    }
                }
            }

            include(subprojectWithCinterops(gradleVersion), "cinteropdependency")

            build(":build", "-Pkotlin.mpp.enableCInteropCommonization=true") {
                assertTasksExecuted(
                    ":cinteropdependency:compileKotlinLinuxX64",
                    ":cinteropdependency:compileKotlinMingwX64",
                )

                assertTasksExecuted(
                    ":cinteropdependency:exportCrossCompilationMetadataForLinuxX64ApiElements",
                    ":cinteropdependency:exportCrossCompilationMetadataForMacosArm64ApiElements",
                    ":cinteropdependency:exportCrossCompilationMetadataForMacosX64ApiElements",
                    ":cinteropdependency:exportCrossCompilationMetadataForMingwX64ApiElements"
                )

                assertTasksExecuted(
                    ":compileKotlinLinuxX64",
                    ":compileKotlinMingwX64",
                )

                assertTasksSkipped(
                    ":cinteropdependency:cinteropMylibMacosArm64",
                    ":cinteropdependency:cinteropMylibMacosX64",
                    ":compileKotlinMacosArm64",
                    ":compileKotlinMacosX64",
                )
            }
        }
    }

    private fun subprojectWithCinterops(gradleVersion: GradleVersion): TestProject = project("empty", gradleVersion) {
        embedDirectoryFromTestData("cinterop-lib/cinterop", "src/nativeInterop/cinterop")
        embedDirectoryFromTestData("cinterop-lib/src", "include")
        embedDirectoryFromTestData("cinterop-lib/libs", "libs")

        plugins {
            kotlin("multiplatform")
        }
        buildScriptInjection {
            with(project) {
                val defFile: (String) -> File = { file("src/nativeInterop/cinterop/$it") }
                applyMultiplatform {
                    macosArm64()
                    @Suppress("DEPRECATION")
                    macosX64()
                    mingwX64()
                    linuxX64()

                    setupCInteropForTarget("mylib", KonanTarget.MACOS_ARM64, defFile("mylib_macos.def"))
                    setupCInteropForTarget("mylib", KonanTarget.MACOS_X64, defFile("mylib_macos.def"))
                    setupCInteropForTarget("mylib", KonanTarget.LINUX_X64, defFile("mylib_linux.def"))
                    setupCInteropForTarget("mylib", KonanTarget.MINGW_X64, defFile("mylib_windows.def"))

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

                    sourceSets.linuxMain.get().compileSource(
                        """
                                package org.foo
                                import kotlinx.cinterop.*
                                import mylib_linux.hello
                                class OneLinux
                                """.trimIndent()
                    )

                    sourceSets.mingwMain.get().compileSource(
                        """
                                package org.foo
                                import kotlinx.cinterop.*
                                import mylib_windows.hello
                                class OneWin
                                """.trimIndent()
                    )
                }
            }
        }
    }
}