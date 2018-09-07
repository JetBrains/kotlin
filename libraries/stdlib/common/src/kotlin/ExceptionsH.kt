/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin


public expect open class Error : Throwable {
    constructor()
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
    constructor(cause: Throwable?)
}

public expect open class Exception : Throwable {
    constructor()
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
    constructor(cause: Throwable?)
}

public expect open class RuntimeException : Exception {
    constructor()
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
    constructor(cause: Throwable?)
}

public expect open class IllegalArgumentException : RuntimeException {
    constructor()
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
    constructor(cause: Throwable?)
}

public expect open class IllegalStateException : RuntimeException {
    constructor()
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
    constructor(cause: Throwable?)
}

public expect open class IndexOutOfBoundsException : RuntimeException {
    constructor()
    constructor(message: String?)
}

public expect open class ConcurrentModificationException : RuntimeException {
    constructor()
    constructor(message: String?)
    @Deprecated("The constructor is not supported on all platforms and will be removed from kotlin-stdlib-common soon.", level = DeprecationLevel.ERROR)
    constructor(message: String?, cause: Throwable?)
    @Deprecated("The constructor is not supported on all platforms and will be removed from kotlin-stdlib-common soon.", level = DeprecationLevel.ERROR)
    constructor(cause: Throwable?)
}

public expect open class UnsupportedOperationException : RuntimeException {
    constructor()
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
    constructor(cause: Throwable?)
}

public expect open class NumberFormatException : IllegalArgumentException {
    constructor()
    constructor(message: String?)
}

public expect open class NullPointerException : RuntimeException {
    constructor()
    constructor(message: String?)
}

public expect open class ClassCastException : RuntimeException {
    constructor()
    constructor(message: String?)
}

public expect open class AssertionError : Error {
    constructor()
    constructor(message: Any?)
}

public expect open class NoSuchElementException : RuntimeException {
    constructor()
    constructor(message: String?)
}

@SinceKotlin("1.3")
public expect open class ArithmeticException : RuntimeException {
    constructor()
    constructor(message: String?)
}

@Deprecated("This exception type is not supposed to be thrown or caught in common code and will be removed from kotlin-stdlib-common soon.")
public expect open class NoWhenBranchMatchedException : RuntimeException {
    constructor()
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
    constructor(cause: Throwable?)
}

@Deprecated("This exception type is not supposed to be thrown or caught in common code and will be removed from kotlin-stdlib-common soon.")
public expect class UninitializedPropertyAccessException : RuntimeException {
    constructor()
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
    constructor(cause: Throwable?)
}
