/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.nio.file.Files
import kotlin.test.assertTrue

@MppGradlePluginTests
class KotlinWasmGradlePluginIT : KGPBaseTest() {

    @DisplayName("Check wasi target")
    @GradleTest
    fun wasiTarget(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-test", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build(":wasmWasiTest") {
                assertTasksExecuted(":kotlinNodeJsSetup")
                assertTasksExecuted(":compileKotlinWasmWasi")
                assertTasksExecuted(":wasmWasiNodeTest")
            }

            projectPath.resolve("src/wasmWasiTest/kotlin/Test.kt").modify {
                it.replace(
                    "fun test2() = assertEquals(foo(), 2)",
                    "fun test2() = assertEquals(foo(), 2)" + "\n" +
                            """
                            @Test
                            fun test3() = assertEquals(foo(), 3)
                            """
                )
            }

            buildAndFail(":wasmWasiTest") {
                assertTasksUpToDate(":compileKotlinWasmWasi")
                assertTasksFailed(":wasmWasiNodeTest")
            }
        }
    }

    @DisplayName("Check wasi and js target")
    @GradleTest
    fun wasiAndJsTarget(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-js-test", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build("assemble") {
                assertTasksExecuted(":app:compileKotlinWasmWasi")
                assertTasksExecuted(":app:compileKotlinWasmJs")

                assertTasksExecuted(":lib:compileKotlinWasmWasi")
                assertTasksExecuted(":lib:compileKotlinWasmJs")
            }
        }
    }
}
