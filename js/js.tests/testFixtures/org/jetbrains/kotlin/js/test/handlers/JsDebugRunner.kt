/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.test.handlers


import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.engine.OneShotScriptEngine
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapError
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapParser
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSegment
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSuccess
import org.jetbrains.kotlin.js.test.utils.getAllFilesForRunner
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File
import java.util.logging.Logger

/**
 * This class is an analogue of the [DebugRunner][org.jetbrains.kotlin.test.backend.handlers.DebugRunner] from JVM stepping tests.
 *
 * It runs a generated JavaScript file under in a D8 using its internal debug API and native syntax to trace. We set a breakpoint right before entering the `box` function,
 * and performs the "step into" action until there is nothing more to step into. On each pause it records the source file name,
 * the source line and the function name of the current call frame, and compares this data with the expectations written in the test file.
 *
 * It uses sourcemaps for mapping locations in the generated JS file to the corresponding locations in the source Kotlin file.
 * Also, it assumes that the sourcemap contains absolute paths to source files. The relative paths are replaced with
 * absolute paths earlier by [JsSourceMapPathRewriter].
 *
 * For simplicity, only the [FULL_DEV][TranslationMode.FULL_DEV] translation mode is
 * supported.
 */
class JsDebugRunner(testServices: TestServices) :
    D8BasedDebugRunner<BinaryArtifacts.Js>(testServices, ArtifactKinds.Js, preserveSteppingOnTheSamePlace = true) {
    val modulesToArtifact = mutableMapOf<TestModule, BinaryArtifacts.Js>()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        if (module.name.endsWith(JsEnvironmentConfigurator.OLD_MODULE_SUFFIX)) return
        modulesToArtifact[module] = info.unwrap()
    }

    private val testModule: TestModule get() = modulesToArtifact.keys.single()

    private val artifactFileName: String
        get() = JsEnvironmentConfigurator.getJsModuleArtifactName(testServices, testModule.name) + ".js"

    // language=html
    override val htmlCodeToIncludeBinaryArtifact: String get() = "<script src='./$artifactFileName'></script>"

    // TODO: rework stepping tests to be saved into a separate directory per test
    override val debugMode: DebugMode get() = DebugMode.NONE

    // language=js
    override val jsCodeToGetModuleWithBoxFunction = "const box = main.box"

    override fun saveEntryFile(outputDir: File, content: String) {
        val testFile = File(outputDir, "test.js")
        if (testFile.exists()) return
        testFile.writeText(content)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val globalDirectives = testServices.moduleStructure.allDirectives
        val esModules = JsEnvironmentConfigurationDirectives.ES_MODULES in globalDirectives

        if (esModules) return

        val outputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices)
        val jsFilePath = getAllFilesForRunner(testServices, modulesToArtifact)[TranslationMode.FULL_DEV]?.single()
            ?: error("Only FULL translation mode is supported")

        val sourceMapFile = File("$jsFilePath.map")
        val parsedSourceMap = when (val parseResult = SourceMapParser.parse(sourceMapFile)) {
            is SourceMapSuccess -> parseResult.value
            is SourceMapError -> error(parseResult.message)
        }

        writeToFilesAndRunTest(outputDir, listOf(parsedSourceMap), artifactFileName)
    }

    override fun runSavedCode(outputDir: File) =
        OneShotScriptEngine.V8.run(
            jsFiles = listOf(artifactFileName, "./test.js"),
            workingDirectory = outputDir,
            toolArgs = listOf("--enable-inspector", "--allow-natives-syntax")
        )

    // TODO: Support "ignoreList" functionality to escape such filtering
    override fun SourceMapSegment.mapOrNull(): SourceMapSegment? {
        val sourceFile = sourceFileName ?: return null
        val testFileName = testFileNameFromMappedLocation(sourceFile, sourceLineNumber) ?: return null
        return copy(sourceFileName = testFileName)
    }

    /**
     * An original test file may represent multiple source files (by using the `// FILE: myFile.kt` comments).
     * Sourcemaps contain paths to original test files. However, in test expectations we write names as in the `// FILE:` comments.
     * This function maps a location in the original test file to the name specified in a `// FILE:` comment.
     */
    private fun testFileNameFromMappedLocation(originalFilePath: String, originalFileLineNumber: Int): String? {
        val originalFile = File(originalFilePath)
        return testServices.moduleStructure.modules.asSequence().flatMap { module -> module.files.asSequence().filter { !it.isAdditional } }
            .findLast {
                it.originalFile.absolutePath == originalFile.absolutePath && it.startLineNumberInOriginalFile <= originalFileLineNumber
            }?.name
    }
}