/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@JsGradlePluginTests
class JsIrConfigurationCacheIT : KGPBaseTest() {
    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(
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
                    ":app:compileProductionExecutableKotlinJs",
                    ":app:browserProductionWebpack",
                )
            )
        }
    }

    @DisplayName("configuration cache is reused when idea.version system property is changed in browser project")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
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
                    ":app:compileProductionExecutableKotlinJs",
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
                ) + listOf(":compileProductionExecutableKotlinJs")
            )
        }
    }

    @DisplayName("configuration cache is reused when idea.version system property is changed in node project")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
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
                ) + listOf(":compileProductionExecutableKotlinJs")
                assertTasksUpToDate(*upToDateTasks.toTypedArray())
            }
        }
    }

    @DisplayName("KT-48241: configuration cache works with test dependencies")
    @GradleTest
    fun testTestDependencies(gradleVersion: GradleVersion) {
        project("kotlin-js-project-with-test-dependencies", gradleVersion) {
            assertSimpleConfigurationCacheScenarioWorks(
                "assemble", "kotlinStorePackageLock",
                buildOptions = defaultBuildOptions.copy(
                    jsOptions = defaultBuildOptions.jsOptions?.copy(
                        yarn = false
                    )
                ),
                executedTaskNames = listOf(":rootPackageJson")
            )
        }
    }

    @DisplayName("KT-48241: configuration cache works with test dependencies for yarn.lock")
    @GradleTest
    fun testTestDependenciesYarnLock(gradleVersion: GradleVersion) {
        project("kotlin-js-project-with-test-dependencies", gradleVersion) {
            assertSimpleConfigurationCacheScenarioWorks(
                "assemble", "kotlinStoreYarnLock",
                buildOptions = defaultBuildOptions.copy(
                    jsOptions = defaultBuildOptions.jsOptions?.copy(
                        yarn = true
                    )
                ),
                executedTaskNames = listOf(":rootPackageJson")
            )
        }
    }

    @DisplayName("Node.js run correctly works with configuration cache")
    @GradleTest
    fun testNodeJsRun(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            build("nodeRun", buildOptions = buildOptions) {
                assertTasksExecuted(":nodeRun")
                if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_5)) {
                    assertOutputContains(
                        "Calculating task graph as no configuration cache is available for tasks: nodeRun"
                    )
                } else {
                    assertOutputContains(
                        "Calculating task graph as no cached configuration is available for tasks: nodeRun"
                    )
                }

                assertConfigurationCacheStored()
            }

            build("clean", buildOptions = buildOptions)

            // Then run a build where tasks states are deserialized to check that they work correctly in this mode
            build("nodeRun", buildOptions = buildOptions) {
                assertTasksExecuted(":nodeRun")
                assertConfigurationCacheReused()
            }
        }
    }
}
