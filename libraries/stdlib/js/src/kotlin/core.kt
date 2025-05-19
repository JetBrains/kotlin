/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * The property that can be used as a placeholder for statements and values that are defined in JavaScript.
 *
 * This property can be used in two cases:
 *
 *   * To represent body of an external function. In most cases Kotlin does not require to provide bodies of external
 *     functions and properties, but if for some reason you want to (for example, due to limitation of your coding style guides),
 *     you should use `definedExternally`.
 *   * To represent value of default argument.
 *
 * There's two forms of using `definedExternally`:
 *
 *   1. `= definedExternally` (for functions, properties and parameters).
 *   2. `{ definedExternally }` (for functions and property getters/setters).
 *
 * This property can't be used from normal code.
 *
 * Examples:
 *
 * ```kotlin
 * external fun foo(): String = definedExternally
 * external fun bar(x: Int) { definedExternally }
 * external fun baz(z: Any = definedExternally): Array<Any>
 * external val prop: Float = definedExternally
 * ```
 */
public external val definedExternally: Nothing

/**
 * Puts the given piece of a JavaScript code right into the calling function.
 * The compiler replaces call to `js(...)` code with the string constant provided as a parameter.
 *
 * Example:
 *
 * ```kotlin
 * fun logToConsole(message: String): Unit {
 *     js("console.log(message)")
 * }
 * ```
 *
 * @param code the piece of JavaScript code to put to the generated code.
 *        Must be a compile-time constant, otherwise compiler produces error message.
 *        You can safely refer to local variables of calling function (but not to local variables of outer functions),
 *        including parameters. You can't refer to functions, properties and classes by their short names.
 */
@IgnorableReturnValue
public external fun js(code: String): dynamic


/**
 * Function corresponding to JavaScript's `typeof` operator
 */
// @JsIntrinsic
//  To prevent people to insert @OptIn every time
public external fun jsTypeOf(a: Any?): String
