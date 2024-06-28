/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.ExcludedFromCodegen

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
 * ``` kotlin
 * external fun foo(): String = definedExternally
 * external fun bar(x: Int) { definedExternally }
 * external fun baz(z: Any = definedExternally): Array<Any>
 * external val prop: Float = definedExternally
 * ```
 */
@ExcludedFromCodegen
@Suppress("WRONG_JS_INTEROP_TYPE")
public external val definedExternally: Nothing

/**
 * This function allows you to incorporate JavaScript [code] into Kotlin/Wasm codebase.
 * It is used to implement top-level functions and initialize top-level properties.
 *
 * It is important to note, that calls to [js] function should be the only expression
 * in a function body or a property initializer.
 *
 * [code] parameter should be a compile-time constant.
 *
 * When used in an expression context, [code] should contain a single JavaScript expression. For example:
 *
 * ``` kotlin
 * val version: String = js("process.version")
 * fun newEmptyJsArray(): JsValue = js("[]")
 * ```
 *
 * When used in a function body, [code] is expected to be a list of JavaScript statements. For example:
 *
 * ``` kotlin
 * fun log(message1: String, message2: String) {
 *     js("""
 *     console.log(message1);
 *     console.log(message2);
 *     """)
 * }
 * ```
 *
 * You can use parameters of calling function in JavaScript [code].
 * However, other Kotlin declarations are not visible inside the [code] block.
 */
@ExcludedFromCodegen
@SinceKotlin("1.9")
public external fun js(code: String): Nothing
