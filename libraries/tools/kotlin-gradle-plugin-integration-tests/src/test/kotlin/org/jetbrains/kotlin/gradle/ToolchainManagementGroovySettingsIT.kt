/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

/**
 * KT-91593: the toolchain management DSL of the settings plugin must be usable from a Groovy `settings.gradle`.
 *
 * Groovy can't pick between the `Function1` and `Action` overloads of an undecorated Kotlin object, so every DSL object
 * reachable from the settings extension has to be decorated by Gradle.
 */
@DisplayName("Toolchain management configured in a Groovy settings file")
@JsGradlePluginTests
class ToolchainManagementGroovySettingsIT : KGPBaseTest() {

    @DisplayName("Closures and Actions in toolchainManagement are resolved with the matching DSL as delegate")
    @GradleTest
    fun testToolchainManagementFromGroovySettings(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            addEcosystemPluginToBuildScriptCompilationClasspath()
            settingsGradle.appendText(
                """

                apply plugin: 'org.jetbrains.kotlin.ecosystem'

                kotlin {
                    toolchainManagement {
                        nodeJs {
                            println "nodeJs-delegate=" + (delegate instanceof org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsToolchainManagementDsl)
                        }
                        wasiAdapter {
                            println "wasiAdapter-delegate=" + (delegate instanceof org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterToolchainManagementDsl)
                        }
                        wasmTools {
                            println "wasmTools-delegate=" + (delegate instanceof org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsToolchainManagementDsl)
                            disable()
                        }
                    }
                    toolchainManagement {
                        nodeJs { println "second-block" }
                    }
                }

                kotlin.toolchainManagement(new org.gradle.api.Action() {
                    void execute(Object dsl) {
                        dsl.nodeJs(new org.gradle.api.Action() {
                            void execute(Object nodeJs) {
                                println "action-syntax-applied"
                            }
                        })
                    }
                })
                """.trimIndent()
            )

            build("help") {
                assertOutputContains("nodeJs-delegate=true")
                assertOutputContains("wasiAdapter-delegate=true")
                assertOutputContains("wasmTools-delegate=true")
                assertOutputContains("second-block")
                assertOutputContains("action-syntax-applied")
            }
        }
    }
}
