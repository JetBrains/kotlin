/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.test.backend.handlers.WasmBinaryArtifactHandler
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

abstract class AbstractWasmArtifactsCollector(testServices: TestServices) : WasmBinaryArtifactHandler(testServices) {
    val modulesToArtifact = mutableMapOf<TestModule, BinaryArtifacts.Wasm>()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        modulesToArtifact[module] = info
    }

    protected fun collectJsArtifacts(originalFile: File): JsArtifacts {
        val jsFiles = mutableListOf<AdditionalFile>()
        val mjsFiles = mutableListOf<AdditionalFile>()
        var entryMjs: String? = "test.mjs"

        testServices.moduleStructure.modules.forEach { m ->
            m.files.forEach { file: TestFile ->
                val name = file.name
                when {
                    name.endsWith(".js") ->
                        jsFiles += AdditionalFile(file.name, file.originalContent)

                    name.endsWith(".mjs") -> {
                        mjsFiles += AdditionalFile(file.name, file.originalContent)
                        if (name == "entry.mjs") {
                            entryMjs = name
                        }
                    }
                }
            }
        }

        originalFile.parentFile.resolve(originalFile.nameWithoutExtension + JavaScript.DOT_EXTENSION)
            .takeIf { it.exists() }
            ?.let {
                jsFiles += AdditionalFile(it.name, it.readText())
            }

        originalFile.parentFile.resolve(originalFile.nameWithoutExtension + JavaScript.DOT_MODULE_EXTENSION)
            .takeIf { it.exists() }
            ?.let {
                mjsFiles += AdditionalFile(it.name, it.readText())
            }

        return JsArtifacts(entryMjs, jsFiles, mjsFiles)
    }

    protected fun JsArtifacts.saveJsArtifacts(baseDir: File): SavedJsArtifacts {
        val mjsFilePaths = mutableListOf<String>()
        for (mjsFile: AdditionalFile in mjsFiles) {
            val file = File(baseDir, mjsFile.name)
            file.writeText(mjsFile.content)
            mjsFilePaths += file.canonicalPath
        }

        val jsFilePaths = mutableListOf<String>()
        for (jsFile: AdditionalFile in jsFiles) {
            val file = File(baseDir, jsFile.name)
            file.writeText(jsFile.content)
            jsFilePaths += file.canonicalPath
        }

        return SavedJsArtifacts(jsFilePaths, mjsFilePaths)
    }

    protected fun processExceptions(exceptions: List<Throwable>) {
        when (exceptions.size) {
            0 -> {} // Everything OK
            1 -> {
                throw exceptions.single()
            }
            else -> {
                throw AssertionError("Failed with several exceptions. Look at suppressed exceptions below.").apply {
                    exceptions.forEach { addSuppressed(it) }
                }
            }
        }
    }

    protected class AdditionalFile(val name: String, val content: String)
    protected class JsArtifacts(val entryPath: String?, val jsFiles: List<AdditionalFile>, val mjsFiles: List<AdditionalFile>)
    protected data class SavedJsArtifacts(val jsFilePaths: List<String>, val mjsFilePaths: List<String>)
}

fun TestServices.getWasmTestOutputDirectory(): File {
    val originalFile = moduleStructure.originalTestDataFiles.first()
    val allDirectives = moduleStructure.allDirectives

    val pathToRootOutputDir = allDirectives[WasmEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR].first()
    val testGroupDirPrefix = allDirectives[WasmEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX].first()
    val pathToTestDir = allDirectives[WasmEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR].first()

    val testGroupOutputDir = File(File(pathToRootOutputDir, "out"), testGroupDirPrefix)
    val stopFile = File(pathToTestDir)
    return generateSequence(originalFile.parentFile) { it.parentFile }
        .takeWhile { it != stopFile }
        .map { it.name }
        .toList().asReversed()
        .fold(testGroupOutputDir, ::File)
        .let { File(it, originalFile.nameWithoutExtension) }
}
