/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.ExternalInterfaceType
import kotlin.wasm.internal.getSimpleName
import kotlin.wasm.internal.jsToKotlinStringAdapter

/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
public open class Throwable(public open val message: String?, public open val cause: kotlin.Throwable?) {
    public constructor(message: String?) : this(message, null)

    public constructor(cause: Throwable?) : this(cause?.toString(), cause)

    public constructor() : this(null, null)

    internal val jsStack: ExternalInterfaceType = captureStackTrace()

    private var _stack: String? = null
    internal val stack: String
        get() {
            var value = _stack
            if (value == null) {
                value = jsToKotlinStringAdapter(jsStack).removePrefix("Error\n")
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
        val s = getSimpleName(this.typeInfo)
        return if (message != null) s + ": " + message.toString() else s
    }
}

private fun captureStackTrace(): ExternalInterfaceType =
    js("new Error().stack")
