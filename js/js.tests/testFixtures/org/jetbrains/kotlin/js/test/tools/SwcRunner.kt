/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.tools

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
        val swcExecutable = swcPath ?: fail("swc: the `swc.path` system property is not set")

        val jsExtension = moduleKind.jsExtension.removePrefix(".")
        val inputFiles = collectInputFiles(artifactsDirectory, jsExtension)

        withStagedInputFiles(artifactsDirectory, inputFiles) { swcInputDir ->
            val configFile = writeConfigFile(
                swcInputDir = swcInputDir,
                moduleKind = moduleKind,
                sourceMapEnabled = sourceMapEnabled,
            )
            val command = buildCommand(
                swcExecutable = swcExecutable,
                artifactsDirectory = artifactsDirectory,
                configFile = configFile,
                moduleKind = moduleKind,
                translationMode = translationMode,
            )
            val output = runSwc(command, swcInputDir)
            validateOutputs(
                artifactsDirectory = artifactsDirectory,
                inputFiles = inputFiles,
                jsExtension = jsExtension,
                sourceMapEnabled = sourceMapEnabled,
                output = output,
            )
        }
    }

    private fun collectInputFiles(artifactsDirectory: File, jsExtension: String): List<File> {
        val jsMapExtension = "$jsExtension.map"

        // Source maps have to be transpiled together with the code they belong to, otherwise swc is unable to compose them
        // with its own ones, and all the mappings to the original `.kt` files are lost.
        val inputFiles = artifactsDirectory.walkTopDown()
            .filter { it.isFile && (it.extension == jsExtension || it.name.endsWith(jsMapExtension)) }
            .map { it.relativeTo(artifactsDirectory) }
            .toList()
        if (inputFiles.none { it.extension == jsExtension }) fail("swc: no *.$jsExtension files in $artifactsDirectory")

        return inputFiles
    }

    private inline fun withStagedInputFiles(artifactsDirectory: File, inputFiles: List<File>, block: (File) -> Unit) {
        // Move input files to a sibling directory, swc silently skips every input file located under `--out-dir`.
        // The directory is a sibling of the artifacts one on purpose: the artifacts directory is dumped recursively.
        val swcInputDir = artifactsDirectory.resolveSibling("${artifactsDirectory.name}-swc-input")
        swcInputDir.deleteRecursively()

        var succeeded = false
        try {
            swcInputDir.mkdirs()
            for (input in inputFiles) {
                moveFile(artifactsDirectory.resolve(input.path), swcInputDir.resolve(input.path))
            }

            block(swcInputDir)
            succeeded = true
        } finally {
            if (!succeeded) {
                // Put the original compiler output back, otherwise the artifacts dump and the failures reported
                // by the subsequent handlers would hide the actual reason of the failure.
                for (input in inputFiles) {
                    val movedFile = swcInputDir.resolve(input.path)
                    if (movedFile.isFile) moveFile(movedFile, artifactsDirectory.resolve(input.path))
                }
            }
            swcInputDir.deleteRecursively()
        }
    }

    private fun writeConfigFile(swcInputDir: File, moduleKind: ModuleKind, sourceMapEnabled: Boolean): File {
        val config = SwcConfig.getConfigWhen(
            sourceMapEnabled = sourceMapEnabled,
            // In tests, we're testing ES5 only
            target = "es5",
            // Since we're running our tests with D8, module resolution doesn't work, so, helpers are not used
            includeExternalHelpers = false,
            moduleKind = moduleKind
        )

        return swcInputDir.resolve(".swcrc").apply {
            writeText(Json.encodeToString(config.toJsonElement()))
        }
    }

    private fun buildCommand(
        swcExecutable: String,
        artifactsDirectory: File,
        configFile: File,
        moduleKind: ModuleKind,
        translationMode: TranslationMode,
    ): Array<String> = arrayOf(
        swcExecutable, *SwcConfig.getArgumentsWhen(
            inputDirectoryOrFiles = listOf("./"),
            outputDirectory = artifactsDirectory.absolutePath,
            configPath = configFile.absolutePath,
            fileExtension = moduleKind.jsExtension,
            environmentCode = if (translationMode.production) "production" else "development"
        ).toTypedArray()
    )

    private fun runSwc(command: Array<String>, swcInputDir: File): String {
        val process = ProcessBuilder(*command)
            .directory(swcInputDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitValue = process.waitFor()

        if (exitValue != 0) {
            fail("swc exited with $exitValue\ncommand: ${command.joinToString(" ")}\noutput:\n$output")
        }

        return output
    }

    private fun validateOutputs(
        artifactsDirectory: File,
        inputFiles: List<File>,
        jsExtension: String,
        sourceMapEnabled: Boolean,
        output: String,
    ) {
        // swc reports neither "compiled N files" nor "skipped everything", so the produced files
        // are the only usable signal.
        // With `sourceMaps: false` swc writes no source maps at all, so only the code files are expected back.
        val expectedFiles = inputFiles.filter { sourceMapEnabled || it.extension == jsExtension }
        val missing = expectedFiles.filterNot { artifactsDirectory.resolve(it.path).isFile }
        if (missing.isNotEmpty()) {
            fail("swc produced no output files for: ${missing.joinToString { it.path }}\n$output")
        }
    }

    private fun moveFile(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

/**
 * kotlinx.serialization fails to serialize generic map of Anys.
 *
 * This little helper manually transform the (possibly nested) maps into JsonElements.
 */
private fun Any?.toJsonElement(): JsonElement = when (this) {
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
