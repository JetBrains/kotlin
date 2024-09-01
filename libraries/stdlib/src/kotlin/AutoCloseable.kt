/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A resource that can be closed or released.
 */
@SinceKotlin("2.0")
@WasExperimental(ExperimentalStdlibApi::class)
public expect interface AutoCloseable {
    /**
     * Closes this resource.
     *
     * This function may throw, thus it is strongly recommended to use the [use] function instead,
     * which closes this resource correctly whether an exception is thrown or not.
     *
     * Implementers of this interface should pay increased attention to cases where the close operation may fail.
     * It is recommended that all underlying resources are closed and the resource internally is marked as closed
     * before throwing an exception. Such a strategy ensures that the resources are released in a timely manner,
     * and avoids many problems that could come up when the resource wraps, or is wrapped, by another resource.
     *
     * Note that calling this function more than once may have some visible side effect.
     * However, implementers of this interface are strongly recommended to make this function idempotent.
     */
    public fun close(): Unit
}

/**
 * Returns an [AutoCloseable] instance that executes the specified [closeAction]
 * upon invocation of its [`close()`][AutoCloseable.close] function.
 *
 * This function allows specifying custom cleanup actions for resources.
 *
 * Note that each invocation of the `close()` function on the returned `AutoCloseable` instance executes the [closeAction].
 * Therefore, implementers are strongly recommended to make the [closeAction] idempotent, or to prevent multiple invocations.
 *
 * Example:
 *
 * ```kotlin
 * val autoCloseable = AutoCloseable {
 *     // Cleanup action, e.g., closing a file or releasing a network connection
 *     Logger.log("Releasing the network connection.")
 *     networkConnection.release()
 * }
 *
 * // Now you can pass the autoCloseable to a function or use it directly.
 * autoCloseable.use {
 *     // Use the connection, which will be automatically released when this scope finishes.
 *     val content = networkConnection.readContent()
 *     Logger.log("Network connection content: $content")
 * }
 * ```
 *
 * @See AutoCloseable.use
 */
@SinceKotlin("2.0")
@kotlin.internal.InlineOnly
public expect inline fun AutoCloseable(crossinline closeAction: () -> Unit): AutoCloseable

/**
 * Executes the given [block] function on this resource and then closes it down correctly whether an exception
 * is thrown or not.
 *
 * In case if the resource is being closed due to an exception occurred in [block], and the closing also fails with an exception,
 * the latter is added to the [suppressed][Throwable.addSuppressed] exceptions of the former.
 *
 * @param block a function to process this [AutoCloseable] resource.
 * @return the result of [block] function invoked on this resource.
 */
@Suppress("EXPECTED_DECLARATION_WITH_BODY", "WRONG_INVOCATION_KIND")
@SinceKotlin("2.0")
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
public expect inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    error("Unreachable")
}
