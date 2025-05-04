/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.ConfigurationCacheValue
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.assertHasDiagnostic
import org.jetbrains.kotlin.gradle.testbase.assertNoDiagnostic
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.assertTasksFailed
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.compileSource
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.publish

@MppGradlePluginTests
class CheckKotlinMultiplatformDependenciesIT : KGPBaseTest() {

    @GradleTest
    fun testCheckKotlinMultiplatformDependencies(gradleVersion: GradleVersion) {
        val published = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    jvm()
                    linuxX64()

                    sourceSets.commonMain.get().compileSource("class Common")
                }
            }
        }.publish()

        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }

            addPublishedProjectToRepositories(published)
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    jvm()
                    linuxArm64()
                    linuxX64()

                    sourceSets.commonMain {
                        dependencies {
                            api(published.rootCoordinate)
                            compileSource("fun foo() = Common()")
                        }
                    }
                }
            }

            val withConfigurationCache = buildOptions.copy(configurationCache = ConfigurationCacheValue.ENABLED)
            buildAndFail("assemble", buildOptions = withConfigurationCache) {
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.DependencyDoesNotSupportKotlinPlatform,
                    withSubstring = "Dependency ${published.rootCoordinate} is not consumable by Kotlin linuxArm64 target"
                )
            }

            val withoutConfigurationCache = buildOptions.copy(configurationCache = ConfigurationCacheValue.DISABLED)
            buildAndFail("assemble", buildOptions = withoutConfigurationCache) {
                assertTasksExecuted(":checkLinuxArm64MainKmpDependencies")
                assertTasksFailed(":compileKotlinLinuxArm64")
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.DependencyDoesNotSupportKotlinPlatform,
                    withSubstring = "Dependency ${published.rootCoordinate} is not consumable by Kotlin linuxArm64 target"
                )
            }

            build(":compileKotlinLinuxX64", buildOptions = withoutConfigurationCache) {
                assertTasksExecuted(":checkLinuxX64MainKmpDependencies")
                assertNoDiagnostic(KotlinToolingDiagnostics.DependencyDoesNotSupportKotlinPlatform)
            }
        }
    }
}