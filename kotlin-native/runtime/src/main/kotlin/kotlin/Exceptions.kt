/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

@Deprecated("Use IndexOutOfBoundsException instead.")
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
public open class ArrayIndexOutOfBoundsException : IndexOutOfBoundsException {

    public constructor() : super()

    public constructor(message: String?) : super(message)
}

@PublishedApi
internal open class TypeCastException : ClassCastException {

    constructor() : super()

    constructor(message: String?) : super(message)
}

public actual open class AssertionError : Error {

    public actual constructor()

    public actual constructor(message: Any?) : super(message?.toString(), message as? Throwable)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
}

@Suppress("EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY")
internal actual open class NoWhenBranchMatchedException : RuntimeException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    actual constructor(cause: Throwable?) : super(cause)
}

@Suppress("EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY")
internal actual open class UninitializedPropertyAccessException : RuntimeException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    actual constructor(cause: Throwable?) : super(cause)
}

public actual open class OutOfMemoryError : Error {

    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)
}
