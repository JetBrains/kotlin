/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.internal.jsQuoted
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinTestRunnerCliArgs
import org.junit.Test
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.test.assertContains
import kotlin.test.assertEquals

class KotlinKarmaTest {
    @Test
    fun checkLoadWasm() {
        val npmProjectDir = createTempDirectory("tmp")
        val executableFile = npmProjectDir.resolve("kotlin/main.mjs")

        val loadWasm = createLoadWasm(npmProjectDir.toFile(), executableFile.toFile())

        assertEquals(
            "static/load.mjs",
            loadWasm.relativeTo(npmProjectDir.toFile()).invariantSeparatorsPath
        )

        assertEquals(
            """
            import * as exports from "../kotlin/main.mjs"
            try {
                const startUnitTests = "startUnitTests"
                exports[startUnitTests]?.()
                window.__karma__.loaded();
            } catch (e) {
                window.__karma__.error("Problem with loading", void 0, void 0, void 0, e)
            }
            """.trimIndent(),
            loadWasm.readText().trimIndent()
        )
    }

    @Test
    fun checkBasify() {
        val npmProjectDir = createTempDirectory("tmp")
        val executableFile = npmProjectDir.resolve("kotlin/main.wasm")

        val based = basify(npmProjectDir.toFile(), executableFile.toFile())

        assertEquals(
            "/base/kotlin/main.wasm",
            based
        )
    }

    @Test
    fun checkWasmDebugConfig() {
        checkDebugConfig(KotlinPlatformType.wasm) { content, testDir ->
            assertContains(
                content, "\"webpackCopy\": [\n" +
                        "    ${testDir.resolve("inputFile.wasm.map").normalize().absolutePathString().jsQuoted()}\n" +
                        "  ]"
            )
        }

    }

    @Test
    fun checkJsDebugConfig() {
        checkDebugConfig(KotlinPlatformType.js)
    }

    private fun checkDebugConfig(
        platformType: KotlinPlatformType,
        additionalCheck: (String, Path) -> Unit = { _, _ -> }
    ) {
        val config = KarmaConfig(
            port = 12345
        )

        val testDir = createTempDirectory("tmp")

        val nodeModules = testDir.resolve("node_modules").toFile().apply {
            mkdirs()
            val kotlinWebHelpersDist = resolve("kotlin-web-helpers/dist")
            kotlinWebHelpersDist.mkdirs()
            kotlinWebHelpersDist.resolve("kotlin-test-karma-runner.js").createNewFile()

            val static = kotlinWebHelpersDist.resolve("static")
            static.mkdirs()
            static.resolve("context.html").createNewFile()
            static.resolve("debug.html").createNewFile()
        }

        val outputResult = testDir.resolve("karma.config.js")

        writeConfig(
            config,
            testDir.resolve("inputFile").toFile(),
            NpmProjectModules(nodeModules),
            "testTask",
            KotlinTestRunnerCliArgs(),
            outputResult.toFile().printWriter(),
            emptyList(),
            emptyList(),
            emptyMap(),
            platformType,
            testDir.toFile(),
            debug = true,
        ) {}

        outputResult.toFile().readText().let {
            assertContains(it, "\"singleRun\": true")
            assertContains(it, "\"autoWatch\": false")
            assertContains(it, "\"basePath\": ${testDir.absolutePathString().jsQuoted()}")
            assertContains(it, "\"port\": 12345")
            assertContains(it, "\"browsers\": []")

            assertContains(it, "config.plugins.push('kotlin-web-helpers/dist/karma-kotlin-debug-plugin.js');")

            additionalCheck(it, testDir)
        }
    }
}