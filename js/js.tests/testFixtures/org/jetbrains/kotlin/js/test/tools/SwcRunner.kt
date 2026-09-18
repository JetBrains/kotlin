/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.tools

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import java.io.File
import kotlin.test.fail
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.platform.js.SwcConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
            writeText(Json.encodeToString(config.toJsonElement()))
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

/**
 * kotlinx.serialization fails to serialize generic map of Anys.
 *
 * This little helper manually transform the (possibly nested) maps into JsonElements.
 */
fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(this.map { it.key.toString() to it.value.toJsonElement() }.toMap())
    is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
    is Array<*> -> JsonArray(this.map { it.toJsonElement() })
    is JsonElement -> this
    else -> throw IllegalArgumentException("Unsupported type: ${this::class}")
}
