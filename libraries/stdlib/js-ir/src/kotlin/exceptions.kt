/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

public actual open class IndexOutOfBoundsException : RuntimeException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class ConcurrentModificationException : RuntimeException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
    public actual constructor(cause: Throwable?) : super(cause)
}

public actual open class UnsupportedOperationException : RuntimeException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
    public actual constructor(cause: Throwable?) : super(cause)
}


public actual open class NumberFormatException : IllegalArgumentException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}


public actual open class NullPointerException : RuntimeException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class ClassCastException : RuntimeException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AssertionError : Error {
    public actual constructor() : super()
    public constructor(message: String?) : super(message)
    public actual constructor(message: Any?) : super(message?.toString(), message as? Throwable)
}

public actual open class NoSuchElementException : RuntimeException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

@SinceKotlin("1.3")
public actual open class ArithmeticException : RuntimeException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
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
