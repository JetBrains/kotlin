/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.test.handlers

import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter
import com.google.gwt.dev.js.rhino.CodePosition
import com.google.gwt.dev.js.rhino.offsetOf
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.parser.parseFunction
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import org.jetbrains.kotlin.js.test.debugger.*
import org.jetbrains.kotlin.js.test.utils.getAllFilesForRunner
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.*
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.util.logging.Level
import java.util.logging.Logger

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
class JsDebugRunner(testServices: TestServices, private val localVariables: Boolean) : AbstractJsArtifactsCollector(testServices) {

    private val logger = Logger.getLogger(this::class.java.name)

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (someAssertionWasFailed) return

        val globalDirectives = testServices.moduleStructure.allDirectives
        val esModules = JsEnvironmentConfigurationDirectives.ES_MODULES in globalDirectives

        if (esModules) return

        // This file generated in the FULL mode should be self-sufficient.
        val jsFilePath = getAllFilesForRunner(testServices, modulesToArtifact)[TranslationMode.FULL_DEV]?.single()
            ?: error("Only FULL translation mode is supported")

        val mainModule = JsEnvironmentConfigurator.getMainModule(testServices)

        val sourceMapFile = File("$jsFilePath.map")
        val sourceMap = when (val parseResult = SourceMapParser.parse(sourceMapFile)) {
            is SourceMapSuccess -> parseResult.value
            is SourceMapError -> error(parseResult.message)
        }

