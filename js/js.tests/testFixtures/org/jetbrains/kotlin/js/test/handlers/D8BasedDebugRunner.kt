/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonArray
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonNode
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonNumber
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonObject
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonString
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMap
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSegment
import org.jetbrains.kotlin.js.parser.sourcemaps.parseJson
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.model.ArtifactKind
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultDirectives
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.SteppingTestLoggedData
import org.jetbrains.kotlin.test.utils.checkSteppingTestResult
import org.jetbrains.kotlin.test.utils.formatAsSteppingTestExpectation
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

abstract class D8BasedDebugRunner<A : ResultingArtifact.Binary<A>>(
    testServices: TestServices,
    artifactKind: ArtifactKind<A>,
    failureDisablesNextSteps: Boolean = false,
    doNotRunIfThereWerePreviousFailures: Boolean = false,
    private val includeColumnInformation: Boolean = false,
    private val preserveSteppingOnTheSamePlace: Boolean = false,
) : BinaryArtifactHandler<A>(testServices, artifactKind, failureDisablesNextSteps, doNotRunIfThereWerePreviousFailures) {
    abstract val debugMode: DebugMode
    abstract val jsCodeToGetModuleWithBoxFunction: String
    abstract val htmlCodeToIncludeBinaryArtifact: String

    abstract fun saveEntryFile(outputDir: File, content: String)
    abstract fun runSavedCode(outputDir: File): String

    protected open fun SourceMapSegment.mapOrNull(): SourceMapSegment? = this

    open fun writeToFilesAndRunTest(outputDir: File, sourceMaps: List<SourceMap>, compiledFileName: String) {
        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

        // language=js
        val testFileContent = """
            let messageId = 0;
            const locations = [];
            function addLocation(frame) {
                locations.push({
                  functionName: frame.functionName,
                  line: frame.location.lineNumber,
                  column: frame.location.columnNumber,
                  functionStartLine: frame.functionLocation?.lineNumber,
                  functionStartColumn: frame.functionLocation?.columnNumber
                })
            }
            function sendMessage(message) { send(JSON.stringify(Object.assign(message, { id: messageId++ }))) } 
            function enableDebugger() { sendMessage({ method: 'Debugger.enable' }) }
            function disableDebugger() { sendMessage({ method: 'Debugger.disable' }) }
            function stepInto() { sendMessage({ method: "Debugger.stepInto" }) ;}
            function setBreakpoint(func) {
                const scriptId = %FunctionGetScriptId(func); 
                const offset = %FunctionGetScriptSourcePosition(func);
                const loc = %ScriptLocationFromLine2(scriptId, undefined, undefined, offset);
                sendMessage({
                  method: "Debugger.setBreakpoint",
                  params: {
                    location: {
                      scriptId: scriptId.toString(),
                      lineNumber: loc.line,
                      columnNumber: loc.column,
                    }
                  }
                })
            }
            globalThis.receive = function(message) {
                message = JSON.parse(message);
                if (message.method == "Debugger.paused") {
                    addLocation(message.params.callFrames[0]);
                    stepInto();
                }
            }
            
            $jsCodeToGetModuleWithBoxFunction;
            
            enableDebugger();
            setBreakpoint(box);
            try {
                if (box.length) {
                  box(jsModule.makeEmptyContinuation());
                } else {
                  box();
                }
            } catch(e) { console.error(e) }
            disableDebugger();
            print(JSON.stringify(locations))
        """.trimIndent()

        saveEntryFile(outputDir, testFileContent)

        if (debugMode >= DebugMode.DEBUG) {
            File(outputDir, "index.html").writeText(
                // language=html
                """
                        <!DOCTYPE html>
                        <html lang="en">
                        <body>
                            <span id="test">UNKNOWN</span>
                            $htmlCodeToIncludeBinaryArtifact
                            <script type="module">
                                let test = document.getElementById("test")
                                try {
                                    const jsModule = $jsCodeToGetModuleWithBoxFunction;
                                    try { 
                                      if (jsModule.box.length) {
                                        box(jsModule.makeEmptyContinuation());
                                      } else {
                                        jsModule.box();
                                      }
                                    } catch(e) { alert(e) }
                                    
                                    test.style.backgroundColor = "#0f0";
                                    test.textContent = "OK"
                                } catch(e) {
                                    test.style.backgroundColor = "#f00";
                                    test.textContent = "NOT OK"
                                    throw e;
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
            )
            // To have access to the content of original files from a browser's DevTools
            testServices.moduleStructure.modules
                .flatMap { it.files }
                .forEach { File(outputDir, it.name).writeText(it.originalContent) }
        }

        val exception = try {
            val result = runSavedCode(outputDir)
            val debuggerSteps = FrameParser(result).parse().mapNotNull { frame ->
                val (pausedLocation, functionLocation) = sourceMaps.firstNotNullOfOrNull { map ->
                    val functionLocation = map.findSegmentForTheGeneratedLocation(
                        frame.currentFunctionStartLocation.line,
                        frame.currentFunctionStartLocation.column
                    )

                    if (functionLocation?.isIgnored == true) return@firstNotNullOfOrNull null

                    val pausedLocation = map.findSegmentForTheGeneratedLocation(frame.pausedLocation.line, frame.pausedLocation.column)
                        ?: return@firstNotNullOfOrNull null

                    pausedLocation to functionLocation
                } ?: return@mapNotNull null

                ProcessedStep(
                    pausedLocation.sourceFileName ?: compiledFileName,
                    functionLocation?.name ?: frame.functionName,
                    Location(
                        pausedLocation.sourceLineNumber.takeIf { it >= 0 } ?: frame.pausedLocation.line,
                        pausedLocation.sourceColumnNumber.takeIf { it >= 0 } ?: frame.pausedLocation.column
                    )
                )
            }

            val groupedByLinesSteppingTestLoggedData = buildList {
                val dummy = ProcessedStep("DUMMY", "DUMMY", Location(-1, -1))
                var lastStep = dummy
                var columns = mutableListOf<Int>()

                for (step in debuggerSteps.plus(lastStep)) {
                    if (!preserveSteppingOnTheSamePlace && lastStep == step) continue

                    if ((!includeColumnInformation && lastStep != dummy) || (!lastStep.isOnTheSameLineAs(step) && columns.isNotEmpty())) {
                        val (fileName, functionName, location) = lastStep
                        val lineNumber = location.line + 1
                        val aggregatedColumns = runIf(includeColumnInformation) { " (${columns.joinToString(", ")})" }.orEmpty()
                        val formatedSteppingExpectation = formatAsSteppingTestExpectation(fileName, lineNumber, functionName, false)
                        push(SteppingTestLoggedData(lineNumber, false, formatedSteppingExpectation + aggregatedColumns))
                        columns = mutableListOf()
                    }

                    columns.push(step.location.column)
                    lastStep = step
                }
            }

            checkSteppingTestResult(
                frontendKind = testServices.defaultsProvider.frontendKind,
                testServices.defaultsProvider.targetBackend!!,
                originalFile,
                groupedByLinesSteppingTestLoggedData,
                testServices.defaultDirectives
            )

            null
        } catch (e: Throwable) {
            e
        }

        processExceptions(listOfNotNull(exception))

    }

    private fun processExceptions(exceptions: List<Throwable>) {
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

    private fun SourceMap.findSegmentForTheGeneratedLocation(lineNumber: Int, columnNumber: Int): SourceMapSegment? {
        return segmentForGeneratedLocation(lineNumber, columnNumber)
            ?.takeIf { it.sourceLineNumber >= 0 && it.sourceFileName != null }
            ?.mapOrNull()
    }


    private data class Location(val line: Int, val column: Int)
    private class Frame(val functionName: String, val pausedLocation: Location, val currentFunctionStartLocation: Location)
    private data class ProcessedStep(val fileName: String, val functionName: String, val location: Location) {
        fun isOnTheSameLineAs(previous: ProcessedStep) =
            previous.fileName == fileName &&
                    previous.functionName == functionName &&
                    previous.location.line == location.line
    }

    private class FrameParser(private val input: String) {
        fun parse(): List<Frame> =
            (parseJson(input) as JsonArray).elements
                .map {
                    val frameObject = it as JsonObject
                    Frame(
                        frameObject.properties["functionName"].asString(),
                        Location(frameObject.properties["line"].asInt(), frameObject.properties["column"].asInt()),
                        Location(
                            frameObject.properties["functionStartLine"].asInt(),
                            frameObject.properties["functionStartColumn"].asInt()
                        ),
                    )
                }

        private fun JsonNode?.asInt() = (this as JsonNumber).value.toInt()
        private fun JsonNode?.asString() = (this as JsonString).value
    }
}
