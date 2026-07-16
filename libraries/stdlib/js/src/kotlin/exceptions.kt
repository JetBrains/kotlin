/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsFileName("ExceptionsJs")

package kotlin

public actual open class AssertionError : Error {
    public actual constructor() : super()
    public constructor(message: String?) : super(message)
    public actual constructor(message: Any?) : super(message?.toString(), message as? Throwable)
    @SinceKotlin("1.4")
    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
}

public actual open class NoWhenBranchMatchedException : RuntimeException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
    public actual constructor(cause: Throwable?) : super(cause)
}

public actual open class UninitializedPropertyAccessException : RuntimeException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
    public actual constructor(cause: Throwable?) : super(cause)
}
