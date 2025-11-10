/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.tools

import java.io.File
import kotlin.test.fail
import org.jetbrains.kotlin.js.config.ModuleKind

object SwcRunner {
    private val swcPath = System.getProperty("swc.path")
    private val nodePath = System.getProperty("javascript.engine.path.NodeJs")

    private val es5Config = "js/js.tests/testFixtures/org/jetbrains/kotlin/js/test/tools/es5.swcrc"
    private val es5WithEsmConfig = "js/js.tests/testFixtures/org/jetbrains/kotlin/js/test/tools/es5.esm.swcrc"

    fun exec(inputDirectory: File, moduleKind: ModuleKind) {
        val processBuilder = ProcessBuilder(
            nodePath,
            swcPath,
            "--config-file=${if (moduleKind == ModuleKind.ES) es5WithEsmConfig else es5Config}",
            "--out-dir=./",
            "--out-file-extension=${if (moduleKind == ModuleKind.ES) "mjs" else "js"}",
            inputDirectory.absolutePath
        )

        val exitValue = processBuilder.inheritIO().start().waitFor()

        if (exitValue != 0) {
            fail("swc exited with non-zero exit code $exitValue")
        }
    }
}