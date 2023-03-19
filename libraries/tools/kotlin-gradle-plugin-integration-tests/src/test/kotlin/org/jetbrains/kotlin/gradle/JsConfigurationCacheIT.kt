/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

abstract class AbstractJsConfigurationCacheIT(protected val irBackend: Boolean) : KGPBaseTest() {
    @Suppress("DEPRECATION")
    private val defaultJsOptions = BuildOptions.JsOptions(
        useIrBackend = irBackend,
        jsCompilerType = if (irBackend) KotlinJsCompilerType.IR else KotlinJsCompilerType.LEGACY,
    )

    final override val defaultBuildOptions =
        super.defaultBuildOptions.copy(
            jsOptions = defaultJsOptions,
            configurationCache = true,
            configurationCacheProblems = BaseGradleIT.ConfigurationCacheProblems.FAIL
        )

    @DisplayName("configuration cache is working for kotlin2js plugin")
    @GradleTest
    fun testKotlin2JsCompilation(gradleVersion: GradleVersion) {
        project("instantExecutionToJs", gradleVersion) {
            assertSimpleConfigurationCacheScenarioWorks(
                "assemble",
                buildOptions = defaultBuildOptions,
                executedTaskNames = listOf(":compileKotlinJs")
            )
        }
    }

    @DisplayName("configuration cache is working for kotlin/js browser project")
    @GradleTest
    fun testBrowserDistribution(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            assertSimpleConfigurationCacheScenarioWorks(
                ":app:build",
                buildOptions = defaultBuildOptions,
                executedTaskNames = listOf(
                    ":app:packageJson",
                    ":app:publicPackageJson",
                    ":app:compileKotlinJs",
                    if (irBackend) ":app:compileProductionExecutableKotlinJs" else ":app:processDceKotlinJs",
                    ":app:browserProductionWebpack",
                )
            )
        }
    }

    @DisplayName("configuration cache is reused when idea.version system property is changed in browser project")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_5)
    @GradleTest
    fun testBrowserDistributionOnIdeaPropertyChange(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            build(":app:build") {
                assertConfigurationCacheStored()
            }
            // check IdeaPropertiesEvaluator for the logic
            build(":app:build", "-Didea.version=2020.1") {
                assertConfigurationCacheReused()
                assertTasksUpToDate(
                    ":app:packageJson",
                    ":app:publicPackageJson",
                    if (irBackend) ":app:compileProductionExecutableKotlinJs" else ":app:processDceKotlinJs",
                    ":app:browserProductionWebpack",
                )
            }
        }
    }

    @DisplayName("configuration cache is working for kotlin/js node project")
    @GradleTest
    fun testNodeJs(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            assertSimpleConfigurationCacheScenarioWorks(
                ":build",
                buildOptions = defaultBuildOptions,
                executedTaskNames = listOf(
                    ":packageJson",
                    ":publicPackageJson",
                    ":rootPackageJson",
                    ":compileKotlinJs",
                    ":nodeTest",
                ) + if (irBackend) listOf(":compileProductionExecutableKotlinJs") else emptyList()
            )
        }
    }

    @DisplayName("configuration cache is reused when idea.version system property is changed in node project")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_5, maxVersion = TestVersions.Gradle.G_7_5)
    @GradleTest
    fun testNodeJsOnIdeaPropertyChange(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            build(":build") {
                assertConfigurationCacheStored()
            }
            // check IdeaPropertiesEvaluator for the logic
            build(":build", "-Didea.version=2020.1") {
                assertConfigurationCacheReused()
                val upToDateTasks = listOf(
                    ":packageJson",
                    ":publicPackageJson",
                    ":rootPackageJson",
                    ":compileKotlinJs",
                    ":nodeTest",
                ) + if (irBackend) listOf(":compileProductionExecutableKotlinJs") else emptyList()
                assertTasksUpToDate(*upToDateTasks.toTypedArray())
            }
        }
    }

    @DisplayName("KT-48241: configuration cache works with test dependencies")
    @GradleTest
    fun testTestDependencies(gradleVersion: GradleVersion) {
        project("kotlin-js-project-with-test-dependencies", gradleVersion) {
            assertSimpleConfigurationCacheScenarioWorks(
                "assemble", "kotlinStoreYarnLock",
                buildOptions = defaultBuildOptions,
                executedTaskNames = listOf(":rootPackageJson")
            )
        }
    }
}

@JsGradlePluginTests
class JsConfigurationCacheIT : AbstractJsConfigurationCacheIT(irBackend = false)

@JsGradlePluginTests
class JsIrConfigurationCacheIT : AbstractJsConfigurationCacheIT(irBackend = true)
