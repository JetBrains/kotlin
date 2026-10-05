/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.js

import kotlinx.serialization.json.Json
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.testbase.*
import kotlin.io.path.readText
import kotlin.test.assertEquals

@JsGradlePluginTests
class NpmDependenciesIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    fun `legacy configuration and modern configuration-less npm dependencies collector works the same`(gradleVersion: GradleVersion) {
        fun buildProject(
            webMainDependencies: KotlinDependencyHandler.() -> Unit,
            webTestDependencies: KotlinDependencyHandler.() -> Unit,
            jsMainDependencies: KotlinDependencyHandler.() -> Unit,
            wasmJsMainDependencies: KotlinDependencyHandler.() -> Unit,
        ) = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                kotlinMultiplatform.apply {
                    js {
                        nodejs()
                    }

                    wasmJs {
                        nodejs()
                    }

                    sourceSets.webMain.dependencies { webMainDependencies() }
                    sourceSets.jsMain.dependencies { jsMainDependencies() }
                    sourceSets.wasmJsMain.dependencies { wasmJsMainDependencies() }
                    sourceSets.webTest.dependencies { webTestDependencies() }
                }
            }

            build("jsPublicPackageJson", "wasmJsPublicPackageJson")
        }

        @Language("JSON")
        val expectedJsPackageJson = """
                {
                  "name": "empty",
                  "version": "0.0.0-unspecified",
                  "main": "empty.js",
                  "devDependencies": {
                    "js-main-dev": "1.0.0",
                    "web-main-dev": "1.0.0"
                  },
                  "dependencies": {
                    "js-main": "1.0.0",
                    "web-main": "1.0.0"
                  },
                  "peerDependencies": {
                    "web-main-peer": "1.0.0"
                  },
                  "optionalDependencies": {
                    "web-main-optional": "1.0.0"
                  },
                  "bundledDependencies": []
                }
            """.let { Json.parseToJsonElement(it) }

        @Language("JSON")
        val expectedWasmJsPackageJson = """
                {
                  "name": "empty",
                  "version": "0.0.0-unspecified",
                  "main": "empty.mjs",
                  "devDependencies": {
                    "web-main-dev": "1.0.0"
                  },
                  "dependencies": {
                    "wasm-js-main": "1.0.0",
                    "web-main": "1.0.0"
                  },
                  "peerDependencies": {
                    "web-main-peer": "1.0.0",
                    "wasm-js-main-peer": "1.0.0"
                  },
                  "optionalDependencies": {
                    "wasm-js-main-optional": "1.0.0",
                    "web-main-optional": "1.0.0"
                  },
                  "bundledDependencies": []
                }
            """.let { Json.parseToJsonElement(it) }

        fun TestProject.publicPackageJson(taskName: String) =
            Json.parseToJsonElement(projectPath.resolve("build/tmp/$taskName/package.json").readText())

        fun TestProject.jsPublicPackageJson() = publicPackageJson("jsPublicPackageJson")
        fun TestProject.wasmJsPublicPackageJson() = publicPackageJson("wasmJsPublicPackageJson")

        // TODO: remove this part and leave only modern npm dependencies collector check
        //  during transition period this test verifies that they both work the same
        val projectWithLegacyNpmDependencies = buildProject(
            webMainDependencies = {
                api(npm("web-main", "1.0.0"))
                implementation(@Suppress("DEPRECATION") devNpm("web-main-dev", "1.0.0"))
                api(@Suppress("DEPRECATION") peerNpm("web-main-peer", "1.0.0"))
                api(@Suppress("DEPRECATION") optionalNpm("web-main-optional", "1.0.0"))
            },
            webTestDependencies = {
                implementation(npm("web-test", "1.0.0"))
                implementation(@Suppress("DEPRECATION") devNpm("web-test-dev", "1.0.0"))
            },
            jsMainDependencies = {
                implementation(npm("js-main", "1.0.0"))
                compileOnly(@Suppress("DEPRECATION") devNpm("js-main-dev", "1.0.0"))
            },
            wasmJsMainDependencies = {
                implementation(npm("wasm-js-main", "1.0.0"))
                implementation(@Suppress("DEPRECATION") peerNpm("wasm-js-main-peer", "1.0.0"))
                runtimeOnly(@Suppress("DEPRECATION") optionalNpm("wasm-js-main-optional", "1.0.0"))
            },
        )

        assertEquals(
            expectedJsPackageJson,
            projectWithLegacyNpmDependencies.jsPublicPackageJson()
        )

        assertEquals(
            expectedWasmJsPackageJson,
            projectWithLegacyNpmDependencies.wasmJsPublicPackageJson()
        )

        val projectWithNpmDependenciesCollector = buildProject(
            webMainDependencies = {
                npm("web-main", "1.0.0")
                npmDev("web-main-dev", "1.0.0")
                npmPeer("web-main-peer", "1.0.0")
                npmOptional("web-main-optional", "1.0.0")
            },
            webTestDependencies = {
                npm("web-test", "1.0.0")
                npmDev("web-test-dev", "1.0.0")
            },
            jsMainDependencies = {
                npm("js-main", "1.0.0")
                npmDev("js-main-dev", "1.0.0")
            },
            wasmJsMainDependencies = {
                npm("wasm-js-main", "1.0.0")
                npmPeer("wasm-js-main-peer", "1.0.0")
                npmOptional("wasm-js-main-optional", "1.0.0")
            },
        )

        assertEquals(
            expectedJsPackageJson,
            projectWithNpmDependenciesCollector.jsPublicPackageJson()
        )

        assertEquals(
            expectedWasmJsPackageJson,
            projectWithNpmDependenciesCollector.wasmJsPublicPackageJson()
        )
    }
}
