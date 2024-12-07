/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

import org.junit.Test
import java.nio.file.Files.createTempDirectory
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
            import { startUnitTests } from "../kotlin/main.mjs"
            try {
                startUnitTests()
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
}