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
@Suppress("NO_ACTUAL_FOR_EXPECT")
@SinceKotlin("1.8")
@ExperimentalStdlibApi
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
 * Executes the given [block] function on this resource and then closes it down correctly whether an exception
 * is thrown or not.
 *
 * In case if the resource is being closed due to an exception occurred in [block], and the closing also fails with an exception,
 * the latter is added to the [suppressed][Throwable.addSuppressed] exceptions of the former.
 *
 * @param block a function to process this [AutoCloseable] resource.
 * @return the result of [block] function invoked on this resource.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECTED_DECLARATION_WITH_BODY")
@SinceKotlin("1.8")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public expect inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    error("Unreachable")
}
