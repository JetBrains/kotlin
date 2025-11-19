/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMap
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapError
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapParser
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSuccess
import org.jetbrains.kotlin.js.test.handlers.D8BasedDebugRunner
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File

abstract class WasmDebugRunnerBase(testServices: TestServices) :
    D8BasedDebugRunner<BinaryArtifacts.Wasm>(testServices, ArtifactKinds.Wasm), WasmArtifactsCollector {
    protected val modulesToArtifact = mutableMapOf<TestModule, BinaryArtifacts.Wasm>()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        modulesToArtifact[module] = info
    }

    // language=html
    override val htmlCodeToIncludeBinaryArtifact = ""

    // language=js
    override val jsCodeToGetModuleWithBoxFunction = "await import('./index.mjs')"

    override val debugMode: DebugMode get() = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

    val WasmCompilerResult.parsedSourceMaps: SourceMap
        get() = when (val parseResult = SourceMapParser.parse(
            debugInformation?.sourceMapForBinary ?: error("Expect to have source maps for stepping test")
        )) {
            is SourceMapSuccess -> parseResult.value
            is SourceMapError -> error(parseResult.message)
        }


    override fun writeCompilationResult(artifact: BinaryArtifacts.Wasm, outputDir: File, compiledFileBaseName: String) {
        writeCompilationResult(artifact.compilerResult, outputDir, compiledFileBaseName)
    }

    override fun saveEntryFile(outputDir: File, content: String) {
        File(outputDir, "test.mjs").writeText(content)
    }

    override fun runSavedCode(outputDir: File): String {
        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
        val collectedJsArtifacts = collectJsArtifacts(originalFile)
        val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(outputDir)

        return WasmVM.V8.run(
            entryFile = "./${collectedJsArtifacts.entryPath}",
            jsFiles = jsFilePaths,
            workingDirectory = outputDir,
            toolArgs = listOf("--enable-inspector", "--allow-natives-syntax")
        )
    }
}