        val numberOfAttempts = 5
        retry(
            numberOfAttempts,
            action = { runGeneratedCode(jsFilePath, sourceMap, mainModule) },
            predicate = { attempt, e ->
                when (e) {
                    is NodeExitedException -> {
                        logger.log(Level.WARNING, "Node.js abruptly exited. Attempt $attempt out of $numberOfAttempts failed.", e)
                        true
                    }
                    else -> false
                }
            }
        )
    }

    private fun runGeneratedCode(
        jsFilePath: String,
        sourceMap: SourceMap,
        mainModule: TestModule,
    ) {
        val originalFile = mainModule.files.first { !it.isAdditional }.originalFile
        val debuggerFacade = NodeJsDebuggerFacade(jsFilePath, localVariables)

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
                        addCallFrameInfoToLoggedItems(jsFile, sourceMap, callFrame, loggedItems)
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

    private suspend fun NodeJsDebuggerFacade.Context.addCallFrameInfoToLoggedItems(
        jsFile: File,
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
                getLocalVariables(jsFile, sourceMap, topMostCallFrame),
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
private class NodeJsDebuggerFacade(jsFilePath: String, private val localVariables: Boolean) {

    private val inspector =
        NodeJsInspectorClient("js/js.tests/test/org/jetbrains/kotlin/js/test/debugger/stepping_test_executor.js", listOf(jsFilePath))

    private val scriptUrls = mutableMapOf<Runtime.ScriptId, String>()

    private var pausedEvent: Debugger.Event.Paused? = null

    private val sourceCache = mutableMapOf<URI, String>()

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

        suspend fun getLocalVariables(
            jsFile: File,
            sourceMap: SourceMap,
            callFrame: Debugger.CallFrame
        ): List<LocalVariableRecord>? {
            if (!localVariables) return null
            val functionScope = callFrame.scopeChain.find { it.type in setOf(Debugger.ScopeType.LOCAL, Debugger.ScopeType.CLOSURE) }
                ?: return null
            val scopeStart = functionScope.startLocation?.toCodePosition() ?: error("Missing scope location")
            val scopeEnd = functionScope.endLocation?.toCodePosition() ?: error("Missing scope location")
            val jsFileURI = jsFile.makeURI()
            require(URI(scriptUrlByScriptId(functionScope.startLocation.scriptId)) == jsFileURI) {
                "Invalid scope location: $scopeStart. Expected scope location to be in $jsFile"
            }

            val sourceText = sourceCache.getOrPut(jsFileURI, jsFile::readText)

            val scopeText = sourceText.let {
                it.substring(it.offsetOf(scopeStart), it.offsetOf(scopeEnd))
            }

            val prefix = "function"

            // Function scope starts with an open paren, so we need to add the keyword to make it valid JavaScript.
            // TODO: This will not work with arrows. As of 2022 we don't generate them, but we might in the future.
            val parseableScopeText = prefix + scopeText
            val scope = JsProgram().scope
            val jsFunction = parseFunction(
                parseableScopeText,
                jsFile.name,
                CodePosition(scopeStart.line, scopeStart.offset - prefix.length),
                0,
                ThrowExceptionOnErrorReporter,
                scope
            ) ?: error("Could not parse scope: \n$parseableScopeText")

            val variables = mutableListOf<SourceInfoAwareJsNode /* JsVars.JsVar | JsParameter */>()

            object : JsVisitor() {
                override fun visitElement(node: JsNode) {
                    node.acceptChildren(this)
                }

                override fun visit(x: JsVars.JsVar) {
                    super.visit(x)
                    variables.add(x)
                }

                override fun visitParameter(x: JsParameter) {
                    super.visitParameter(x)
                    variables.add(x)
                }
            }.accept(jsFunction)

            val nameMapping = variables.mapNotNull { variable ->
                if (variable !is HasName) error("Unexpected JsNode: $variable")

                // Filter out variables declared in nested functions
                if (!jsFunction.scope.hasOwnName(variable.name.toString())) return@mapNotNull null

                val location = variable.source
                if (location !is JsLocation?) error("JsLocation expected. Found instead: $location")
                if (location == null)
                    null
                else sourceMap.segmentForGeneratedLocation(location.startLine, location.startChar)?.name?.let {
                    it to variable.name.toString()
                }
            }

            if (nameMapping.isEmpty()) return emptyList()

            val expression = nameMapping.joinToString(separator = ",", prefix = "[", postfix = "]") { (_, generatedName) ->
                "__makeValueDescriptionForSteppingTests($generatedName)"
            }
            val evaluationResult = debugger.evaluateOnCallFrame(callFrame.callFrameId, expression, returnByValue = true)
            if (evaluationResult.exceptionDetails != null) {
                evaluationResult.exceptionDetails.rethrow()
            }

            val valueDescriptions =
                Json.Default.decodeFromJsonElement<List<ValueDescription?>>(evaluationResult.result.value ?: error("missing value"))

            return nameMapping.mapIndexedNotNull { i, (originalName, _) ->
                valueDescriptions[i]?.toLocalVariableRecord(originalName)
            }
        }

        private fun Runtime.ExceptionDetails.rethrow(): Nothing {
            if (exception?.description != null) error(exception.description)
            if (scriptId == null) error(text)
            val scriptURL = scriptUrls[scriptId] ?: url ?: error(text)
            error("$text ($scriptURL:$lineNumber:$columnNumber)")
        }
    }
}

private fun File.makeURI(): URI = absoluteFile.toURI().withAuthority("")

private fun URI.withAuthority(newAuthority: String?) =
    URI(scheme, newAuthority, path, query, fragment)

private fun Debugger.Location.toCodePosition() = CodePosition(lineNumber, columnNumber ?: -1)

@Serializable
private class ValueDescription(val isNull: Boolean, val isReferenceType: Boolean, val valueDescription: String, val typeName: String) {
    fun toLocalVariableRecord(variableName: String) = LocalVariableRecord(
        variable = variableName,
        variableType = null, // In JavaScript variables are untyped
        value = when {
            isNull -> LocalNullValue
            isReferenceType -> LocalReference("", typeName)
            else -> LocalPrimitive(valueDescription, typeName)
        }
    )
}

/**
 * Retries [action] the specified number of [times]. If [action] throws an exception, calls [predicate] to determine if
 * another run should be attempted. If [predicate] returns `false`, rethrows the exception.
 *
 * If after the last attempt results in an exception, rethrows that exception without calling [predicate].
 */
internal inline fun <T> retry(times: Int, action: (Int) -> T, predicate: (Int, Throwable) -> Boolean): T {
    if (times < 1) throw IllegalArgumentException("'times' argument must be at least 1")
    for (i in 1..times) {
        try {
            return action(i)
        } catch (e: Throwable) {
            if (i == times || !predicate(i, e)) throw e
        }
    }
    throw IllegalStateException("unreachable")
}
