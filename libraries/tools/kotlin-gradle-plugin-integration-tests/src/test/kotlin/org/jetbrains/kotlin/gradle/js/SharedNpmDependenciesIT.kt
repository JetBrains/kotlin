/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.js

import kotlinx.serialization.json.Json
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.readText
import kotlin.test.assertEquals

@JsGradlePluginTests
class SharedNpmDependenciesIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

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
                            implementation(npm("is-even", "1.0.0"))
                            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
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
                            implementation(npm("cowsay", "9.9.9"))
                        }
                    }
                }
            }

            build("kotlinSetupSharedNpmProject", "kotlinWasmSetupSharedNpmProject") {
                assertTasksExecuted(
                    ":lib-a:jsPackageJson",
                    ":lib-a:jsTestPackageJson",
                    ":lib-b:jsPackageJson",
                    ":lib-b:jsTestPackageJson",
                    ":lib-b:wasmJsPackageJson",
                    ":lib-b:wasmJsTestPackageJson",
                    ":kotlinSetupSharedNpmProject",
                    ":kotlinWasmSetupSharedNpmProject",
                )

                val js = projectPath.resolve("build/js/shared-npm-project")
                val wasm = projectPath.resolve("build/wasm/shared-npm-project")

                assertPackageJson(js.resolve("package.json"), PackageJson("emptyKts", "unspecified").apply {
                    private = true
                    workspaces = listOf(
                        "packages/emptyKts-lib-a",
                        "packages/emptyKts-lib-a-test",
                        "packages/emptyKts-lib-b",
                        "packages/emptyKts-lib-b-test"
                    )
                })
                assertPackageJson(
                    js.resolve("packages/emptyKts-lib-a/package.json"),
                    PackageJson("emptyKts-lib-a", "0.0.0-unspecified").apply {
                        main = "kotlin/emptyKts-lib-a.js"
                        dependencies["is-even"] = "1.0.0"
                        dependencies["@js-joda/core"] = "3.2.0"
                        dependencies["format-util"] = "^1.0.5"
                    })
                assertPackageJson(
                    js.resolve("packages/emptyKts-lib-a-test/package.json"),
                    PackageJson("emptyKts-lib-a-test", "0.0.0-unspecified").apply {
                        main = "kotlin/emptyKts-lib-a-test.js"
                        dependencies["is-even"] = "1.0.0"
                        dependencies["@js-joda/core"] = "3.2.0"
                        dependencies["format-util"] = "^1.0.5"
                    })
                assertPackageJson(
                    js.resolve("packages/emptyKts-lib-b/package.json"),
                    PackageJson("emptyKts-lib-b", "0.0.0-unspecified").apply {
                        main = "kotlin/emptyKts-lib-b.js"
                        dependencies["cowsay"] = "9.9.9"
                    })
                assertPackageJson(
                    js.resolve("packages/emptyKts-lib-b-test/package.json"),
                    PackageJson("emptyKts-lib-b-test", "0.0.0-unspecified").apply {
                        main = "kotlin/emptyKts-lib-b-test.js"
                        dependencies["cowsay"] = "9.9.9"
                    })

                assertPackageJson(wasm.resolve("package.json"), PackageJson("emptyKts", "unspecified").apply {
                    private = true
                    workspaces = listOf("packages/emptyKts-lib-b", "packages/emptyKts-lib-b-test")
                })
                assertPackageJson(
                    wasm.resolve("packages/emptyKts-lib-b/package.json"),
                    PackageJson("emptyKts-lib-b", "0.0.0-unspecified").apply {
                        main = "kotlin/emptyKts-lib-b.mjs"
                        dependencies["cowsay"] = "9.9.9"
                    })
                assertPackageJson(
                    wasm.resolve("packages/emptyKts-lib-b-test/package.json"),
                    PackageJson("emptyKts-lib-b-test", "0.0.0-unspecified").apply {
                        main = "kotlin/emptyKts-lib-b-test.mjs"
                        dependencies["cowsay"] = "9.9.9"
                    })
            }

            build("kotlinSetupSharedNpmProject", "kotlinWasmSetupSharedNpmProject") {
                assertConfigurationCacheReused()
                assertTasksUpToDate(":kotlinSetupSharedNpmProject", ":kotlinWasmSetupSharedNpmProject")
            }
        }
    }

    private fun assertPackageJson(file: Path, expected: PackageJson) {
        assertFileExists(file)
        val expectedFile = createTempFile("expected-", ".json")
        expected.saveTo(expectedFile.toFile())
        assertEquals(
            Json.parseToJsonElement(expectedFile.readText()),
            Json.parseToJsonElement(file.readText()),
            "Unexpected content of $file"
        )
    }
}
