/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("AutoCloseableKt")
@file:JvmPackageName("kotlin.jdk7")

package kotlin

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Represents an object that may hold resources, like open files or network connections, until [close] is called.
 *
 * Instances should normally be managed with [use], which closes the object after the operation completes, including when the operation
 * throws an exception.
 *
 * Java types implementing [java.lang.AutoCloseable] can be used as Kotlin [AutoCloseable] resources, and Kotlin
 * implementations can be used in Java's try-with-resources statement.
 *
 * @sample samples.misc.AutoCloseables.naive
 * @sample samples.misc.AutoCloseables.idempotent
 * @see [java.lang.AutoCloseable]
 */
@SinceKotlin("2.0")
public actual typealias AutoCloseable = java.lang.AutoCloseable

@SinceKotlin("2.0")
@kotlin.internal.InlineOnly
public actual inline fun AutoCloseable(crossinline closeAction: () -> Unit): AutoCloseable = java.lang.AutoCloseable { closeAction() }

@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
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

@SinceKotlin("1.2")
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
