/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.ExternalInterfaceType

/**
 * A wrapper for an exception thrown by a JavaScript code.
 * All exceptions thrown by JS code are signalled to Wasm code as `JsException`.
 *
 * @param thrownValue value thrown by JavaScript; commonly it's an instance of an `Error` or its subclass, but it can be any JavaScript value
 */
@ExperimentalWasmJsInterop
public actual class JsException internal constructor(thrownValue: JsAny?) :
    Throwable(
        message = getJsErrorMessage(thrownValue),
        cause = null,
        jsError = toJsError(thrownValue),
        overwriteJsErrorName = thrownValue !is JsError) {
    internal val thrownValueIsJsError: Boolean = thrownValue is JsError
}

@ExperimentalWasmJsInterop
public actual val JsException.thrownValue: JsAny? get() = if (thrownValueIsJsError) jsError else jsError.cause

@OptIn(ExperimentalWasmJsInterop::class)
@JsName("Error")
internal external class JsError : JsAny {
    val message: String?
    var name: String
    val stack: ExternalInterfaceType
    val cause: JsAny?
    var kotlinException: JsReference<Throwable>?
}

internal const val DEFAULT_JS_EXCEPTION_MESSAGE = "Exception was thrown while running JavaScript code"

@OptIn(ExperimentalWasmJsInterop::class)
internal fun toJsError(thrownValue: JsAny?): JsError {
    return (thrownValue as? JsError) ?: createPlaceholderJsError(DEFAULT_JS_EXCEPTION_MESSAGE, thrownValue)
}

@OptIn(ExperimentalWasmJsInterop::class)
internal fun createPlaceholderJsError(message: String?, cause: JsAny?): JsError =
    setPlaceholderStack(createJsError(message, cause))

@OptIn(ExperimentalWasmJsInterop::class)
internal fun setPlaceholderStack(jsError: JsError): JsError =
    js("Object.assign(jsError, { stack: '' })")

@OptIn(ExperimentalWasmJsInterop::class)
internal fun getJsErrorMessage(thrownValue: JsAny?): String {
    return (thrownValue as? JsError)?.message ?: DEFAULT_JS_EXCEPTION_MESSAGE
}