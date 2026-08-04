/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.js

import kotlinx.serialization.json.Json
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNpmTooling
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.js.PackageLockJson
import org.junit.jupiter.api.assertNotNull
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.test.assertEquals

@GradleTestVersions(
    // Gradle version is irrelevant
    minVersion = TestVersions.Gradle.MAX_SUPPORTED
)
sealed class WasmNpmToolingDependenciesIT(
    private val packageManager: String,
) : KGPBaseTest() {

    class Npm : WasmNpmToolingDependenciesIT(packageManager = "npm")

    class Yarn : WasmNpmToolingDependenciesIT(packageManager = "yarn")

    private val useYarn: Boolean = when (packageManager) {
        "yarn" -> true
        "npm" -> false
        else -> error("unknown packageManager $packageManager")
    }

    @GradleTest
    @MppGradlePluginTests
    fun `can override versions of Wasm npm tooling dependencies`(
        gradleVersion: GradleVersion,
    ) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }

            val wasmJsNpmToolingDir = projectPath.resolve("customWasmJsNpmToolingDir")
                .createDirectories().toFile()

            gradleProperties.appendText("\nkotlin.js.yarn=${useYarn}\n")
            @Suppress("INVISIBLE_REFERENCE")
            val defaultWebpackVersion = NpmVersions.defaultVersions.getValue("webpack")

            // npm semver supports ranges. Spaces means 'AND'. So adding `>0.0.0` means nothing.
            // We can just add it to check this custom version gets propagated to the `package.json`.
            val overrideWebpackVersion = ">0.0.0 $defaultWebpackVersion"

            buildScriptInjection {
                kotlinMultiplatform.apply {
                    wasmJs {
                        browser()
                        binaries.executable()
                    }
                }

                project.plugins.withType<WasmNodeJsRootPlugin> {
                    project.extensions.configure<WasmNodeJsRootExtension> {
                        versions.apply {
                            webpack.version = overrideWebpackVersion
                        }
                    }
                }

                // override the default shared install dir, so our tests don't corrupt the live shared dir
                project.plugins.withType<WasmNodeJsRootPlugin>().configureEach { _ ->
                    project.extensions.configure<WasmNpmTooling> {
                        @Suppress("INVISIBLE_REFERENCE")
                        defaultInstallationDir.fileValue(wasmJsNpmToolingDir)
                    }
                }
            }

            build("kotlinWasmToolingSetup") {

                assertTasksExecuted(":kotlinWasmToolingSetup")

                val kgpNpmToolingDir = wasmJsNpmToolingDir.toPath().listDirectoryEntries().singleOrNull()
                assertNotNull(kgpNpmToolingDir) {
                    "missing npm tooling directory in ${wasmJsNpmToolingDir}. " +
                            "All entries: " +
                            wasmJsNpmToolingDir.toPath().listDirectoryEntries()
                }

                val packageJsonFile = kgpNpmToolingDir.resolve("package.json")
                assertFileExists(packageJsonFile)

                val packageJson = json.decodeFromString<PackageLockJson.Package>(packageJsonFile.readText())
                assertEquals(
                    overrideWebpackVersion,
                    packageJson.dependencies["webpack"],
                )
            }
        }
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }
}
