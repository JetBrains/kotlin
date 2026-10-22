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
                project.applyMultiplatform {
                    iosX64 {
                        binaries {
                            framework {
                                baseName = "libFramework"
                            }
                        }
                    }
                    iosArm64 {
                        binaries {
                            framework {
                                baseName = "libFramework"
                            }
                        }
                    }

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }

            build("assemble") {
                assertTasksExecuted(
                    ":compileKotlinIosArm64",
                    ":compileKotlinIosX64",
                    ":linkDebugFrameworkIosArm64",
                    ":linkDebugFrameworkIosX64",
                    ":linkReleaseFrameworkIosArm64",
                    ":linkReleaseFrameworkIosX64",
                )
            }
        }
    }
}
