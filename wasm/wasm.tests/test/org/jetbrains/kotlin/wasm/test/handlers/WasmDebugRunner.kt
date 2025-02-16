/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.defaultDirectives
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.SteppingTestLoggedData
import org.jetbrains.kotlin.test.utils.checkSteppingTestResult
import org.jetbrains.kotlin.test.utils.formatAsSteppingTestExpectation
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File

class WasmDebugRunner(testServices: TestServices) : AbstractWasmArtifactsCollector(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            saveAndRunWasmCode()
        }
    }

    private fun saveAndRunWasmCode() {
        val artifacts = modulesToArtifact.values.single()
        val outputDirBase = testServices.getWasmTestOutputDirectory()
        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
        val mainModule = WasmEnvironmentConfigurator.getMainModule(testServices)
        val collectedJsArtifacts = collectJsArtifacts(originalFile)
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

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
            
            const jsModule = await import('./index.mjs');
            const box = jsModule.box;
            
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

        fun writeToFilesAndRunTest(mode: String, res: WasmCompilerResult) {
            val compiledFileBase = "index"
            val sourceMap = res.parsedSourceMaps

            val dir = File(outputDirBase, mode)
            dir.mkdirs()

            writeCompilationResult(res, dir, compiledFileBase, true)
            File(dir, "test.mjs").writeText(testFileContent)

            val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(dir)

            if (debugMode >= DebugMode.DEBUG) {
                File(dir, "index.html").writeText(
                    // language=html
                    """
                        <!DOCTYPE html>
                        <html lang="en">
                        <body>
                            <span id="test">UNKNOWN</span>
                            <script type="module">
                                let test = document.getElementById("test")
                                try {
                                    const jsModule = await import('./index.mjs');
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
                    .forEach { File(dir, it.name).writeText(it.originalContent) }
            }

            val exception = try {
                val result = WasmVM.V8.run(
                    entryFile = "./${collectedJsArtifacts.entryPath}",
                    jsFiles = jsFilePaths,
                    workingDirectory = dir,
                    toolArgs = listOf("--enable-inspector", "--allow-natives-syntax")
                )
                val debuggerSteps = FrameParser(result).parse().mapNotNull { frame ->
                    val functionLocation = sourceMap
                        .findSegmentForTheGeneratedLocation(
                            frame.currentFunctionStartLocation.line,
                            frame.currentFunctionStartLocation.column
                        )
                        ?.takeIf { it.sourceLineNumber >= 0 }

                    if (functionLocation?.isIgnored == true) return@mapNotNull null

                    val pausedLocation = sourceMap
                        .findSegmentForTheGeneratedLocation(frame.pausedLocation.line, frame.pausedLocation.column)
                        ?: return@mapNotNull null

                    ProcessedStep(
                        pausedLocation.sourceFileName ?: "$compiledFileBase.wasm",
                        frame.functionName,
                        Location(
                            pausedLocation.sourceLineNumber.takeIf { it >= 0 } ?: frame.pausedLocation.line,
                            pausedLocation.sourceColumnNumber.takeIf { it >= 0 } ?: frame.pausedLocation.column
                        )
                    )
                }

                val groupedByLinesSteppingTestLoggedData = buildList<SteppingTestLoggedData> {
                    var lastStep = ProcessedStep("DUMMY", "DUMMY", Location(-1, -1))
                    var columns = mutableListOf<Int>()

                    for (step in debuggerSteps.plus(lastStep)) {
                        if (lastStep == step) {
                            continue
                        }

                        if (!lastStep.isOnTheSameLineAs(step) && columns.isNotEmpty()) {
                            val (fileName, functionName, location) = lastStep
                            val lineNumber = location.line + 1
                            val aggregatedColumns = " (${columns.joinToString(", ")})"
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
                    testServices.defaultsProvider.targetBackend ?: TargetBackend.WASM,
                    originalFile,
                    groupedByLinesSteppingTestLoggedData,
                    testServices.defaultDirectives
                )

                null
            } catch (e: Throwable) { e }

            processExceptions(listOfNotNull(exception))
        }

        writeToFilesAndRunTest("dev", artifacts.compilerResult)
        writeToFilesAndRunTest("dce", artifacts.compilerResultWithDCE)
    }

    private fun SourceMap.findSegmentForTheGeneratedLocation(lineNumber: Int, columnNumber: Int): SourceMapSegment? {
        val group = groups.getOrNull(lineNumber)?.takeIf { it.segments.isNotEmpty() } ?: return null
        return group.segments
            .indexOfLast { columnNumber >= it.generatedColumnNumber }
            .takeIf { it >= 0 }
            ?.let(group.segments::get)
    }

    private val WasmCompilerResult.parsedSourceMaps: SourceMap
        get() = when (val parseResult = SourceMapParser.parse(debugInformation?.sourceMapForBinary ?: error("Expect to have source maps for stepping test"))) {
            is SourceMapSuccess -> parseResult.value
            is SourceMapError -> error(parseResult.message)
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
