/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.tools

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import java.io.File
import kotlin.test.fail
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.config.SwcConfig

object SwcRunner {
    private val swcPath = System.getProperty("swc.path")

    private val es5Config = "js/js.tests/testFixtures/org/jetbrains/kotlin/js/test/tools/es5.swcrc"
    private val es5WithEsmConfig = "js/js.tests/testFixtures/org/jetbrains/kotlin/js/test/tools/es5.esm.swcrc"

    fun exec(inputDirectory: File, moduleKind: ModuleKind, translationMode: TranslationMode) {
        val command = arrayOf(
            swcPath, *SwcConfig.getArgumentsWhen(
                inputDirectory = "./",
                outputDirectory = "./",
                configPath = if (moduleKind == ModuleKind.ES) es5WithEsmConfig else es5Config,
                fileExtension = if (moduleKind == ModuleKind.ES) "mjs" else "js",
                environmentCode = if (translationMode.production) "production" else "development"
            ).toTypedArray()
        )

        val processBuilder = ProcessBuilder(*command)
            .directory(inputDirectory)
            .redirectErrorStream(true)

        val exitValue = processBuilder.inheritIO().start().waitFor()

        if (exitValue != 0) {
            fail("swc exited with non-zero exit code $exitValue")
        }
    }
}