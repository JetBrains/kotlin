/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Represents an object that may hold resources, like open files or network connections, until [close] is called.
 *
 * Instances should normally be managed with [use], which closes the object after the operation completes, including when the operation
 * throws an exception.
 *
 * @sample samples.misc.AutoCloseables.naive
 * @sample samples.misc.AutoCloseables.idempotent
 */
@SinceKotlin("2.0")
@WasExperimental(ExperimentalStdlibApi::class)
public expect interface AutoCloseable {
    /**
     * Releases the resources held by this object.
     *
     * This function may throw, and unless the object is managed by [use], it's caller's responsibility to catch those exceptions.
     *
     * In case of a failure implementations should release their underlying resources and mark the object as closed before
     * throwing an exception. This helps to ensure timely cleanup, especially when the resources wrap, or are wrapped by, other resources.
     *
     * Calling this function more than once may have observable side effects. However, implementations should make it idempotent whenever
     * feasible.
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
@IgnorableReturnValue
public expect inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    error("Unreachable")
}
