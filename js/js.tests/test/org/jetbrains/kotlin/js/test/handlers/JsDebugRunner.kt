/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.test.handlers

import kotlinx.coroutines.withTimeout
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import org.jetbrains.kotlin.js.test.debugger.*
import org.jetbrains.kotlin.js.test.utils.getAllFilesForRunner
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
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
 * It runs a generated JavaScript file under a debugger, stops right before entering the `box` function,
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
        val originalFile = mainModule.files.first { !it.isAdditional }.originalFile
        val debuggerFacade = NodeJsDebuggerFacade(jsFilePath)

        val jsFile = File(jsFilePath)

        val jsFileURI = jsFile.makeURI()

        val loggedItems = mutableListOf<SteppingTestLoggedData>()

        debuggerFacade.run {
            debugger.resume()
            waitForResumeEvent()
            waitForPauseEvent {
                it.reason == Debugger.PauseReason.OTHER // hit the 'debugger' statement
            }

            suspend fun repeatedlyStepInto(action: suspend (Debugger.CallFrame) -> Boolean) {
                while (true) {
                    val topMostCallFrame = waitForPauseEvent().callFrames[0]
                    if (!action(topMostCallFrame)) break
                    debugger.stepInto()
                    waitForResumeEvent()
                }
            }

            fun Debugger.CallFrame.isInFileUnderTest() = try {
                URI(scriptUrlByScriptId(location.scriptId)) == jsFileURI
            } catch (_: URISyntaxException) {
                false
            }

            repeatedlyStepInto {
                !it.isInFileUnderTest()
            }
            repeatedlyStepInto { callFrame ->
                callFrame.isInFileUnderTest().also {
                    if (it)
                        addCallFrameInfoToLoggedItems(sourceMap, callFrame, loggedItems)
                }
            }

            debugger.resume()
            waitForResumeEvent()
        }
        checkSteppingTestResult(
            mainModule.frontendKind,
            mainModule.targetBackend ?: TargetBackend.JS_IR,
            originalFile,
            loggedItems
        )
    }

    private fun addCallFrameInfoToLoggedItems(
        sourceMap: SourceMap,
        topMostCallFrame: Debugger.CallFrame,
        loggedItems: MutableList<SteppingTestLoggedData>
    ) {
        val originalFunctionName = topMostCallFrame.functionLocation?.let {
            sourceMap.segmentForGeneratedLocation(it.lineNumber, it.columnNumber)?.name
        }
        sourceMap.segmentForGeneratedLocation(
            topMostCallFrame.location.lineNumber,
            topMostCallFrame.location.columnNumber
        )?.let { (_, sourceFile, sourceLine, _, _) ->
            if (sourceFile == null || sourceLine < 0) return@let
            val testFileName = testFileNameFromMappedLocation(sourceFile, sourceLine) ?: return
            val expectation = formatAsSteppingTestExpectation(
                testFileName,
                sourceLine + 1,
                originalFunctionName ?: topMostCallFrame.functionName,
                false,
            )
            loggedItems.add(SteppingTestLoggedData(sourceLine + 1, false, expectation))
        }
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
    fun <T> run(body: suspend Context.() -> T) = inspector.run {
        debugger.enable()
        debugger.setSkipAllPauses(false)
        runtime.runIfWaitingForDebugger()

        with(Context(this)) {
            waitForPauseEvent { it.reason == Debugger.PauseReason.BREAK_ON_START }

            withTimeout(30000) {
                body()
            }
        }
    }

    inner class Context(private val underlying: NodeJsInspectorClientContext) : NodeJsInspectorClientContext by underlying {

        fun scriptUrlByScriptId(scriptId: Runtime.ScriptId) = scriptUrls[scriptId] ?: error("unknown scriptId $scriptId")

        suspend fun waitForPauseEvent(suchThat: (Debugger.Event.Paused) -> Boolean = { true }) =
            waitForValueToBecomeNonNull {
                pausedEvent?.takeIf(suchThat)
            }

        suspend fun waitForResumeEvent() = waitForConditionToBecomeTrue { pausedEvent == null }
    }
}

private fun File.makeURI(): URI = absoluteFile.toURI().withAuthority("")

private fun URI.withAuthority(newAuthority: String?) =
    URI(scheme, newAuthority, path, query, fragment)

private fun SourceMap.segmentForGeneratedLocation(lineNumber: Int, columnNumber: Int?): SourceMapSegment? {

    val group = groups.getOrNull(lineNumber)?.takeIf { it.segments.isNotEmpty() } ?: return null
    return if (columnNumber == null || columnNumber <= group.segments[0].generatedColumnNumber) {
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
