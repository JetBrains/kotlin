/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName

@JsGradlePluginTests
@MustRunOnChangesInGradle
class JsIrConfigurationCacheIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @TestMetadata("instantExecutionToJs")
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

    @DisplayName("configuration cache is working for kotlin/js browser project with file-based npm dependency declared with npm DSL")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testBrowserDistributionWithNpmDslFileDependency(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            subProject("app").buildGradleKts.replaceText(
                """implementation(npm(projectDir.resolve("src/jsMain/css")))""",
                """npm(projectDir.resolve("src/jsMain/css"))""",
            )

            assertSimpleConfigurationCacheScenarioWorks(
                ":app:build",
                buildOptions = defaultBuildOptions,
                executedTaskNames = listOf(
                    ":app:jsPackageJson",
                    ":app:jsPublicPackageJson",
                    ":app:compileKotlinJs",
                    ":app:compileProductionExecutableKotlinJs",
                    ":app:jsBrowserProductionWebpack",
                )
            )
        }
    }

    @DisplayName("configuration cache is working for kotlin/js browser project with legacy file-based npm dependency")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testBrowserDistributionWithLegacyFileDependency(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {

            val upToDateTasks = listOf(
                ":app:jsPackageJson",
                ":app:jsPublicPackageJson",
                ":app:compileKotlinJs",
                ":app:compileProductionExecutableKotlinJs",
            )
            val webpackTask = ":app:jsBrowserProductionWebpack"

            assertSimpleConfigurationCacheScenarioWorks(
                ":app:build",
                buildOptions = defaultBuildOptions,
                executedTaskNames = upToDateTasks + webpackTask,
                checkUpToDateOnRebuild = false,
            )

            build(":app:build", buildOptions = defaultBuildOptions) {
                assertTasksUpToDate(*upToDateTasks.toTypedArray())
                assertTasksExecuted(webpackTask)
            }
        }
    }

    @TestMetadata("kotlin-js-browser-project")
    @DisplayName("configuration cache is reused when idea.version system property is changed in browser project with file-based npm dependency declared with npm DSL")
    @GradleTest
    fun testBrowserDistributionOnIdeaPropertyChangeWithNpmDslFileDependency(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            subProject("app").buildGradleKts.replaceText(
                """implementation(npm(projectDir.resolve("src/jsMain/css")))""",
                """npm(projectDir.resolve("src/jsMain/css"))""",
            )

            build(":app:build") {
                assertConfigurationCacheStored()
            }
            // check IdeaPropertiesEvaluator for the logic
            build(":app:build", "-Didea.version=2020.1") {
                assertConfigurationCacheReused()
                assertTasksUpToDate(
                    ":app:jsPackageJson",
                    ":app:jsPublicPackageJson",
                    ":app:compileProductionExecutableKotlinJs",
                    ":app:jsBrowserProductionWebpack",
                )
            }
        }
    }

    @TestMetadata("kotlin-js-browser-project")
    @DisplayName("configuration cache is reused when idea.version system property is changed in browser project with legacy file-based npm dependency")
    @GradleTest
    fun testBrowserDistributionOnIdeaPropertyChangeWithLegacyFileDependency(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {

            build(":app:build") {
                assertConfigurationCacheStored()
            }
            // check IdeaPropertiesEvaluator for the logic
            build(":app:build", "-Didea.version=2020.1") {
                assertConfigurationCacheReused()
                assertTasksUpToDate(
                    ":app:jsPackageJson",
                    ":app:jsPublicPackageJson",
                    ":app:compileProductionExecutableKotlinJs",
                )
                assertTasksExecuted(":app:jsBrowserProductionWebpack")
            }
        }
    }

    @TestMetadata("kotlin-js-nodejs-project")
    @DisplayName("configuration cache is working for kotlin/js node project")
    @GradleTest
    fun testNodeJs(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            assertSimpleConfigurationCacheScenarioWorks(
                ":build",
                buildOptions = defaultBuildOptions,
                executedTaskNames = listOf(
                    ":jsPackageJson",
                    ":jsPublicPackageJson",
                    ":rootPackageJson",
                    ":compileKotlinJs",
                    ":jsNodeTest",
                ) + listOf(":compileProductionExecutableKotlinJs")
            )
        }
    }

    @TestMetadata("kotlin-js-nodejs-project")
    @DisplayName("configuration cache is reused when idea.version system property is changed in node project")
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
                    ":jsPackageJson",
                    ":jsPublicPackageJson",
                    ":rootPackageJson",
                    ":compileKotlinJs",
                    ":jsNodeTest",
                ) + listOf(":compileProductionExecutableKotlinJs")
                assertTasksUpToDate(*upToDateTasks.toTypedArray())
            }
        }
    }

    @TestMetadata("kotlin-js-project-with-test-dependencies")
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

    @TestMetadata("kotlin-js-project-with-test-dependencies")
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

    @TestMetadata("kotlin-js-nodejs-project")
    @DisplayName("Node.js run correctly works with configuration cache")
    @GradleTest
    fun testNodeJsRun(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            build("jsNodeDevelopmentRun", buildOptions = buildOptions) {
                assertTasksExecuted(":jsNodeDevelopmentRun")
                assertOutputContains(
                    "Calculating task graph as no cached configuration is available for tasks: jsNodeDevelopmentRun"
                )

                assertConfigurationCacheStored()
            }

            build("clean", buildOptions = buildOptions)

            // Then run a build where tasks states are deserialized to check that they work correctly in this mode
            build("jsNodeDevelopmentRun", buildOptions = buildOptions) {
                assertTasksExecuted(":jsNodeDevelopmentRun")
                assertConfigurationCacheReused()
            }
        }
    }

    @TestMetadata("kotlin-js-build-logic")
    @DisplayName("Test with custom build logic plugin")
    @GradleTest
    fun testWithCustomBuildLogic(gradleVersion: GradleVersion) {
        project("kotlin-js-build-logic", gradleVersion) {

            settingsGradleKts
                .replaceText(
                    "pluginManagement {",
                    """

                    pluginManagement {
                        includeBuild("build-logic")

                    """.trimIndent()
                )

            assertSimpleConfigurationCacheScenarioWorks(
                ":rootPackageJson",
                buildOptions = defaultBuildOptions,
            )
        }
    }
}
