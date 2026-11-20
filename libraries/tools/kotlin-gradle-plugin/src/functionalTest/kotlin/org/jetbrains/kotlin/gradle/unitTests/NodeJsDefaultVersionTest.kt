/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalNodeJsToolchainDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.requestDefaultNodeJs
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.utils.getByType
import org.jetbrains.kotlin.internal.KgpBuildConstants
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals

class NodeJsDefaultVersionTest {

    @TempDir
    private lateinit var projectDir: Path

    @TestFactory
    fun `toolchain and env specs use the same default version`(): List<DynamicTest> {
        val project = buildJsAndWasmProject()

        return assertNodeJsVersion(project, KgpBuildConstants.DEFAULT_NODE_JS_VERSION)
    }

    @TestFactory
    fun `default version is overridden from gradle properties`(): List<DynamicTest> {
        val overriddenVersion = "11.22.33"

        val project = buildJsAndWasmProject(defaultNodeJsVersion = overriddenVersion)

        return assertNodeJsVersion(project, overriddenVersion)
    }

    private fun buildJsAndWasmProject(
        defaultNodeJsVersion: String? = null,
    ): Project {
        if (defaultNodeJsVersion != null) {
            projectDir.createDirectories()
            projectDir.resolve("gradle.properties")
                .writeText("kotlin.defaultNodeJsVersion=$defaultNodeJsVersion")
        }
        val project = buildProjectWithMPP(
            projectBuilder = { withProjectDir(projectDir.toFile()) },
        ) {
            multiplatformExtension.apply {
                js {
                    nodejs()
                    browser()
                    binaries.executable()
                }
                wasmJs {
                    nodejs()
                }
            }
        }
        return project.evaluate()
    }

    companion object {

        private fun assertNodeJsVersion(project: Project, expected: String): List<DynamicTest> =
            listOf(
                "requestDefaultNodeJs()" to project.requestDefaultNodeJs().orNull?.version?.orNull?.version,
                "NodeJsEnvSpec" to project.extensions.getByType<NodeJsEnvSpec>().version.orNull,
                "WasmNodeJsEnvSpec" to project.extensions.getByType<WasmNodeJsEnvSpec>().version.orNull,
            ).map { (source, actual) ->
                dynamicTest(source) {
                    assertEquals(expected, actual, "Node.js version from $source")
                }
            }
    }
}
