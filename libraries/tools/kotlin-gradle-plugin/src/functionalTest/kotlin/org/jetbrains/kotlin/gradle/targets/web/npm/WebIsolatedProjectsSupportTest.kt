/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.npm.BaseNpmExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmCachesSetup
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNpmTooling
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.web.nodejs.CommonNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootExtension
import org.jetbrains.kotlin.gradle.targets.web.yarn.CommonYarnPlugin
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals

/**
 * Test the `kotlin.internal.web.isolatedProjectsSupport` property, see [org.jetbrains.kotlin.gradle.targets.web.WebIsolatedProjectsSupport].
 *
 * Only types marked with [ObsoleteNpmDependencyModel] are checked.
 */
@Suppress("UsagesOfObsoleteApi")
class WebIsolatedProjectsSupportTest {

    @TempDir(cleanup = CleanupMode.NEVER)
    // FIXME Remove `cleanup = CleanupMode.NEVER` when https://github.com/gradle/gradle/issues/39799 is fixed.
    lateinit var workingDir: Path

    @TestFactory
    fun `when isolatedProjectsSupport is Off or property is not set`(): List<DynamicContainer> {
        return listOf(
            "Off",
            null,
        ).map { value ->
            val project = setupProject(isolatedProjectsSupport = value)

            dynamicContainer(
                "when isolatedProjectsSupport is $value",
                listOf(
                    dynamicTest("expect obsolete plugins are applied") {
                        assertEquals(
                            listOf(
                                "NodeJsRootPlugin",
                                "NpmResolverPlugin",
                                "WasmNodeJsRootPlugin",
                                "WasmNpmResolverPlugin",
                                "WasmYarnPlugin",
                                "YarnPlugin",
                            ),
                            project.obsoletePlugins(),
                        )
                    },
                    dynamicTest("expect obsolete extensions are registered") {
                        assertEquals(
                            listOf(
                                "kotlinNodeJs",
                                "kotlinNpm",
                                "kotlinWasmNodeJs",
                                "kotlinWasmNpm",
                                "kotlinWasmYarn",
                                "kotlinWasmYarnSpec",
                                "kotlinYarn",
                                "kotlinYarnSpec",
                                "wasmNpmTooling",
                            ),
                            project.obsoleteExtensions(),
                        )
                    },
                    dynamicTest("expect obsolete tasks are registered") {
                        assertEquals(
                            listOf(
                                "jsPackageJson",
                                "jsTestPackageJson",
                                "kotlinKotlinNpmCachesSetup",
                                "kotlinWasmKotlinNpmCachesSetup",
                                "wasmJsPackageJson",
                                "wasmJsTestPackageJson",
                            ),
                            project.obsoleteTasks(),
                        )
                    },
                )
            )
        }
    }

    @TestFactory
    fun `when isolatedProjectsSupport is Preview`(): List<DynamicTest> {
        val project = setupProject(isolatedProjectsSupport = "Preview")
        return listOf(
            dynamicTest("expect no obsolete plugins are applied") {
                assertEquals(emptyList(), project.obsoletePlugins())
            },
            dynamicTest("expect no obsolete extensions are registered") {
                assertEquals(emptyList(), project.obsoleteExtensions())
            },
            dynamicTest("expect no obsolete tasks are registered") {
                assertEquals(emptyList(), project.obsoleteTasks())
            },
            dynamicTest("expect test tasks can be realized") {
                val testTasks = project.tasks.withType(KotlinJsTest::class.java).toList()
                assertEquals(
                    listOf(
                        "jsBrowserTest",
                        "jsNodeTest",
                        "wasmJsBrowserTest",
                        "wasmJsD8Test",
                        "wasmJsNodeTest",
                        "wasmWasiNodeTest",
                        "wasmWasiWasmtimeTest",
                    ),
                    testTasks.map { it.name }.sorted(),
                )
            },
            dynamicTest("expect library distribution tasks can be realized") {
                val distributionTasks = project.tasks.withType(Copy::class.java)
                    .matching { it.name.endsWith("Distribution") }
                    .toList()
                assertEquals(
                    listOf(
                        "jsBrowserDevelopmentLibraryDistribution",
                        "jsBrowserProductionLibraryDistribution",
                        "jsNodeDevelopmentLibraryDistribution",
                        "jsNodeProductionLibraryDistribution",
                        "wasmJsBrowserDevelopmentLibraryDistribution",
                        "wasmJsBrowserProductionLibraryDistribution",
                        "wasmJsD8DevelopmentLibraryDistribution",
                        "wasmJsD8ProductionLibraryDistribution",
                        "wasmJsNodeDevelopmentLibraryDistribution",
                        "wasmJsNodeProductionLibraryDistribution",
                    ),
                    distributionTasks.map { it.name }.sorted(),
                )
            },
        )
    }

    /**
     * Create a KMP project with every JS and Wasm target and environment that uses npm dependency management.
     */
    private fun setupProject(isolatedProjectsSupport: String?): Project {
        // Each case gets its own directory, so cases don't share gradle.properties.
        val projectDir = workingDir.resolve(isolatedProjectsSupport ?: "unset")

        projectDir.resolve("gradle.properties").apply {
            createParentDirectories()
            writeText(
                buildString {
                    if (isolatedProjectsSupport != null) {
                        appendLine("kotlin.internal.web.isolatedProjectsSupport=$isolatedProjectsSupport")
                    }
                    appendLine("# enable delegated transpilation so we can also test SwcPlugin")
                    appendLine("kotlin.js.delegated.transpilation=true")
                }
            )
        }

        val project = buildProjectWithMPP(
            projectBuilder = { withProjectDir(projectDir.toFile()) }
        ) {
            kotlin {
                js {
                    browser()
                    nodejs()
                    binaries.executable()
                    binaries.library()
                }
                wasmJs {
                    browser()
                    nodejs()
                    d8()
                    binaries.executable()
                    binaries.library()
                }
                wasmWasi {
                    @Suppress("DEPRECATION")
                    nodejs()
                    wasmtime()
                    binaries.executable()
                }

                sourceSets.commonMain {
                    dependencies {
                        implementation(npm("is-even", "1.0.0"))
                    }
                }
            }
        }

        return project.evaluate()
    }

    companion object {

        private val obsoleteExtensionTypes: Set<Class<out Any>> = setOf(
            BaseNodeJsRootExtension::class.java,
            BaseNpmExtension::class.java,
            BaseYarnRootExtension::class.java,
            BaseYarnRootEnvSpec::class.java,
            WasmNpmTooling::class.java,
        )

        private val obsoleteTaskTypes: Set<Class<out Task>> = setOf(
            KotlinNpmCachesSetup::class.java,
            KotlinPackageJsonTask::class.java,
        )

        private fun Project.obsoletePlugins(): List<String> =
            listOf(
                CommonNodeJsRootPlugin::class,
                CommonNpmResolverPlugin::class,
                CommonYarnPlugin::class,
            ).flatMap { plugins.withType(it.java) }
                .map { it::class.java.simpleName.removeSuffix("_Decorated") }
                .sorted()

        private fun Project.obsoleteExtensions(): List<String> =
            extensions.extensionsSchema.elements
                .filter { element -> obsoleteExtensionTypes.any { it.isAssignableFrom(element.publicType.concreteClass) } }
                .map { it.name }
                .sorted()

        /**
         * Names of the registered obsolete tasks. Tasks are not realized.
         */
        private fun Project.obsoleteTasks(): List<String> =
            obsoleteTaskTypes.flatMap { type -> tasks.withType(type).names }.sorted()
    }
}
