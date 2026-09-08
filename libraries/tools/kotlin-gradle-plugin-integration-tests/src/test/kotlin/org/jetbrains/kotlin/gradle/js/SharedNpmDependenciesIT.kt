/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class, ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.js

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertEquals

@JsGradlePluginTests
class SharedNpmDependenciesIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @DisplayName("The root project collects the package.json files of all JS/WasmJS subprojects")
    @GradleTest
    fun testCollection(gradleVersion: GradleVersion) {
        sharedNpmProject(gradleVersion) {
            build("kotlinSetupSharedNpmProjectJs", "kotlinSetupSharedNpmProjectWasmJs") {
                assertTasksExecuted(
                    ":lib-a:jsPackageJson",
                    ":lib-b:jsPackageJson",
                    ":lib-b:wasmJsPackageJson",
                    ":kotlinSetupSharedNpmProjectJs",
                    ":kotlinSetupSharedNpmProjectWasmJs",
                )

                assertSharedNpmProjectAssembled(
                    sharedNpmProjectDir = projectPath.resolve("build/js/shared-npm-project"),
                    expectedWorkspaceCount = 4,
                    expectedDependencies = mapOf("is-even" to "1.0.0", "cowsay" to "9.9.9"),
                )
                assertSharedNpmProjectAssembled(
                    sharedNpmProjectDir = projectPath.resolve("build/wasm/shared-npm-project"),
                    expectedWorkspaceCount = 2,
                    expectedDependencies = mapOf("cowsay" to "9.9.9"),
                )
            }

            build("kotlinSetupSharedNpmProjectJs", "kotlinSetupSharedNpmProjectWasmJs") {
                assertConfigurationCacheReused()
                assertTasksUpToDate(
                    ":lib-a:jsPackageJson",
                    ":lib-b:jsPackageJson",
                    ":lib-b:wasmJsPackageJson",
                    ":kotlinSetupSharedNpmProjectJs",
                    ":kotlinSetupSharedNpmProjectWasmJs",
                )
            }
        }
    }

    /**
     * A multi-project build with the manager plugin applied to the root project:
     * `lib-a` (JS), `lib-b` (JS + WasmJS), and `lib-c` without any Kotlin target,
     * which must be skipped by the lenient collection.
     */
    private fun sharedNpmProject(
        gradleVersion: GradleVersion,
        test: TestProject.() -> Unit,
    ) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
                for (path in listOf(":lib-a", ":lib-b", ":lib-c")) {
                    project.dependencies.add("kotlinNpmSharedDependenciesJs", project.dependencies.project(mapOf("path" to path)))
                }
                project.dependencies.add("kotlinNpmSharedDependenciesWasmJs", project.dependencies.project(mapOf("path" to ":lib-b")))
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-a") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        sourceSets.getByName("jsMain").dependencies {
                            implementation(npm("is-even", "1.0.0"))
                        }
                    }
                }
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-b") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        wasmJs { nodejs() }
                        sourceSets.getByName("commonMain").dependencies {
                            implementation(npm("cowsay", "9.9.9"))
                        }
                    }
                }
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-c")

            test()
        }
    }

    /**
     * Asserts the assembled shared-npm-project structure: a root `package.json` with
     * `private: true` and the expected workspaces, and that every expected npm dependency
     * appears in exactly the declared version in some workspace `package.json`.
     */
    private fun assertSharedNpmProjectAssembled(
        sharedNpmProjectDir: Path,
        expectedWorkspaceCount: Int,
        expectedDependencies: Map<String, String>,
    ) {
        val rootPackageJsonFile = sharedNpmProjectDir.resolve("package.json")
        assertFileExists(rootPackageJsonFile)

        val rootPackageJson = json.parseToJsonElement(rootPackageJsonFile.readText()).jsonObject
        assertEquals(true, rootPackageJson.getValue("private").jsonPrimitive.content.toBoolean())

        val workspaces = rootPackageJson.getValue("workspaces").jsonArray.map { it.jsonPrimitive.content }
        assertEquals(expectedWorkspaceCount, workspaces.size, "Unexpected workspaces: $workspaces")

        val dependenciesByName = mutableMapOf<String, String>()
        for (workspace in workspaces) {
            val packageJsonFile = sharedNpmProjectDir.resolve(workspace).resolve("package.json")
            assertFileExists(packageJsonFile)
            val packageJson = json.parseToJsonElement(packageJsonFile.readText()).jsonObject
            val dependencies = packageJson["dependencies"]?.jsonObject ?: continue
            for (entry in dependencies) {
                dependenciesByName[entry.key] = entry.value.jsonPrimitive.content
            }
        }

        for (entry in expectedDependencies) {
            assertEquals(
                entry.value,
                dependenciesByName[entry.key],
                "Expected npm dependency '${entry.key}' in a workspace of $sharedNpmProjectDir.",
            )
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}
