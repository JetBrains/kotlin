@file:Suppress("EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES")
package kotlin.js

/**
 * A wrapper for an exception thrown by a JavaScript code.
 * All exceptions thrown by JS code are signalled to Wasm code as `JsException`.
 * */
@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
public actual typealias JsException = Throwable

/**
 *
 * @property thrownValue value thrown by JavaScript; commonly it's an instance of an `Error` or its subclass, but it can be any JavaScript value
 * */
@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
public actual inline val JsException.thrownValue: JsAny?
    get() = unsafeCast<JsAny?>()
