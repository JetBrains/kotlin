/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.tools

import kotlinx.serialization.encodeToString
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import java.io.File
import kotlin.test.fail
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.platform.js.SwcConfig
import kotlinx.serialization.json.Json

object SwcRunner {
    private val swcPath = System.getProperty("swc.path")

    fun exec(
        artifactsDirectory: File,
        moduleKind: ModuleKind,
        translationMode: TranslationMode,
        sourceMapEnabled: Boolean,
    ) {
        val config = SwcConfig.getConfigWhen(
            sourceMapEnabled = sourceMapEnabled,
            // In tests, we're testing ES5 only
            target = "es5",
            // Since we're running our tests with D8, module resolution doesn't work, so, helpers are not used
            includeExternalHelpers = false,
            moduleKind = moduleKind
        )

        val configFile = artifactsDirectory.resolve(".swcrc").apply {
            writeText(Json.encodeToString(config))
        }

        val command = arrayOf(
            swcPath, *SwcConfig.getArgumentsWhen(
                inputDirectoryOrFiles = listOf("./"),
                outputDirectory = "./",
                configPath = configFile.absolutePath,
                fileExtension = moduleKind.jsExtension,
                environmentCode = if (translationMode.production) "production" else "development"
            ).toTypedArray()
        )

        val processBuilder = ProcessBuilder(*command)
            .directory(artifactsDirectory)
            .redirectErrorStream(true)

        val exitValue = processBuilder.inheritIO().start().waitFor()

        if (exitValue != 0) {
            fail("swc exited with non-zero exit code $exitValue")
        }
    }
}