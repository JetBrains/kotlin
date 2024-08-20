/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.ExternalInterfaceType

private val stackPlaceHolder: ExternalInterfaceType = js("''")

/**
 * A wrapper for an exception thrown by a JavaScript code.
 * All exceptions thrown by JS code are signalled to Wasm code as `JsException`.
 *
 * @property thrownValue value thrown by JavaScript; commonly it's an instance of an `Error` or its subclass, but it can be any JavaScript value
 * */
public class JsException internal constructor(public val thrownValue: JsAny?) : Throwable(null, null, stackPlaceHolder) {
    private var _message: String? = null
    override val message: String
        get() {
            var value = _message
            if (value == null) {
                value = if (thrownValue is JsError) thrownValue.message else "Exception was thrown while running JavaScript code"
                _message = value
            }
            return value
        }

    private var _jsStack: ExternalInterfaceType? = null
    internal override val jsStack: ExternalInterfaceType
        get() {
            var value = _jsStack
            if (value == null) {
                value = if (thrownValue is JsError) thrownValue.stack else stackPlaceHolder
                _jsStack = value
            }
            return value
        }
}

@JsName("Error")
private external class JsError : JsAny {
    val message: String
    val stack: ExternalInterfaceType
}
