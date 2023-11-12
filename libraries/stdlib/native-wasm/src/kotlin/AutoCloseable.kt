/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@SinceKotlin("1.8")
@ExperimentalStdlibApi
public actual interface AutoCloseable {
    public actual fun close(): Unit
}

@SinceKotlin("1.8")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public actual inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        this.closeFinally(exception)
    }
}

@SinceKotlin("1.8")
@ExperimentalStdlibApi
@PublishedApi
internal fun AutoCloseable?.closeFinally(cause: Throwable?): Unit = when {
    this == null -> {}
    cause == null -> close()
    else ->
        try {
            close()
        } catch (closeException: Throwable) {
            cause.addSuppressed(closeException)
        }
}