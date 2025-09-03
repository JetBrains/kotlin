/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.ManagedExternref
import kotlin.wasm.internal.jsToKotlinStringAdapter
import kotlin.wasm.internal.wasmGetObjectRtti
import kotlin.wasm.internal.getQualifiedName
import kotlin.wasm.internal.getSimpleName

/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
@OptIn(ExperimentalWasmJsInterop::class)
public actual open class Throwable internal constructor(
    public actual open val message: String?,
    public actual open val cause: Throwable?,
    @ManagedExternref internal val jsError: JsError,
    overwriteJsErrorName: Boolean = true
) {
    init {
        if (overwriteJsErrorName)
            jsError.name = getSimpleName(wasmGetObjectRtti(this))
        jsError.kotlinException = toJsReference()
    }

    public actual constructor(message: String?, cause: Throwable?) : this(message, cause, createJsError(message, cause?.jsError))

    public actual constructor(message: String?) : this(message, null)

    public actual constructor(cause: Throwable?) : this(cause?.toString(), cause)

    public actual constructor() : this(null, null)

    internal val jsStack get() = jsError.stack

    private var _stack: String? = null
    internal val stack: String
        get() {
            var value = _stack
            if (value == null) {
                value = jsToKotlinStringAdapter(jsStack)
                _stack = value
            }

            return value
        }

    internal var suppressedExceptionsList: MutableList<Throwable>? = null

    /**
     * Returns the short description of this throwable consisting of the exception class name
     * followed by the exception message if it is not null.
     */
    public override fun toString(): String {
        val s = getQualifiedName(wasmGetObjectRtti(this))
        return if (message != null) "$s: $message" else s
    }
}

internal actual var Throwable.suppressedExceptionsList: MutableList<Throwable>?
    get() = this.suppressedExceptionsList
    set(value) { this.suppressedExceptionsList = value }

internal actual val Throwable.stack: String get() = this.stack

@OptIn(ExperimentalWasmJsInterop::class)
internal fun createJsError(message: String?, cause: JsAny?): JsError =
    js("new Error(message, { cause })")