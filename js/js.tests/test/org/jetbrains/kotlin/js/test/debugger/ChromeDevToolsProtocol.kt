/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.debugger

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Represents an incoming [Chrome DevTools protocol](https://chromedevtools.github.io/devtools-protocol/) message,
 * which can be:
 * - an event
 * - an method invocation result
 * - an error, if a method invocation failed.
 */
sealed class CDPResponse {

    /**
     * Indicates that a CDP method was invoked successfully.
     *
     * [id] is a message id with which the method was invoked, [result] is the result of the invocation.
     */
    class MethodInvocationResult(val id: Int, val result: CDPMethodInvocationResult) : CDPResponse()

    /**
     * Indicates that a CDP method invocation failed.
     *
     * [id] is a message id with which the method was invoked, [error] is the failure information.
     */
    class Error(val id: Int, val error: CDPError) : CDPResponse()

    /**
     * Indicates an event received from CDP. Events are not tied to any method invocations.
     */
    class Event(val event: CDPEvent) : CDPResponse()
}

private val json = Json {
    ignoreUnknownKeys = true
}

/**
 * Decodes an instance of [CDPResponse] from JSON-encoded [message].
 *
 * [serializerForMessageId] is used for retrieving additional information about how to decode the response for the given message id.
 */
fun decodeCDPResponse(
    message: String,
    serializerForMessageId: (Int) -> CDPMethodCallEncodingInfo
): CDPResponse {
    val jsonElement = json.parseToJsonElement(message)
    return when (val id = jsonElement.jsonObject["id"]?.jsonPrimitive?.int) {
        null -> CDPResponse.Event(decodeCDPEvent(jsonElement))
        else -> {
            val serializer = when (val encodingInfo = serializerForMessageId(id)) {
                is CDPMethodCallEncodingInfoImpl -> encodingInfo.serializer
                is CDPMethodCallEncodingInfoPlainText -> null
            }
            val result = jsonElement.jsonObject["result"]
            val error = jsonElement.jsonObject["error"]
            if (result != null) {
                CDPResponse.MethodInvocationResult(
                    id,
                    serializer?.let { json.decodeFromJsonElement(it, result) } ?: CDPMethodInvocationResultPlainText(result.toString())
                )
            } else if (error != null) {
                CDPResponse.Error(id, json.decodeFromJsonElement(error))
            } else {
                error("Missing 'result' or 'error' properties in JSON")
            }
        }
    }
}

/**
 * An opaque object that contains information about how to decode a method invocation result.
 *
 * Depending on the method, the type of the result may be different. So, when invoking a method,
 * we save the information about how to decode its result, so that when the result is received,
 * we could decode a value of the correct type.
 */
sealed interface CDPMethodCallEncodingInfo

private class CDPMethodCallEncodingInfoImpl(
    val serializer: DeserializationStrategy<out CDPMethodInvocationResult>
) : CDPMethodCallEncodingInfo

/**
 * Indicates that the result of the method call does not need any deserialization.
 */
object CDPMethodCallEncodingInfoPlainText : CDPMethodCallEncodingInfo

/**
 * Returns the JSON message for invoking the method, as well as the information about how to decode its result.
 */
private inline fun <reified Response : CDPMethodInvocationResult, reified Params : CDPRequestParams> encodeCDPMethodCall(
    messageId: Int,
    methodName: String,
    params: Params?
): Pair<String, CDPMethodCallEncodingInfo> {
    val request = CDPRequest(messageId, methodName, params)
    return json.encodeToString(request) to
            CDPMethodCallEncodingInfoImpl(json.serializersModule.serializer<Response>())
}

/**
 * A superclass for each kind of CDP method invocation result.
 */
@Serializable
sealed class CDPMethodInvocationResult

/**
 * A special kind of method invocation result that indicates that the response contents should be discarded.
 */
@Serializable
object CDPMethodInvocationResultUnit : CDPMethodInvocationResult()

/**
 * A special kind of method invocation result that indicates that the response contents should not be decoded from JSON,
 * and instead should be returned as-is.
 */
class CDPMethodInvocationResultPlainText(val string: String) : CDPMethodInvocationResult()

/**
 * A superclass for each kind of CDP method invocation parameters.
 */
@Serializable
sealed class CDPRequestParams

/**
 * A special kind of method invocation parameters that indicates that the method accepts no parameters.
 */
@Serializable
internal object CDPRequestParamsUnit : CDPRequestParams()

/**
 * A representation of a [Chrome DevTools protocol](https://chromedevtools.github.io/devtools-protocol/) request.
 */
@Serializable
class CDPRequest<Params : CDPRequestParams>(
    val id: Int,
    val method: String,
    val params: Params? = null
)

/**
 * A representation of a [Chrome DevTools protocol](https://chromedevtools.github.io/devtools-protocol/) error.
 */
@Serializable
class CDPError private constructor(val code: Int, val message: String? = null)

/**
 * A superclass for each kind of CDP event.
 */
@Serializable
sealed class CDPEvent

/**
 * When we encounter a CDP event whose name we don't know, we return an instance of this class.
 */
class UnknownCDPEvent(val name: String) : CDPEvent()

private fun decodeCDPEvent(element: JsonElement): CDPEvent {
    val method = element.jsonObject["method"]!!.jsonPrimitive.content
    val params = element.jsonObject["params"] ?: error("missing params")
    return when (method) {
        "Debugger.breakpointResolved" -> json.decodeFromJsonElement(Debugger.Event.BreakpointResolved.serializer(), params)
        "Debugger.paused" -> json.decodeFromJsonElement(Debugger.Event.Paused.serializer(), params)
        "Debugger.resumed" -> json.decodeFromJsonElement(Debugger.Event.Resumed.serializer(), params)
        "Debugger.scriptFailedToParse" -> json.decodeFromJsonElement(Debugger.Event.ScriptFailedToParse.serializer(), params)
        "Debugger.scriptParsed" -> json.decodeFromJsonElement(Debugger.Event.ScriptParsed.serializer(), params)
        "Runtime.executionContextCreated" -> json.decodeFromJsonElement(Runtime.Event.ExecutionContextCreated.serializer(), params)
        "Runtime.executionContextDestroyed" -> json.decodeFromJsonElement(Runtime.Event.ExecutionContextDestroyed.serializer(), params)
        else -> UnknownCDPEvent(method)
    }
}

/**
 * Something capable of invoking a [Chrome DevTools protocol](https://chromedevtools.github.io/devtools-protocol/) method and returning
 * the result of the invocation.
 */
interface CDPRequestEvaluator {

    /**
     * @param encodeMethodCallWithMessageId passed a message id, expected to return the JSON-encoded CDP message
     * and some information about how to decode the response.
     */
    suspend fun genericEvaluateRequest(
        encodeMethodCallWithMessageId: (Int) -> Pair<String, CDPMethodCallEncodingInfo>
    ): CDPMethodInvocationResult
}

private suspend inline fun <reified T : CDPMethodInvocationResult> CDPRequestEvaluator.evaluateRequest(
    noinline body: (Int) -> Pair<String, CDPMethodCallEncodingInfo>
) = genericEvaluateRequest(body) as T

/**
 * The [`Runtime` domain](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/) of Chrome DevTools protocol.
 */
class Runtime(private val requestEvaluator: CDPRequestEvaluator) {

    /**
     * Enables reporting of execution contexts creation by means of [Runtime.Event.ExecutionContextCreated] event.
     * When the reporting gets enabled the event will be sent immediately for each existing execution context.
     *
     * See [Runtime.enable](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#method-enable)
     */
    suspend fun enable() {
        requestEvaluator.evaluateRequest<CDPMethodInvocationResultUnit> { messageId ->
            encodeCDPMethodCall<CDPMethodInvocationResultUnit, CDPRequestParamsUnit>(messageId, "Runtime.enable", null)
        }
    }

    /**
     * Tells inspected instance to run if it was waiting for debugger to attach.
     *
     * See [Runtime.runIfWaitingForDebugger](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#method-runIfWaitingForDebugger)
     */
    suspend fun runIfWaitingForDebugger() {
        requestEvaluator.evaluateRequest<CDPMethodInvocationResultUnit> { messageId ->
            encodeCDPMethodCall<CDPMethodInvocationResultUnit, CDPRequestParamsUnit>(messageId, "Runtime.runIfWaitingForDebugger", null)
        }
    }

    @Serializable
    class EvaluationResult private constructor(
        /**
         * Evaluation result.
         */
        val result: RemoteObject,
        /**
         * Exception details.
         */
        val exceptionDetails: ExceptionDetails? = null
    ) : CDPMethodInvocationResult()

    @Serializable
    class EvaluateRequestParams(val expression: String, val contextId: ExecutionContextId? = null) : CDPRequestParams()

    /**
     * Evaluates expression on global object.
     *
     * See [Runtime.evaluate](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#method-evaluate)
     *
     * @param expression Expression to evaluate.
     * @param contextId Specifies in which execution context to perform evaluation.
     * If the parameter is omitted the evaluation will be performed in the context of the inspected page.
     */
    suspend fun evaluate(expression: String, contextId: ExecutionContextId? = null) =
        requestEvaluator.evaluateRequest<EvaluationResult> { messageId ->
            encodeCDPMethodCall<EvaluationResult, EvaluateRequestParams>(
                messageId,
                "Runtime.evaluate",
                EvaluateRequestParams(expression, contextId)
            )
        }

    /**
     * Unique script identifier.
     *
     * See [Runtime.ScriptId](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-ScriptId)
     */
    @Serializable
    @JvmInline
    value class ScriptId(val value: String)

    /**
     * Id of an execution context.
     *
     * See [Runtime.ExecutionContextId](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-ExecutionContextId)
     */
    @Serializable
    @JvmInline
    value class ExecutionContextId(val value: Int)

    /**
     * Unique object identifier.
     *
     * See [Runtime.RemoteObjectId](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-RemoteObjectId)
     */
    @Serializable
    @JvmInline
    value class RemoteObjectId(val value: String)

    /**
     * Object type.
     */
    @Serializable
    enum class ValueType {
        @SerialName("object")
        OBJECT,

        @SerialName("function")
        FUNCTION,

        @SerialName("undefined")
        UNDEFINED,

        @SerialName("string")
        STRING,

        @SerialName("number")
        NUMBER,

        @SerialName("boolean")
        BOOLEAN,

        @SerialName("symbol")
        SYMBOL,

        @SerialName("accessor")
        ACCESSOR,

        @SerialName("bigint")
        BIGINT,
    }

    /**
     * Object subtype hint.
     */
    @Serializable
    enum class ObjectSubtype {
        @SerialName("array")
        ARRAY,

        @SerialName("null")
        NULL,

        @SerialName("node")
        NODE,

        @SerialName("regexp")
        REGEXP,

        @SerialName("date")
        DATE,

        @SerialName("map")
        MAP,

        @SerialName("set")
        SET,

        @SerialName("weakmap")
        WEAKMAP,

        @SerialName("weakset")
        WEAKSET,

        @SerialName("iterator")
        ITERATOR,

        @SerialName("generator")
        GENERATOR,

        @SerialName("error")
        ERROR,

        @SerialName("proxy")
        PROXY,

        @SerialName("promise")
        PROMISE,

        @SerialName("typedarray")
        TYPEDARRAY,

        @SerialName("arraybuffer")
        ARRAYBUFFER,

        @SerialName("dataview")
        DATAVIEW,

        @SerialName("webassemblymemory")
        WEBASSEMBLYMEMORY,

        @SerialName("wasmvalue")
        WASMVALUE
    }

    /**
     * Mirror object referencing original JavaScript object.
     *
     * See [Runtime.RemoteObject](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-RemoteObject)
     */
    @Serializable
    class RemoteObject private constructor(
        /**
         * Object type.
         */
        val type: ValueType,
        /**
         * Object subtype hint. Specified for [ValueType.OBJECT] type values only.
         */
        val subtype: ObjectSubtype? = null,
        /**
         * Object class (constructor) name. Specified for [ValueType.OBJECT] type values only.
         */
        val className: String? = null,
        /**
         * Remote object value in case of primitive values or JSON values (if it was requested).
         */
        val value: JsonElement? = null,
        /**
         * String representation of the object.
         */
        val description: String? = null,
        /**
         * Unique object identifier (for non-primitive values).
         */
        val objectId: RemoteObjectId? = null,
    )

    /**
     * Stack entry for runtime errors and assertions.
     *
     * See [Runtime.CallFrame](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-CallFrame)
     */
    @Serializable
    class CallFrame private constructor(
        /**
         * JavaScript function name.
         */
        val functionName: String,
        /**
         * JavaScript script id.
         */
        val scriptId: ScriptId,
        /**
         * JavaScript script name or url.
         */
        val url: String,
        /**
         * JavaScript script line number (0-based).
         */
        val lineNumber: Int,
        /**
         * JavaScript script column number (0-based).
         */
        val columnNumber: Int
    )

    /**
     * Call frames for assertions or error messages.
     *
     * See [Runtime.StackTrace](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-StackTrace)
     */
    @Serializable
    class StackTrace private constructor(
        /**
         * String label of this stack trace. For async traces this may be a name of the function that initiated the async call.
         */
        val description: String? = null,
        /**
         * A list of call frames in the stack trace.
         */
        val callFrames: List<CallFrame>,

        /**
         * Asynchronous JavaScript stack trace that preceded this stack, if available.
         */
        val parent: StackTrace? = null
    )

    /**
     * Detailed information about exception (or error) that was thrown during script compilation or execution.
     *
     * See [Runtime.ExceptionDetails](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-ExceptionDetails)
     */
    @Serializable
    class ExceptionDetails private constructor(
        /**
         * Exception id.
         */
        val exceptionId: Int,
        /**
         * Exception text, which should be used together with exception object when available.
         */
        val text: String,
        /**
         * Line number of the exception location (0-based).
         */
        val lineNumber: Int,
        /**
         * Column number of the exception location (0-based).
         */
        val columnNumber: Int,
        /**
         * Script ID of the exception location.
         */
        val scriptId: ScriptId? = null,
        /**
         * URL of the exception location, to be used when the script was not reported.
         */
        val url: String? = null,
        /**
         * JavaScript stack trace if available.
         */
        val stackTrace: StackTrace? = null,
        /**
         * Exception object if available.
         */
        val exception: RemoteObject? = null,
        /**
         * Identifier of the context where exception happened.
         */
        val executionContextId: ExecutionContextId? = null,
    )

    /**
     * Description of an isolated world.
     *
     * See [Runtime.ExecutionContextDescription](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#type-ExecutionContextDescription)
     */
    @Serializable
    class ExecutionContextDescription(
        /**
         * Unique id of the execution context. It can be used to specify in which execution context script evaluation should be performed.
         */
        val id: ExecutionContextId,
        /**
         * Execution context origin.
         */
        val origin: String,
        /**
         * Human readable name describing given context.
         */
        val name: String,
    )

    /**
     * An event in the `Runtime` CDP domain.
     */
    @Serializable
    sealed class Event : CDPEvent() {
        /**
         * Issued when new execution context is created.
         *
         * See [Runtime.executionContextCreated](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#event-executionContextCreated)
         */
        @Serializable
        class ExecutionContextCreated private constructor(
            /**
             * A newly created execution context.
             */
            val context: ExecutionContextDescription,
        ) : Event()

        /**
         * Issued when execution context is destroyed.
         *
         * See [Runtime.executionContextDestroyed](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/#event-executionContextDestroyed)
         */
        @Serializable
        class ExecutionContextDestroyed private constructor(
            /**
             * Id of the destroyed context
             */
            val executionContextId: ExecutionContextId
        ) : Event()
    }
}

/**
 * The [`Debugger` domain](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/) of Chrome DevTools protocol.
 */
class Debugger(private val requestEvaluator: CDPRequestEvaluator) {

    /**
     * Enables the debugger.
     *
     * See [Debugger.enable](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#method-enable)
     */
    suspend fun enable() {
        requestEvaluator.evaluateRequest<CDPMethodInvocationResultUnit> { messageId ->
            encodeCDPMethodCall<CDPMethodInvocationResultUnit, CDPRequestParamsUnit>(messageId, "Debugger.enable", null)
        }
    }

    @Serializable
    class ResumeRequestParams private constructor(val terminateOnResume: Boolean = false) : CDPRequestParams()

    /**
     * Resumes JavaScript execution.
     *
     * See [Debugger.resume](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#method-resume)
     */
    suspend fun resume() {
        requestEvaluator.evaluateRequest<CDPMethodInvocationResultUnit> { messageId ->
            encodeCDPMethodCall<CDPMethodInvocationResultUnit, ResumeRequestParams>(messageId, "Debugger.resume", null)
        }
    }

    @Serializable
    class SetBreakpointByUrlResult private constructor(
        /**
         * Id of the created breakpoint for further reference.
         */
        val breakpointId: BreakpointId,

        /**
         * List of the locations this breakpoint resolved into upon addition.
         */
        val locations: List<Location>
    ) : CDPMethodInvocationResult()

    @Serializable
    private class SetBreakpointByUrlRequestParams(
        val lineNumber: Int,
        val url: String,
        val scriptHash: String? = null,
        val columnNumber: Int? = null,
        val condition: String? = null,
    ) : CDPRequestParams()

    /**
     * Sets JavaScript breakpoint at given location specified either by URL or URL regex.
     * Once this command is issued, all existing parsed scripts will have breakpoints resolved and returned in
     * [SetBreakpointByUrlResult.locations] property.
     * Further matching script parsing will result in subsequent [Debugger.Event.BreakpointResolved] events issued.
     *
     * See [Debugger.setBreakpointByUrl](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#method-setBreakpointByUrl)
     *
     * @param lineNumber Line number to set breakpoint at.
     * @param url URL of the resources to set breakpoint on.
     * @param scriptHash Script hash of the resources to set breakpoint on.
     * @param columnNumber Offset in the line to set breakpoint at.
     * @param condition Expression to use as a breakpoint condition.
     * When specified, debugger will only stop on the breakpoint if this expression evaluates to true.
     */
    suspend fun setBreakpointByUrl(
        lineNumber: Int,
        url: String,
        scriptHash: String? = null,
        columnNumber: Int? = null,
        condition: String? = null
    ) = requestEvaluator.evaluateRequest<SetBreakpointByUrlResult> { messageId ->
        encodeCDPMethodCall<SetBreakpointByUrlResult, SetBreakpointByUrlRequestParams>(
            messageId,
            "Debugger.setBreakpointByUrl",
            SetBreakpointByUrlRequestParams(
                lineNumber,
                url,
                scriptHash,
                columnNumber,
                condition
            )
        )
    }

    @Serializable
    class SetBreakpointResult private constructor(
        /**
         * Id of the created breakpoint for further reference.
         */
        val breakpointId: BreakpointId,

        /**
         * Location this breakpoint resolved into.
         */
        val actualLocation: Location
    ) : CDPMethodInvocationResult()

    @Serializable
    private class SetBreakpointRequestParams(
        val location: Location,
        val condition: String? = null,
    ) : CDPRequestParams()

    /**
     * Sets JavaScript breakpoint at a given location.
     *
     * See [Debugger.setBreakpoint](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#method-setBreakpoint)
     *
     * @param scriptId Script identifier as reported in the [Debugger.Event.ScriptParsed].
     * @param lineNumber Line number in the script (0-based).
     * @param columnNumber Column number in the script (0-based).
     * @param condition Expression to use as a breakpoint condition.
     * When specified, debugger will only stop on the breakpoint if this expression evaluates to true.
     */
    suspend fun setBreakpoint(
        scriptId: Runtime.ScriptId,
        lineNumber: Int,
        columnNumber: Int? = null,
        condition: String? = null
    ) = requestEvaluator.evaluateRequest<SetBreakpointResult> { messageId ->
        encodeCDPMethodCall<SetBreakpointResult, SetBreakpointRequestParams>(
            messageId,
            "Debugger.setBreakpoint",
            SetBreakpointRequestParams(Location(scriptId, lineNumber, columnNumber), condition)
        )
    }

    @Serializable
    private class SetSkipAllPausesRequestParams(val skip: Boolean) : CDPRequestParams()

    /**
     * Makes page not interrupt on any pauses (breakpoint, exception, dom exception etc).
     *
     * See [Debugger.setSkipAllPauses](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#method-setSkipAllPauses)
     *
     * @param skip New value for skip pauses state.
     */
    suspend fun setSkipAllPauses(skip: Boolean) {
        requestEvaluator.evaluateRequest<CDPMethodInvocationResultUnit> { messageId ->
            encodeCDPMethodCall<CDPMethodInvocationResultUnit, SetSkipAllPausesRequestParams>(
                messageId,
                "Debugger.setSkipAllPauses",
                SetSkipAllPausesRequestParams(skip)
            )
        }
    }

    /**
     * Steps into the function call.
     *
     * See [Debugger.stepInto](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#method-stepInto)
     */
    suspend fun stepInto() {
        requestEvaluator.evaluateRequest<CDPMethodInvocationResultUnit> { messageId ->
            encodeCDPMethodCall<CDPMethodInvocationResultUnit, CDPRequestParamsUnit>(messageId, "Debugger.stepInto", null)
        }
    }

    @Serializable
    private class EvaluateOnCallFrameRequestParams(
        val callFrameId: CallFrameId,
        val expression: String,
        val returnByValue: Boolean? = null,
    ) : CDPRequestParams()

    /**
     * Evaluates expression on a given call frame.
     *
     * See [Debugger.evaluateOnCallFrame](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#method-evaluateOnCallFrame)
     *
     * @param callFrameId Call frame identifier to evaluate on.
     * @param expression Expression to evaluate.
     * @param returnByValue Whether the result is expected to be a JSON object that should be sent by value.
     */
    suspend fun evaluateOnCallFrame(
        callFrameId: CallFrameId,
        expression: String,
        returnByValue: Boolean? = null,
    ) = requestEvaluator.evaluateRequest<Runtime.EvaluationResult> { messageId ->
        encodeCDPMethodCall<Runtime.EvaluationResult, EvaluateOnCallFrameRequestParams>(
            messageId,
            "Debugger.evaluateOnCallFrame",
            EvaluateOnCallFrameRequestParams(callFrameId, expression, returnByValue)
        )
    }

    /**
     * Breakpoint identifier.
     *
     * See [Debugger.BreakpointId](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#type-BreakpointId)
     */
    @Serializable
    @JvmInline
    value class BreakpointId(val value: String)

    /**
     * Location in the source code.
     *
     * See [Debugger.Location](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#type-Location)
     */
    @Serializable
    class Location internal constructor(
        /**
         * Script identifier as reported in the [Debugger.Event.ScriptParsed].
         */
        val scriptId: Runtime.ScriptId,
        /**
         * Line number in the script (0-based).
         */
        val lineNumber: Int,
        /**
         * Column number in the script (0-based).
         */
        val columnNumber: Int? = null
    )

    /**
     * Call frame identifier.
     *
     * See [Debugger.CallFrameId](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#type-CallFrameId)
     */
    @Serializable
    @JvmInline
    value class CallFrameId(val value: String)

    /**
     * JavaScript call frame. Array of call frames form the call stack.
     *
     * See [Debugger.CallFrame](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#type-CallFrame)
     */
    @Serializable
    class CallFrame private constructor(
        /**
         * Call frame identifier. This identifier is only valid while the virtual machine is paused.
         */
        val callFrameId: CallFrameId,

        /**
         * Name of the JavaScript function called on this call frame.
         */
        val functionName: String,

        /**
         * Function location in the source code.
         */
        val functionLocation: Location? = null,

        /**
         * Location in the source code.
         */
        val location: Location,

        /**
         * Scope chain for this call frame.
         */
        val scopeChain: List<Scope>,

        /**
         * `this` object for this call frame.
         */
        val `this`: Runtime.RemoteObject,

        /**
         * The value being returned, if the function is at return point.
         */
        val returnValue: Runtime.RemoteObject? = null,
    )

    @Serializable
    class Scope private constructor(

        /**
         * Scope type.
         */
        val type: ScopeType,

        /**
         * Object representing the scope. For [ScopeType.GLOBAL] and [Scopetype.WITH] scopes it represents the actual object;
         * for the rest of the scopes, it is artificial transient object enumerating scope variables as its properties.
         */
        val `object`: Runtime.RemoteObject,

        val name: String? = null,

        /**
         * Location in the source code where scope starts
         */
        val startLocation: Location? = null,

        /**
         * Location in the source code where scope ends
         */
        val endLocation: Location? = null,
    )

    @Serializable
    enum class ScopeType {
        @SerialName("global")
        GLOBAL,

        @SerialName("local")
        LOCAL,

        @SerialName("with")
        WITH,

        @SerialName("closure")
        CLOSURE,

        @SerialName("catch")
        CATCH,

        @SerialName("block")
        BLOCK,

        @SerialName("script")
        SCRIPT,

        @SerialName("eval")
        EVAL,

        @SerialName("module")
        MODULE,

        @SerialName("wasm-expression-stack")
        WASM_EXPRESSION_STACK,
    }

    @Serializable
    enum class PauseReason {

        @SerialName("ambiguous")
        AMBIGUOUS,

        @SerialName("assert")
        ASSERT,

        @SerialName("CSPViolation")
        CSP_VIOLATION,

        @SerialName("debugCommand")
        DEBUG_COMMAND,

        @SerialName("DOM")
        DOM,

        @SerialName("EventListener")
        EVENT_LISTENER,

        @SerialName("exception")
        EXCEPTION,

        @SerialName("instrumentation")
        INSTRUMENTATION,

        @SerialName("OOM")
        OOM,

        @SerialName("other")
        OTHER,

        @SerialName("promiseRejection")
        PROMISE_REJECTION,

        @SerialName("XHR")
        XHR,

        @SerialName("step")
        STEP,

        @SerialName("Break on start")
        BREAK_ON_START,
    }

    /**
     * An event in the `Debugger` CDP domain.
     */
    @Serializable
    sealed class Event : CDPEvent() {

        /**
         * Fired when breakpoint is resolved to an actual script and location.
         *
         * See [Debugger.breakpointResolved](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#event-breakpointResolved)
         */
        @Serializable
        class BreakpointResolved private constructor(
            /**
             * Breakpoint unique identifier.
             */
            val breakpointId: BreakpointId,
            /**
             * Actual breakpoint location.
             */
            val location: Location
        ) : Event()

        /**
         * Fired when the virtual machine stopped on breakpoint or exception or any other stop criteria.
         *
         * See [Debugger.paused](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#event-paused)
         */
        @Serializable
        class Paused private constructor(
            /**
             * Call stack the virtual machine stopped on.
             */
            val callFrames: List<CallFrame>,
            /**
             * Pause reason.
             */
            val reason: PauseReason,

            /**
             * Hit breakpoints IDs
             */
            val hitBreakpoints: List<BreakpointId> = emptyList()
        ) : Event()

        /**
         * Fired when the virtual machine resumed execution.
         *
         * See [Debugger.resumed](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#event-resumed)
         */
        @Serializable
        object Resumed : Event()

        /**
         * Fired when virtual machine fails to parse the script.
         *
         * See [Debugger.scriptFailedToParse](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#event-scriptFailedToParse)
         */
        @Serializable
        class ScriptFailedToParse private constructor(
            /**
             * Identifier of the script parsed.
             */
            val scriptId: Runtime.ScriptId,
            /**
             * URL or name of the script parsed (if any).
             */
            val url: String,
            /**
             * Line offset of the script within the resource with given URL (for script tags).
             */
            val startLine: Int,
            /**
             * Column offset of the script within the resource with given URL.
             */
            val startColumn: Int,
            /**
             * Last line of the script.
             */
            val endLine: Int,
            /**
             * Length of the last line of the script.
             */
            val endColumn: Int,
            /**
             * URL of source map associated with script (if any).
             */
            val sourceMapUrl: String? = null,
        ) : Event()

        /**
         * Fired when virtual machine parses script. This event is also fired for all known and uncollected scripts upon enabling debugger.
         *
         * See [Debugger.scriptParsed](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/#event-scriptParsed)
         */
        @Serializable
        class ScriptParsed private constructor(
            /**
             * Identifier of the script parsed.
             */
            val scriptId: Runtime.ScriptId,
            /**
             * URL or name of the script parsed (if any).
             */
            val url: String,
            /**
             * Line offset of the script within the resource with given URL (for script tags).
             */
            val startLine: Int,
            /**
             * Column offset of the script within the resource with given URL.
             */
            val startColumn: Int,
            /**
             * Last line of the script.
             */
            val endLine: Int,
            /**
             * Length of the last line of the script.
             */
            val endColumn: Int,
            /**
             * URL of source map associated with script (if any).
             */
            val sourceMapUrl: String? = null,
        ) : Event()
    }
}
