/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.coroutines.cancellation

/**
 * Thrown by cancellable suspending functions if the coroutine is cancelled while it is suspended.
 * It indicates _normal_ cancellation of a coroutine.
 */
@SinceKotlin("1.4")
public open class CancellationException : IllegalStateException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
