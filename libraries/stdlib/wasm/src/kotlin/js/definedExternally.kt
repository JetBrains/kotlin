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
public external val definedExternally: Nothing