/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.test.handlers

import kotlinx.coroutines.withTimeout
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import org.jetbrains.kotlin.js.test.debugger.*
import org.jetbrains.kotlin.js.test.utils.getAllFilesForRunner
import org.jetbrains.kotlin.js.test.utils.getBoxFunction
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.SteppingTestLoggedData
import org.jetbrains.kotlin.test.utils.checkSteppingTestResult
import org.jetbrains.kotlin.test.utils.formatAsSteppingTestExpectation
import java.io.File
import java.net.URI
import java.net.URISyntaxException

/**
 * This class is an analogue of the [DebugRunner][org.jetbrains.kotlin.test.backend.handlers.DebugRunner] from JVM stepping tests.
 *
 * It runs a generated JavaScript file under a debugger, sets a breakpoint in the beginning of the `box` function
 * and performs the "step into" action until there is nothing more to step into. On each pause it records the source file name,
 * the source line and the function name of the current call frame, and compares this data with the expectations written in the test file.
 *
 * It uses sourcemaps for mapping locations in the generated JS file to the corresponding locations in the source Kotlin file.
 * Also, it assumes that the sourcemap contains absolute paths to source files. The relative paths are replaced with
 * absolute paths earlier by [JsSourceMapPathRewriter].
 *
 * Stepping tests only work with the IR backend. The legacy backend is not supported.
 *
 * For simplicity, only the [FULL][org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode.FULL] translation mode is
 * supported.
 *
 */
class JsDebugRunner(testServices: TestServices) : AbstractJsArtifactsCollector(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (someAssertionWasFailed) return

        val globalDirectives = testServices.moduleStructure.allDirectives
        val esModules = JsEnvironmentConfigurationDirectives.ES_MODULES in globalDirectives

        if (esModules) return

        // This file generated in the FULL mode should be self-sufficient.
        val jsFilePath = getAllFilesForRunner(testServices, modulesToArtifact)[TranslationMode.FULL]?.single()
            ?: error("Only FULL translation mode is supported")

        val mainModule = JsEnvironmentConfigurator.getMainModule(testServices)

        val sourceMapFile = File("$jsFilePath.map")
        val sourceMap = when (val parseResult = SourceMapParser.parse(sourceMapFile)) {
            is SourceMapSuccess -> parseResult.value
            is SourceMapError -> error(parseResult.message)
        }

        runGeneratedCode(jsFilePath, sourceMap, mainModule)
    }

    private fun runGeneratedCode(
        jsFilePath: String,
        sourceMap: SourceMap,
        mainModule: TestModule,
    ) {
        val (testFileWithBoxFunction, boxFunctionStartLine) = getBoxFunctionStartLocation(mainModule)
        val originalFileWithBoxFunction = testFileWithBoxFunction.originalFile

        val boxFunctionLineInGeneratedFile =
            sourceMap.breakpointLineInGeneratedFile(originalFileWithBoxFunction, boxFunctionStartLine)

        if (boxFunctionLineInGeneratedFile < 0)
            error("Could not find the location of the 'box' function in the generated file")

        val debuggerFacade = NodeJsDebuggerFacade(jsFilePath)

        val jsFileURI = File(jsFilePath).absoluteFile.toURI().withAuthority("")

        val loggedItems = mutableListOf<SteppingTestLoggedData>()
        debuggerFacade.run {
            with(debuggerFacade) {
                val boxFunctionBreakpoint = debugger.setBreakpointByUrl(boxFunctionLineInGeneratedFile, jsFileURI.toString())
                debugger.resume()
                waitForResumeEvent()
                waitForPauseEvent {
                    it.reason == Debugger.PauseReason.OTHER && it.hitBreakpoints.contains(boxFunctionBreakpoint.breakpointId)
                }
                while (true) {
                    val topMostCallFrame = waitForPauseEvent().callFrames[0]
                    try {
                        if (URI(scriptUrlByScriptId(topMostCallFrame.location.scriptId)) != jsFileURI) break
                    } catch (_: URISyntaxException) {
                        // Probably something like 'evalmachine.<anonymous>' brought us here. Ignore.
                    }
                    addCallFrameInfoToLoggedItems(sourceMap, topMostCallFrame, loggedItems)
                    debugger.stepInto()
                    waitForResumeEvent()
                }
                debugger.resume()
                waitForResumeEvent()
            }
        }
        checkSteppingTestResult(
            mainModule.frontendKind,
            mainModule.targetBackend ?: TargetBackend.JS_IR,
            originalFileWithBoxFunction,
            loggedItems
        )
    }

    private fun addCallFrameInfoToLoggedItems(
        sourceMap: SourceMap,
        topMostCallFrame: Debugger.CallFrame,
        loggedItems: MutableList<SteppingTestLoggedData>
    ) {
        val originalFunctionName = topMostCallFrame.functionLocation?.let {
            sourceMap.getSourceLineForGeneratedLocation(it)?.name
        }
        sourceMap.getSourceLineForGeneratedLocation(topMostCallFrame.location)?.let { (_, sourceFile, sourceLine, _, _) ->
            if (sourceFile == null || sourceLine < 0) return@let
            val testFileName = testFileNameFromMappedLocation(sourceFile, sourceLine) ?: return
            val expectation =
                formatAsSteppingTestExpectation(testFileName, sourceLine + 1, originalFunctionName ?: topMostCallFrame.functionName, false)
            loggedItems.add(SteppingTestLoggedData(sourceLine + 1, false, expectation))
        }
    }

    /**
     * Returns the test file and the line number in that file where the body of the `box` function begins.
     */
    private fun getBoxFunctionStartLocation(mainModule: TestModule): Pair<TestFile, Int> {
        val boxFunction = getBoxFunction(testServices) ?: error("Missing 'box' function")
        val file = boxFunction.containingKtFile
        val mapping = KtPsiSourceFileLinesMapping(file)
        val firstStatementOffset = boxFunction.bodyBlockExpression?.firstStatement?.startOffset
            ?: boxFunction.bodyExpression?.startOffset
            ?: boxFunction.startOffset
        return mainModule.files.single { it.name == file.name } to mapping.getLineByOffset(firstStatementOffset)
    }

    /**
     * Maps the location in the source file to the location in the generated file.
     *
     * The Node.js debugger is not sourcemap-aware, so we need to set a breakpoint in the `box` function in the generated JS file.
     *
     * We don't know where the generated `box` function is located, so we use the source map to figure it out.
     *
     * This is basically what Intellij IDEA's built-in JavaScript debugger does when you set a breakpoint in a source file: it tries
     * to map the location of the breakpoint in the source file to a location in the generated file. Here we use a simplified
     * algorithm for that.
     */
    private fun SourceMap.breakpointLineInGeneratedFile(sourceFile: File, sourceLine: Int): Int {
        val sourceFileAbsolutePath = sourceFile.absoluteFile.normalize()
        var candidateSegment: Pair<Int, SourceMapSegment>? = null
        for ((generatedLineNumber, group) in groups.withIndex()) {
            for (segment in group.segments) {
                if (segment.sourceFileName?.let { File(it).absoluteFile.normalize() } != sourceFileAbsolutePath ||
                    segment.sourceLineNumber != sourceLine)
                    continue
                if (candidateSegment == null)
                    candidateSegment = generatedLineNumber to segment
                // Find the segment that points to the earliest column in the source file
                if (segment.sourceColumnNumber < candidateSegment.second.sourceColumnNumber)
                    candidateSegment = generatedLineNumber to segment
            }
        }
        return candidateSegment?.first ?: -1
    }

    /**
     * Maps [location] in the generated JavaScript file to the corresponding location in a source file.
     * @return The source file path (as specified in the source map) and the line number in that source file.
     */
    private fun SourceMap.getSourceLineForGeneratedLocation(location: Debugger.Location): SourceMapSegment? {

        val group = groups.getOrNull(location.lineNumber)?.takeIf { it.segments.isNotEmpty() } ?: return null
        val columnNumber = location.columnNumber ?: return group.segments[0]
        return if (columnNumber <= group.segments[0].generatedColumnNumber) {
            group.segments[0]
        } else {
            val candidateIndex = group.segments.indexOfFirst {
                columnNumber <= it.generatedColumnNumber
            }
            if (candidateIndex < 0)
                null
            else if (candidateIndex == 0 || group.segments[candidateIndex].generatedColumnNumber == columnNumber)
                group.segments[candidateIndex]
            else
                group.segments[candidateIndex - 1]
        }
    }

    /**
     * An original test file may represent multiple source files (by using the `// FILE: myFile.kt` comments).
     * Sourcemaps contain paths to original test files. However, in test expectations we write names as in the `// FILE:` comments.
     * This function maps a location in the original test file to the name specified in a `// FILE:` comment.
     */
    private fun testFileNameFromMappedLocation(originalFilePath: String, originalFileLineNumber: Int): String? {
        val originalFile = File(originalFilePath)
        return testServices.moduleStructure.modules.flatMap { it.files }.findLast {
            it.originalFile.absolutePath == originalFile.absolutePath && it.startLineNumberInOriginalFile <= originalFileLineNumber
        }?.name
    }
}

