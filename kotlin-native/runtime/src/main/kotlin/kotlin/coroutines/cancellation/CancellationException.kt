/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.coroutines.cancellation

@ExperimentalStdlibApi
@SinceKotlin("1.4")
public actual open class CancellationException : IllegalStateException {
    actual constructor() : super()
    actual constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
