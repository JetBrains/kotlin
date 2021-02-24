/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.cancellation

/**
 * Thrown by cancellable suspending functions if the coroutine is cancelled while it is suspended.
 * It indicates _normal_ cancellation of a coroutine.
 */
@SinceKotlin("1.4")
public expect open class CancellationException : IllegalStateException {
    public constructor()
    public constructor(message: String?)
}

/**
 * Creates an instance of [CancellationException] with the given [message] and [cause].
 */
@SinceKotlin("1.4")
@Suppress("FunctionName", "NO_ACTUAL_FOR_EXPECT")
public expect fun CancellationException(message: String?, cause: Throwable?): CancellationException

/**
 * Creates an instance of [CancellationException] with the given [cause].
 */
@SinceKotlin("1.4")
@Suppress("FunctionName", "NO_ACTUAL_FOR_EXPECT")
public expect fun CancellationException(cause: Throwable?): CancellationException