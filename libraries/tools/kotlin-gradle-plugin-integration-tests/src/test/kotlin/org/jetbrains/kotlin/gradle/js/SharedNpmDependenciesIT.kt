/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class, ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.js

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.test.assertEquals

@JsGradlePluginTests
class SharedNpmDependenciesIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    private val versions = NpmVersions()

    private val karmaTools = mapOf(
        versions.karma.name to versions.karma.version,
        versions.webpack.name to versions.webpack.version,
    )

    private val mochaTools = mapOf(
        versions.mocha.name to versions.mocha.version,
    )

    @DisplayName("Shared npm projects contain a package.json per compilation of the declared JS and WasmJS projects")
    @GradleTest
    fun testSharedNpmProjectContent(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
                project.dependencies.add("kotlinNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-a")))
                project.dependencies.add("kotlinNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-b")))
                project.dependencies.add("kotlinWasmNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-b")))
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-a") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        sourceSets.getByName("jsMain").dependencies {
                            npm("is-even", "1.0.0")
                        }
                    }
                }
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-b") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { browser() }
                        wasmJs { browser() }
                        sourceSets.getByName("commonMain").dependencies {
                            npm("cowsay", "9.9.9")
                        }
                    }
                }
            }

            val libADependencies = mapOf("is-even" to "1.0.0")
            val libBDependencies = mapOf("cowsay" to "9.9.9")

            build("kotlinSetupSharedNpmProject", "kotlinWasmSetupSharedNpmProject") {
                assertTasksExecuted(
                    ":lib-a:kotlinSharedPackageJson",
                    ":lib-b:kotlinSharedPackageJson",
                    ":lib-b:kotlinWasmSharedPackageJson",
                    ":kotlinSetupSharedNpmProject",
                    ":kotlinWasmSetupSharedNpmProject",
                )

                val js = projectPath.resolve("build/js/shared-npm-project")
                val wasm = projectPath.resolve("build/wasm/shared-npm-project")

                assertRootPackageJson(
                    js.resolve("package.json"),
                    workspaces = listOf(
                        "packages/emptyKts-lib-a",
                        "packages/emptyKts-lib-a-test",
                        "packages/emptyKts-lib-b",
                        "packages/emptyKts-lib-b-test",
                    ),
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-lib-a/package.json"),
                    name = "emptyKts-lib-a",
                    main = "kotlin/emptyKts-lib-a.js",
                    dependencies = libADependencies,
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-lib-a-test/package.json"),
                    name = "emptyKts-lib-a-test",
                    main = "kotlin/emptyKts-lib-a-test.js",
                    dependencies = libADependencies,
                    requiredDevDependencies = mochaTools,
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-lib-b/package.json"),
                    name = "emptyKts-lib-b",
                    main = "kotlin/emptyKts-lib-b.js",
                    dependencies = libBDependencies,
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-lib-b-test/package.json"),
                    name = "emptyKts-lib-b-test",
                    main = "kotlin/emptyKts-lib-b-test.js",
                    dependencies = libBDependencies,
                    requiredDevDependencies = karmaTools,
                )

                assertRootPackageJson(
                    wasm.resolve("package.json"),
                    workspaces = listOf("packages/emptyKts-lib-b", "packages/emptyKts-lib-b-test"),
                )
                assertWorkspacePackageJson(
                    wasm.resolve("packages/emptyKts-lib-b/package.json"),
                    name = "emptyKts-lib-b",
                    main = "kotlin/emptyKts-lib-b.mjs",
                    dependencies = libBDependencies,
                )
                assertWorkspacePackageJson(
                    wasm.resolve("packages/emptyKts-lib-b-test/package.json"),
                    name = "emptyKts-lib-b-test",
                    main = "kotlin/emptyKts-lib-b-test.mjs",
                    dependencies = libBDependencies,
                    requiredDevDependencies = karmaTools,
                )
            }

            build("kotlinSetupSharedNpmProject", "kotlinWasmSetupSharedNpmProject") {
                assertConfigurationCacheReused()
                assertTasksUpToDate(
                    ":lib-a:kotlinSharedPackageJson",
                    ":lib-b:kotlinSharedPackageJson",
                    ":lib-b:kotlinWasmSharedPackageJson",
                    ":kotlinSetupSharedNpmProject",
                    ":kotlinWasmSetupSharedNpmProject",
                )
            }
        }
    }

    private fun readPackageJson(file: Path): PackageJson {
        assertFileExists(file)
        return fromSrcPackageJson(file.toFile()) ?: error("Cannot parse $file")
    }

    private fun assertRootPackageJson(file: Path, workspaces: List<String>) {
        val packageJson = readPackageJson(file)
        assertEquals("emptyKts", packageJson.name, "name of $file")
        assertEquals("unspecified", packageJson.version, "version of $file")
        assertEquals(true, packageJson.private, "private of $file")
        assertEquals(workspaces, packageJson.workspaces?.toList(), "workspaces of $file")
    }

    private fun assertWorkspacePackageJson(
        file: Path,
        name: String,
        main: String,
        dependencies: Map<String, String>,
        requiredDevDependencies: Map<String, String> = emptyMap(),
    ) {
        val packageJson = readPackageJson(file)
        assertEquals(name, packageJson.name, "name of $file")
        assertEquals("0.0.0-unspecified", packageJson.version, "version of $file")
        assertEquals(main, packageJson.main, "main of $file")
        assertEquals(dependencies.toSortedMap(), packageJson.dependencies.toSortedMap(), "dependencies of $file")
        assertEquals(
            requiredDevDependencies.toSortedMap(),
            packageJson.devDependencies.filterKeys { it in requiredDevDependencies }.toSortedMap(),
            "devDependencies of $file",
        )
    }
}
