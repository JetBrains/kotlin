/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

public actual open class Error : Throwable {

    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    public actual constructor(cause: Throwable?) : super(cause)
}

public actual open class Exception : Throwable {

    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    public actual constructor(cause: Throwable?) : super(cause)
}

public actual open class RuntimeException : Exception {

    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    public actual constructor(cause: Throwable?) : super(cause)
}

public actual open class NullPointerException : RuntimeException {

    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)
}

public actual open class NoSuchElementException : RuntimeException {

    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)
}

public actual open class IllegalArgumentException : RuntimeException {

    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    public actual constructor(cause: Throwable?) : super(cause)
}

public actual open class IllegalStateException : RuntimeException {

    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    public actual constructor(cause: Throwable?) : super(cause)
}

public actual open class UnsupportedOperationException : RuntimeException {

    public actual constructor()

    public actual constructor(message: String?) : super(message)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    public actual constructor(cause: Throwable?) : super(cause)
}

public actual open class IndexOutOfBoundsException : RuntimeException {

    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)
}

@Deprecated("Use IndexOutOfBoundsException instead.")
@DeprecatedSinceKotlin(warningSince = "1.9")
public open class ArrayIndexOutOfBoundsException : IndexOutOfBoundsException {

    public constructor() : super()

    public constructor(message: String?) : super(message)
}

public actual open class ClassCastException : RuntimeException {

    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)
}

@PublishedApi
internal open class TypeCastException : ClassCastException {

    constructor() : super()

    constructor(message: String?) : super(message)
}

public actual open class ArithmeticException : RuntimeException {
    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)
}

public actual open class AssertionError : Error {

    public actual constructor()

    @Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(cause: Throwable?) : super(cause)

    public actual constructor(message: Any?) : super(message?.toString(), message as? Throwable)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
internal actual open class NoWhenBranchMatchedException : RuntimeException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    actual constructor(cause: Throwable?) : super(cause)
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
internal actual open class UninitializedPropertyAccessException : RuntimeException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    actual constructor(cause: Throwable?) : super(cause)
}

public open class OutOfMemoryError : Error {

    public constructor() : super()

    public constructor(message: String?) : super(message)
}

public actual open class NumberFormatException : IllegalArgumentException {

    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)
}

public actual open class ConcurrentModificationException actual constructor(message: String?, cause: Throwable?) :
        RuntimeException(message, cause) {

    public actual constructor() : this(null, null)

    public actual constructor(message: String?) : this(message, null)

    public actual constructor(cause: Throwable?) : this(null, cause)
}