/**
 * A wrapper around [NodeJsInspectorClient] that handles all the ceremony and allows us to only care about executing common debugging
 * actions.
 *
 * @param jsFilePath the test file to execute and debug.
 */
private class NodeJsDebuggerFacade(jsFilePath: String) {

    private val inspector =
        NodeJsInspectorClient("js/js.tests/test/org/jetbrains/kotlin/js/test/debugger/stepping_test_executor.js", listOf(jsFilePath))

    private val scriptUrls = mutableMapOf<Runtime.ScriptId, String>()

    private var pausedEvent: Debugger.Event.Paused? = null

    init {
        inspector.onEvent { event ->
            when (event) {
                is Debugger.Event.ScriptParsed -> {
                    scriptUrls[event.scriptId] = event.url
                }

                is Debugger.Event.Paused -> {
                    pausedEvent = event
                }

                is Debugger.Event.Resumed -> {
                    pausedEvent = null
                }

                else -> {}
            }
        }
    }

    /**
     * By the time [body] is called, the execution is paused, no code is executed yet.
     */
    fun <T> run(body: suspend NodeJsInspectorClientContext.() -> T) = inspector.run {
        debugger.enable()
        debugger.setSkipAllPauses(false)
        runtime.runIfWaitingForDebugger()
        waitForPauseEvent { it.reason == Debugger.PauseReason.BREAK_ON_START }

        withTimeout(30000) {
            body()
        }
    }

    fun scriptUrlByScriptId(scriptId: Runtime.ScriptId) = scriptUrls[scriptId] ?: error("unknown scriptId")

    suspend fun NodeJsInspectorClientContext.waitForPauseEvent(suchThat: (Debugger.Event.Paused) -> Boolean = { true }) =
        waitForValueToBecomeNonNull {
            pausedEvent?.takeIf(suchThat)
        }

    suspend fun NodeJsInspectorClientContext.waitForResumeEvent() = waitForConditionToBecomeTrue { pausedEvent == null }
}

private fun URI.withAuthority(newAuthority: String?) =
    URI(scheme, newAuthority, path, query, fragment)
