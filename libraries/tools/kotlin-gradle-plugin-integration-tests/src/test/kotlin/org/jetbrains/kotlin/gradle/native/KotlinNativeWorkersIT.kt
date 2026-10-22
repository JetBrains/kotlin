/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.ConfigurationCacheValue
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.IsolatedProjectsMode
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.compileStubSourceWithSourceSetName
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@DisplayName("Smoke tests for Kotlin Native tasks that use Workers API")
@NativeGradlePluginTests
class KotlinNativeWorkersIT : KGPBaseTest() {

    @DisplayName("Smoke test for KT-90576 Migrate select Native tasks to Workers API")
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    fun testRunsNativeTasksWithConfigurationCachingDisabled(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(
            configurationCache = ConfigurationCacheValue.DISABLED,
            isolatedProjects = IsolatedProjectsMode.DISABLED,
        )
        project("empty", gradleVersion, buildOptions) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                // Same cinterop name is attached to more than one native target below, which is exactly
                // what makes CInteropCommonizerTask (registered as ':commonizeCInterop') have actual
                // work to do, instead of ending up with an empty commonizer group.
                val defFile = project.layout.projectDirectory.dir("src/nativeInterop/cinterop").file("myInterop.def").asFile
                defFile.parentFile.mkdirs()
                defFile.writeText(
                    """
                    language = Objective-C
                    """.trimIndent()
                )

                project.applyMultiplatform {
                    iosX64 {
                        binaries {
                            framework {
                                baseName = "libFramework"
                            }
                        }
                        compilations.getByName("main").cinterops.create("myInterop") {
                            it.definitionFile.set(defFile)
                        }
                    }
                    iosArm64 {
                        binaries {
                            framework {
                                baseName = "libFramework"
                            }
                        }
                        compilations.getByName("main").cinterops.create("myInterop") {
                            it.definitionFile.set(defFile)
                        }
                    }

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }

            build(
                "assemble",
                // CInteropCommonizerTask is only registered when c-interop commonization is enabled.
                "-Pkotlin.mpp.enableCInteropCommonization=true",
            ) {
                assertTasksExecuted(
                    ":compileKotlinIosArm64",
                    ":compileKotlinIosX64",
                    ":linkDebugFrameworkIosArm64",
                    ":linkDebugFrameworkIosX64",
                    ":linkReleaseFrameworkIosArm64",
                    ":linkReleaseFrameworkIosX64",
                    ":commonizeCInterop",
                )
            }
        }
    }
}
