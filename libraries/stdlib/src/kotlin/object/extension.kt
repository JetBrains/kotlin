/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.`object`

import kotlin.contracts.contract


/**
 * Checks whether the receiver object is not null, enhancing readability and enabling smart casting.
 *
 * This extension function improves code clarity by providing an expressive alternative to the standard
 * null-check (`this != null`). Additionally, the function utilizes Kotlin contracts to inform the compiler
 * about the nullability of the receiver, allowing for safe smart casting in subsequent code.
 *
 * @receiver Any object of type [T].
 * @return `true` if the receiver is not null, `false` otherwise.
 *
 * Example usage:
 * ```
 * fun processInput(input: String?) {
 *     if (input.isNotNull()) {
 *         // The compiler smart-casts 'input' to a non-nullable String here
 *         println("Processing input: ${input.uppercase()}")
 *     } else {
 *         println("Input is null")
 *     }
 * }
 * ```
 *
 * @see kotlin.contracts.Contract
 */
public fun <T> T.isNotNull(): Boolean {
    contract {
        returns(true) implies (this@isNotNull != null)
    }
    return this != null
}