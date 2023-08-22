/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
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

        val testFileContent = """
            let messageId = 0;
            const locations = [];
            function addLocation(frame) {
                locations.push({ 
                    functionName: frame.functionName,
                    line: frame.location.lineNumber,
                    column: frame.location.columnNumber
                })
            }
            function sendMessage(message) { 
                if (typeof send !== "function") return
                send(JSON.stringify(Object.assign(message, { id: messageId++ })))
             } 
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
            const box = jsModule.default.box;
            
            enableDebugger();
            setBreakpoint(box);
            box();
            disableDebugger();
            print(JSON.stringify(locations))
        """.trimIndent()

        fun writeToFilesAndRunTest(mode: String, res: WasmCompilerResult) {
            val sourceMap = res.parsedSourceMaps

            val dir = File(outputDirBase, mode)
            dir.mkdirs()

            writeCompilationResult(res, dir, "index")
            File(dir, "test.mjs").writeText(testFileContent)

            if (debugMode >= DebugMode.DEBUG) {
                // Save test files inside the result directory to debug the source maps by browser
                testServices.moduleStructure.modules.forEach { testModule ->
                    testModule.files.forEach { testFile ->
                        File(dir, testFile.name).writeText(testFile.originalContent)
                    }
                }
                // Add index.html to load script with the source maps
                writeIndexHtmlForManualDebug(dir)
            }

            val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(dir)

            val exception = try {
                val result = WasmVM.V8.run(
                    entryMjs = "./${collectedJsArtifacts.entryPath}",
                    jsFiles = jsFilePaths,
                    workingDirectory = dir,
                    toolArgs = listOf("--enable-inspector", "--allow-natives-syntax")
                )
                val parsedLocations = FrameParser(result).parse().mapNotNull {
                    val functionName = it.functionName
                    val pausedLocation = sourceMap.segmentForGeneratedLocation(it.location.line, it.location.column)
                    val testFileName = pausedLocation?.sourceFileName

                    if (testFileName == null || pausedLocation.sourceLineNumber < 0) {
                        null
                    } else {
                        val lineNumber = pausedLocation.sourceLineNumber + 1
                        SteppingTestLoggedData(
                            lineNumber,
                            false,
                            formatAsSteppingTestExpectation(testFileName, lineNumber, functionName, false)
                        )
                    }
                }

                checkSteppingTestResult(
                    frontendKind = mainModule.frontendKind,
                    mainModule.targetBackend ?: TargetBackend.WASM,
                    originalFile,
                    parsedLocations
                )

                null
            } catch (e: Throwable) { e }

            processExceptions(listOfNotNull(exception))
        }

        writeToFilesAndRunTest("dev", artifacts.compilerResult)
        writeToFilesAndRunTest("dce", artifacts.compilerResultWithDCE)
    }

    private fun writeIndexHtmlForManualDebug(dir: File) {
        File(dir, "index.html").writeText(
            """
                <!DOCTYPE html>
                <html lang="en">
                <body>
                    <span id="test">UNKNOWN</span>
                    <script type="module">
                        let test = document.getElementById("test")
                        try {
                            await import("./test.mjs");
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
    }

    private val WasmCompilerResult.parsedSourceMaps: SourceMap
        get() = when (val parseResult = SourceMapParser.parse(sourceMap ?: error("Expect to have source maps for stepping test"))) {
            is SourceMapSuccess -> parseResult.value
            is SourceMapError -> error(parseResult.message)
        }

    private class Location(val line: Int, val column: Int)
    private class Frame(val functionName: String, val location: Location)
    private class FrameParser(private val input: String) {
        fun parse(): List<Frame> =
            (parseJson(input) as JsonArray).elements
                .map {
                    val frameObject = it as JsonObject
                    Frame(
                        frameObject.properties["functionName"].asString(),
                        Location(frameObject.properties["line"].asInt(), frameObject.properties["column"].asInt())
                    )
                }

        private fun JsonNode?.asInt() = (this as JsonNumber).value.toInt()
        private fun JsonNode?.asString() = (this as JsonString).value
    }
}