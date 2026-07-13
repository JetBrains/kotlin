/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.settings

import org.gradle.kotlin.dsl.withType
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.addDefaultSettingsToSettingsGradle
import org.jetbrains.kotlin.gradle.testbase.addEcosystemPluginToBuildScriptCompilationClasspath
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.settingsBuildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.source
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

@DisplayName("Kotlin ecosystem plugin")
@JvmGradlePluginTests
class EcosystemPluginIT : KGPBaseTest() {

    @DisplayName("Works in buildSrc")
    @GradleTest
    fun testWithBuildSrc(
        gradleVersion: GradleVersion
    ) {
        project("emptyKts", gradleVersion) {
            addEcosystemPluginToBuildScriptCompilationClasspath()

            settingsBuildScriptInjection {
                settings.plugins.apply("org.jetbrains.kotlin.ecosystem")
            }

            with(subProject("buildSrc")) {
                projectPath.createDirectories()
                buildGradleKts.createFile()
                settingsGradleKts.createFile()
                projectPath.addDefaultSettingsToSettingsGradle(gradleVersion)

                addEcosystemPluginToBuildScriptCompilationClasspath(buildOptions.kotlinVersion)

                settingsBuildScriptInjection {
                    settings.plugins.apply("org.jetbrains.kotlin.ecosystem")
                    settings.dependencyResolutionManagement.repositories {
                        it.mavenLocal()
                        it.mavenCentral()
                    }
                }

                plugins {
                    id("kotlin-dsl")
                }

                if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_0)) {
                    buildScriptInjection {
                        @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
                        kotlinJvm.compilerVersion.set("2.1.0")

                        // this code can be unwrapped from afterEvaluate after upgrading minimal supported Gradle version to 8.2 or newer
                        project.afterEvaluate {
                            project.tasks.withType<KotlinJvmCompile>().configureEach {
                                @Suppress("DEPRECATION_ERROR")
                                it.compilerOptions {
                                    apiVersion.set(KotlinVersion.KOTLIN_1_8)
                                    languageVersion.set(KotlinVersion.KOTLIN_1_8)
                                }
                            }
                        }
                    }
                }

                kotlinSourcesDir().source("test.kt") {
                    """
                    fun test() = Unit
                    """.trimIndent()
                }
            }


            build("help")
        }
    }
}
