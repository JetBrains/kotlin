/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.transformBuildScriptWithPluginsDsl
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata

@MppGradlePluginTests
class MppDslWasmIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata("new-mpp-wasm-js")
    fun testWasmJs(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-wasm-js",
            gradleVersion = gradleVersion,
        ) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            buildGradleKts.replaceText("<JsEngine>", "d8")
            build("build") {
                assertTasksExecuted(":compileKotlinJs")
                assertTasksExecuted(":compileKotlinWasmJs")

                val jsPackages = "build/js/packages/"
                val jsOutput = "$jsPackages/redefined-js-module-name/kotlin/"
                assertFileInProjectExists("$jsOutput/redefined-js-module-name.js")

                val wasmOutput = "build/compileSync/wasmJs/main/productionExecutable/optimized"
                assertFileInProjectExists("$wasmOutput/redefined-wasm-module-name.mjs")
                assertFileInProjectExists("$wasmOutput/redefined-wasm-module-name.wasm")
            }
        }
    }

    @GradleTest
    @TestMetadata("new-mpp-wasm-test")
    fun testWasmNodeTest(gradleVersion: GradleVersion) = testWasmTest(gradleVersion, "nodejs", "Node")

    @GradleTest
    @TestMetadata("new-mpp-wasm-test")
    fun testWasmD8Test(gradleVersion: GradleVersion) = testWasmTest(gradleVersion, "d8", "D8")

    private fun testWasmTest(gradleVersion: GradleVersion, engine: String, name: String) {
        project(
            projectName = "new-mpp-wasm-test",
            gradleVersion = gradleVersion,
        ) {
            buildGradleKts.modify {
                transformBuildScriptWithPluginsDsl(it)
                    .replace("<JsEngine>", engine)
            }
            buildAndFail(":wasmJs${name}Test") {
                assertTasksExecuted(":compileKotlinWasmJs")
                assertTasksAreNotInTaskGraph(":compileTestDevelopmentExecutableKotlinWasmJsOptimize")
                assertTasksFailed(":wasmJs${name}Test")
                assertTestResults(
                    projectPath.resolve("TEST-${engine}.xml"),
                    "wasmJs${name}Test"
                )
            }
        }
    }
}
