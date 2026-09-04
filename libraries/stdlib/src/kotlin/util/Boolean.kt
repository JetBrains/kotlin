/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.contracts.*
import kotlin.internal.InlineOnly

/**
 * Performs the given [action] if `this` [Boolean] is `true`.
 * Returns the original `Boolean` unchanged.
 *
 * This is a shorthand for `if (this) { action(); true } else { false }`.
 *
 * ## Usage
 *
 * ### Performing an action while returning a `Boolean`
 *
 * Occasionally, a function needs to compute a `Boolean` indicating if an operation should be performed.
 *
 * ```
 * fun trySkipChar(): Boolean = (index < length).onTrue { ++index }
 * ```
 *
 * Similarly, `onTrue` can serve as a shorthand for `.also { if (it) { action() } }`. Example:
 *
 * ```
 * val someOperationFailed = operations.any {
 *     it.hasFailed().onTrue {
 *         log("Operation $it failed")
 *     }
 * }
 * ```
 *
 * ### Acting on the result of a `Boolean`-returning operation
 *
 * Some operations indicate their success or failure by returning a [Boolean] rather than throwing an exception,
 * returning `null`, or returning a [Result].
 * `onTrue` can help succinctly process such results:
 *
 * ```
 * tryStoreInDatabase().onTrue {
 *     return StorageStatus.SUCCESS
 * }
 * ```
 *
 * @see onFalse
 */
@SinceKotlin("2.5")
@IgnorableReturnValue
@ExperimentalStdlibApi
@InlineOnly
public inline fun Boolean.onTrue(action: () -> Unit): Boolean {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return if (this) {
        action()
        true
    } else {
        false
    }
}

/**
 * Performs the given [action] if `this` [Boolean] is `false`.
 * Returns the original `Boolean` unchanged.
 *
 * This is a shorthand for `if (this) { true } else { action(); false }`.
 *
 * ## Usage
 *
 * ### Performing an action while returning a `Boolean`
 *
 * Occasionally, a function needs to compute a `Boolean` indicating if an operation should be performed.
 *
 * ```
 * fun tryAllocate(): Boolean {
 *     val cell = allocateCell()
 *     return allocateImpl(cell).onFalse {
 *         cell.release()
 *     }
 * }
 * ```
 *
 * Similarly, `onFalse` can serve as a shorthand for `.also { if (!it) { action() } }`. Example:
 *
 * ```
 * val allOperationsSucceeded = operations.all {
 *     it.hasSucceeded().onFalse {
 *         log("Operation $it failed")
 *     }
 * }
 * ```
 *
 * ### Acting on the result of a `Boolean`-returning operation
 *
 * Some operations indicate their success or failure by returning a [Boolean] rather than throwing an exception,
 * returning `null`, or returning a [Result].
 * `onFalse` can help succinctly process such results:
 *
 * ```
 * tryStoreInDatabase().onFalse {
 *     processError(queryDatabaseError())
 * }
 * ```
 *
 * @see onTrue
 */
@SinceKotlin("2.5")
@IgnorableReturnValue
@ExperimentalStdlibApi
@InlineOnly
public inline fun Boolean.onFalse(action: () -> Unit): Boolean {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return if (this) {
        true
    } else {
        action()
        false
    }
}

/**
 * Checks [condition], and if it is `true`, runs the [block] and returns its result. Returns `null` otherwise.
 *
 * This is a shorthand for `if (condition) { block() } else { null }`.
 *
 * ## Usage
 *
 * This function is useful when a condition determines whether an action needs to be performed, with its result returned.
 *
 * ```
 * // If `shouldSendPopup` is `false`, `createPopup` will not get called
 * val popup: Popup? = ifOrNull(shouldSendPopup) {
 *     sendPopup()
 * }
 * ```
 */
@SinceKotlin("2.5")
@ExperimentalStdlibApi
@InlineOnly
public inline fun <T> ifOrNull(condition: Boolean, block: () -> T): T? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (condition) {
        block()
    } else {
        null
    }
}
