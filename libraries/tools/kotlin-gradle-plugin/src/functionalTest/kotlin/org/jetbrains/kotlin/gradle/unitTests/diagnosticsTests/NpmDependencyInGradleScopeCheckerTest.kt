/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION") // the deprecated npm DSL is the subject of this test

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.NpmDependencyInGradleScope
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.checkDiagnosticsWithMppProject
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import kotlin.test.Test

@OptIn(ExperimentalWasmDsl::class)
class NpmDependencyInGradleScopeCheckerTest {

    @Test
    fun testNpmDependencyInEveryGradleScope() {
        checkDiagnosticsWithMppProject("npmDependencyInEveryGradleScope") {
            kotlin {
                js { browser() }
                wasmJs { browser() }

                sourceSets.getByName("jsMain").dependencies {
                    api(npm("api-dependency", "1.0.0"))
                    implementation(npm("implementation-dependency", "1.0.0"))
                    compileOnly(npm("compile-only-dependency", "1.0.0"))
                    runtimeOnly(npm("runtime-only-dependency", "1.0.0"))
                }

                // Check that dependencies of every source set are reported, not only of the first one
                sourceSets.getByName("wasmJsMain").dependencies {
                    implementation(npm("wasm-dependency", "1.0.0"))
                }
            }
        }
    }

    @Test
    fun testNpmDependencyKindsInGradleScope() {
        checkDiagnosticsWithMppProject("npmDependencyKindsInGradleScope") {
            kotlin {
                js { browser() }

                sourceSets.getByName("jsMain").dependencies {
                    implementation(devNpm("karma", "6.4.0"))
                    implementation(optionalNpm("is-even", "1.0.0"))
                    implementation(peerNpm("react", "18.0.0"))
                }
            }
        }
    }

    @Test
    fun testLocalPathNpmDependencyInGradleScope() {
        checkDiagnosticsWithMppProject("localPathNpmDependencyInGradleScope") {
            val npmDirectory = layout.projectDirectory.dir("npm/is-odd-even").asFile.also { it.mkdirs() }

            kotlin {
                js { browser() }

                sourceSets.getByName("jsMain").dependencies {
                    implementation(npm("is-odd-even", npmDirectory))
                }
            }
        }
    }

    @Test
    fun testNpmDependenciesDeclaredWithNewDsl() {
        val project = buildProjectWithMPP {
            kotlin {
                js { browser() }

                sourceSets.getByName("jsMain").dependencies {
                    npm("is-odd-even", "1.0.0")
                    npmDev("karma", "6.4.0")
                    npmOptional("is-even", "1.0.0")
                    npmPeer("react", "18.0.0")
                }
            }
        }.evaluate()

        project.assertNoDiagnostics(NpmDependencyInGradleScope)
    }

    @Test
    fun testSuppressedNpmDependencyInGradleScope() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                propertiesExtension.set("kotlin.suppressGradlePluginWarnings", "NpmDependencyInGradleScope")
            }
        ) {
            kotlin {
                js { browser() }

                sourceSets.getByName("jsMain").dependencies {
                    implementation(npm("is-odd-even", "1.0.0"))
                }
            }
        }.evaluate()

        project.assertNoDiagnostics(NpmDependencyInGradleScope)
    }
}
