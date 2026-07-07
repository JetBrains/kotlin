/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.model.ArtifactKind
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File

abstract class D8BasedDebugRunner<A : ResultingArtifact.Binary<A>>(
    testServices: TestServices,
    artifactKind: ArtifactKind<A>,
    failureDisablesNextSteps: Boolean = false,
    doNotRunIfThereWerePreviousFailures: Boolean = false,
    private val includeColumnInformation: Boolean = false,
    private val preserveSteppingOnTheSamePlace: Boolean = false,
    private val includeLocalVariableInformation: Boolean = false,
) : BinaryArtifactHandler<A>(testServices, artifactKind, failureDisablesNextSteps, doNotRunIfThereWerePreviousFailures) {
    abstract val debugMode: DebugMode
    abstract val jsCodeToGetModuleWithBoxFunction: String
    abstract val htmlCodeToIncludeBinaryArtifact: String

    abstract fun saveEntryFile(outputDir: File, content: String)
    abstract fun runSavedCode(outputDir: File): String

    protected open fun SourceMapSegment.mapOrNull(): SourceMapSegment? = this

    open fun writeToFilesAndRunTest(outputDir: File, sourceMaps: List<SourceMap>, compiledFileName: String) {
        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

        val jsCodeAddLocationFunction = if (!includeLocalVariableInformation) {
            // language=js
            """
            function addLocation(frame) {
                locations.push({
                   functionName: frame.functionName,
                   line: frame.location.lineNumber,
                   column: frame.location.columnNumber,
                   functionStartLine: frame.functionLocation?.lineNumber,
                   functionStartColumn: frame.functionLocation?.columnNumber
               })
            }
            """ // no trim indent, that would break the trimIndent() of `testFileContent`
        } else {
            // language=js
            """
            function addLocation(frame) {
                // get info on locals from runtime
                let localsInfo = {};
                for (let scope of frame.scopeChain.filter(scope => scope.type === "local")) {
                    try {
                        /// Repeatedly calls `getProperties` on v8 
                        function recurseIntoObjects(objId, maxDepth) {
                            let mainSubobj = {};
                            // base case: stop recursing if we don't care anymore
                            // this avoids excessive/infinite recursing into internal structures like vtables
                            if (maxDepth === 0) {
                                return mainSubobj;
                            }
                            const {result, internalProperties, privateProperties} = getProperties(objId);
                            const mainProps = result;

                            const recurseIntoProps = (subobj, props) => {
                                for(const prop of props) {
                                    // keep diggin'
                                    // NOTE: loose inequality check against null checks exactly and only for null/undefined
                                    if(prop.value?.type === "object" && prop.value.objectId != null) {

                                        subobj[prop.name] = recurseIntoObjects(prop.value.objectId, maxDepth - 1);
                                    }else{
                                        subobj[prop.name] = prop.value;
                                    }
                                }
                            };

                            recurseIntoProps(mainSubobj, mainProps);
                             
                            if(internalProperties !== undefined) {
                                let internalSubobj = {};
                                mainSubobj["__internal__"] = internalSubobj;
                                recurseIntoProps(internalSubobj, internalProperties);
                            }
                            if(privateProperties !== undefined) {
                                let privateSubobj = {};
                                mainSubobj["__private__"] = privateSubobj;
                                recurseIntoProps(privateSubobj, privateProperties);
                            }
                            
                            return mainSubobj;
                        }
                        // 2 layers would be enough to get values of primitives, and types of complex objects
                        // do 3 layers, to get the constructor of JS objects (and its description) as well
                        let obj = recurseIntoObjects(scope.object.objectId, 3);

                        // some locals are undefined, if they're part of the scope, but haven't been initialized yet
                        // filter those out, as we don't typically want to see them (this only works for JS code, as wasm locals cannot be undefined)
                        obj = Object.fromEntries(Object.entries(obj).filter(([name, value]) => value !== undefined));
                        
                        // extract locals, we only need a distinction for 3 things:
                        // - primitives:
                        //   - Wasm: i32, i64, f32, f64
                        //   - JS: number, string, boolean, symbol, bigint, undefined, null
                        // - references: only need the type not the value
                        // - null values: nice to have the type if possible
                        for (const [name, value] of Object.entries(obj)) {
                            const localsEntry = value;

                            // distinguish between wasm objects and JS objects.
                            // for wasm objects, type and value always exist, and are both always objects, whereas for JS objects, the value/type properties can be primitives/strings, and don't have to exist (e.g. for plain objects, which only have their keys as properties)

                            const isWasmObject = "value" in localsEntry &&
                                                    "type" in localsEntry &&
                                                    typeof localsEntry.value === "object" &&
                                                    typeof localsEntry.type === "object";

                            if(isWasmObject) {
                                const { value, type } = localsEntry;

                                const typeAsString = type.value;
                                const hasNestedValue = "value" in value;
                                // easiest case: null values
                                // value.value is actually null here, so just check that
                                if(hasNestedValue && value.value === null) {
                                    localsInfo[name] = { kind: "LocalNullValue", type: typeAsString };
                                }else if(hasNestedValue && typeof value.value !== "object") {
                                    // primitives have "direct" (meaning non-null, non-object) values
                                    localsInfo[name] = { kind: "LocalPrimitive", type: typeAsString, value: value.value };
                                }else{
                                    // references are truncated, because they point to deeper structs
                                    localsInfo[name] = { kind: "LocalReference", type: typeAsString };
                                }
                            }else{ // JS object
                                function tryGetTypeOfJsObjectFromConstructor(localsEntry) {
                                    let type = "object";

                                    let internalPrototype = localsEntry?.__internal__?.["[[Prototype]]"];

                                    // TODO(KT-83244): get more info on JS object type from constructor or similar.
                                    //                 This is a rough and hacky approximation for now
                                    if(internalPrototype !== undefined && Object.prototype.hasOwnProperty.call(internalPrototype, "constructor")) {
                                        // use hasOwnProperty to eliminate inherited constructor from Object or similar, which we don't care about, as this is the *description* of an object, not the actual object itself. So the description needs to have a constructor property directly.

                                        // __internal__ syntax comes from parsing inside `recurseIntoObjects`
                                        const constructor = internalPrototype["constructor"];

                                        // parse approximation for the type from the constructor description
                                        // find first " {", and take the preceding string as type
                                        const typeEndIdx = constructor.description.indexOf(" {");
                                        if(typeEndIdx !== -1) {
                                            type = constructor.description.slice(0, typeEndIdx);
                                            if(type.startsWith("function Object")) {
                                                type = "object";
                                            }
                                        }
                                    }
                                    return type;
                                }

                                // objects don't have a type/value pair, so check or that
                                if(!("type" in localsEntry) && !("value" in localsEntry)) {
                                    localsInfo[name] = { kind: "LocalReference", type: tryGetTypeOfJsObjectFromConstructor(localsEntry) };
                                }else if("value" in localsEntry && localsEntry.value === null) {
                                    // null never has a user-defined type, "object" is hardcoded here when no explicit type is supplied
                                    localsInfo[name] = { kind: "LocalNullValue", type: localsEntry.type || "object" };
                                }else if("type" in localsEntry && localsEntry.type === "function") {
                                    // its a class or local fun definition, we don't track those right now
                                }else {
                                    let type = localsEntry.type
                                    if(type === undefined) {
                                        type = typeof localsEntry.value;
                                    }

                                    localsInfo[name] = { kind: "LocalPrimitive", type: type, value: localsEntry.value };
                                }
                            }
                        }


                    } catch (error) {
                        if(!($jsLocalsErrorKey in localsInfo))
                            localsInfo[$jsLocalsErrorKey] = [];

                        localsInfo[$jsLocalsErrorKey].push({
                            message: `Error retrieving local variables: ${'$'}{error.message}`,
                            stack: error.stack,
                            scopeInfo: scope
                        })
                    }
                }

                // NOTE: only difference in root level JSON structure is presence/absence of locals key
                locations.push({
                   functionName: frame.functionName,
                   line: frame.location.lineNumber,
                   column: frame.location.columnNumber,
                   functionStartLine: frame.functionLocation?.lineNumber,
                   functionStartColumn: frame.functionLocation?.columnNumber,
                   locals: localsInfo
               })
            }
            """
        }

        // language=js
        val testFileContent = """
            let messageId = 0;
            const locations = [];
            $jsCodeAddLocationFunction
            function sendMessage(message) { send(JSON.stringify(Object.assign(message, { id: messageId++ }))) } 
            function sendMessageExpectResponse(message) { 
                expectingMsgResponse.id = messageId;
                // response is null iff there is a message in flight, i.e., the response hasn't arrived yet
                expectingMsgResponse.response = null;
                sendMessage(message);
                // d8 calls receive for us synchronously here, receive deposits the response in expectingMsgResponse
                return expectingMsgResponse.response;
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
            // sendMessageExpectResponse sets the id, such that the receive function can identify the relevant message and store its result
            let expectingMsgResponse = {
                id : null,
                response : null
            };
            function getProperties(objectId) {
                return sendMessageExpectResponse({ method: 'Runtime.getProperties', params: { 
                    objectId : objectId,
                    ownProperties: true
                }});
            }
            globalThis.receive = function(message) {
                message = JSON.parse(message);
                if (message.method === "Debugger.paused") {
                    addLocation(message.params.callFrames[0]);
                    stepInto();
                }else if (message.id === expectingMsgResponse.id) {
                    expectingMsgResponse.id = null;
                    expectingMsgResponse.response = message.result;
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
                val [pausedLocation, functionLocation] = sourceMaps.firstNotNullOfOrNull { map ->
                    val functionLocation = map.findSegmentForTheGeneratedLocation(
                        frame.currentFunctionStartLocation.line,
                        frame.currentFunctionStartLocation.column
                    )

                    if (functionLocation?.isIgnored == true) return@firstNotNullOfOrNull null

                    val pausedLocation = map.findSegmentForTheGeneratedLocation(frame.pausedLocation.line, frame.pausedLocation.column)
                        ?: return@firstNotNullOfOrNull null

                    if (pausedLocation.isIgnored) return@firstNotNullOfOrNull null

                    pausedLocation to functionLocation
                } ?: return@mapNotNull null

                ProcessedStep(
                    pausedLocation.sourceFileName ?: compiledFileName,
                    functionLocation?.name ?: frame.functionName,
                    Location(
                        pausedLocation.sourceLineNumber.takeIf { it >= 0 } ?: frame.pausedLocation.line,
                        pausedLocation.sourceColumnNumber.takeIf { it >= 0 } ?: frame.pausedLocation.column
                    ),
                    frame.locals
                )
            }

            val groupedByLinesSteppingTestLoggedData = buildList {
                val dummy = ProcessedStep("DUMMY", "DUMMY", Location(-1, -1))
                var lastStep = dummy
                var columns = mutableListOf<Int>()

                for (step in debuggerSteps.plus(lastStep)) {
                    if (!preserveSteppingOnTheSamePlace && lastStep == step) continue

                    if ((!includeColumnInformation && lastStep != dummy) || (!lastStep.isOnTheSameLineAs(step) && columns.isNotEmpty())) {
                        val (fileName, functionName, location, locals) = lastStep
                        val lineNumber = location.line + 1
                        val aggregatedColumns = runIf(includeColumnInformation) { " (${columns.joinToString(", ")})" }.orEmpty()
                        val formatedSteppingExpectation =
                            formatAsSteppingTestExpectation(fileName, lineNumber, functionName, false, locals)
                        push(SteppingTestLoggedData(lineNumber, false, formatedSteppingExpectation + aggregatedColumns))
                        columns = mutableListOf()
                    }

                    columns.push(step.location.column)
                    lastStep = step
                }
            }

            checkSteppingTestResult(
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
    private class Frame(
        val functionName: String,
        val pausedLocation: Location,
        val currentFunctionStartLocation: Location,
        val locals: List<LocalVariableRecord>? = null,
    )

    private data class ProcessedStep(
        val fileName: String,
        val functionName: String,
        val location: Location,
        val locals: List<LocalVariableRecord>? = null,
    ) {
        fun isOnTheSameLineAs(previous: ProcessedStep) =
            previous.fileName == fileName &&
                    previous.functionName == functionName &&
                    previous.location.line == location.line
    }

    private inner class FrameParser(private val input: String) {
        fun JsonObject.toLocalsRecord(name: String): LocalVariableRecord {
            val type = (properties["type"] as JsonString).value
            val value: LocalValue = when ((properties["kind"] as JsonString).value) {
                "LocalNullValue" -> LocalNullValue // properties["type"] is still set for nulls, though it might be "nullref" if its opaque
                "LocalPrimitive" -> LocalPrimitive(properties["value"].toString(), type)
                "LocalReference" -> LocalReference(properties["id"].toString(), type)
                else -> shouldNotBeCalled("Internal test infrastructure failure: invalid local variable kind: ${properties["kind"]}")
            }
            return LocalVariableRecord(
                name,
                type,
                value
            )
        }

        fun parse(): List<Frame> =
            (parseJson(input) as JsonArray).elements
                .map { it as JsonObject }
                .map { frameObject ->
                    // Extract locals
                    val locals = if (!includeLocalVariableInformation) {
                        null
                    } else {
                        // Parse from properties
                        val localsProps = (frameObject.properties["locals"] as JsonObject).properties
                        if (localsProps.containsKey(jsLocalsErrorKey))
                            shouldNotBeCalled("Internal test infrastructure failure: unable to retrieve local variables for stack frame object, error: ${localsProps[jsLocalsErrorKey]}")

                        // All var names that D8 gives us start with a $, so the rest of the prefix comes after that
                        val internalLocalsPrefix = "$" + run {
                            // Ensure the variable prefix is up-to-date, by getting it from the compiler config
                            // Get any module, as the local variable prefix will be the same across all of them
                            val module = this@D8BasedDebugRunner.testServices.moduleStructure.modules.first()
                            val config = this@D8BasedDebugRunner.testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
                            config[WasmConfigurationKeys.WASM_INTERNAL_LOCAL_VARIABLE_PREFIX]
                        }

                        // Filter out internal locals:
                        // - they create extra noise in EXPECTATIONS blocks
                        // - if anything internal mismatches, but there's no observable behavior change, that likely shouldn't trigger a test failure
                        localsProps.filter { [name, _] -> !name.startsWith(internalLocalsPrefix) }
                            .map { [name, info] -> (info as JsonObject).toLocalsRecord(name) }
                    }
                    Frame(
                        frameObject.properties["functionName"].asString(),
                        Location(frameObject.properties["line"].asInt(), frameObject.properties["column"].asInt()),
                        Location(
                            frameObject.properties["functionStartLine"].asInt(),
                            frameObject.properties["functionStartColumn"].asInt()
                        ),
                        locals
                    )
                }

        private fun JsonNode?.asInt() = (this as JsonNumber).value.toInt()
        private fun JsonNode?.asString() = (this as JsonString).value
    }

    companion object {
        /// use the key "{error}" inside the `locals` object, to constrain its scope, but not possibly confuse it with actual locals, as the names of those cannot contain "{"
        const val jsLocalsErrorKey = """{error}"""
    }
}
