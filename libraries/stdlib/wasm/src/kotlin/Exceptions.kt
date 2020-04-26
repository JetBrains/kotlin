/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin


/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
public open class Throwable(open val message: String?, open val cause: Throwable?) {
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(cause?.toString(), cause)
    constructor() : this(null, null)
}

public actual open class Exception actual constructor(message: String?, cause: Throwable?) : Throwable(message, cause) {
    actual constructor() : this(null, null)
    actual constructor(message: String?) : this(message, null)
    actual constructor(cause: Throwable?) : this(null, cause)
}

public actual open class Error actual constructor(message: String?, cause: Throwable?) : Throwable(message, cause) {
    actual constructor() : this(null, null)
    actual constructor(message: String?) : this(message, null)
    actual constructor(cause: Throwable?) : this(null, cause)
}

public actual open class RuntimeException actual constructor(message: String?, cause: Throwable?) : Exception(message, cause) {
    actual constructor() : this(null, null)
    actual constructor(message: String?) : this(message, null)
    actual constructor(cause: Throwable?) : this(null, cause)
}

public actual open class IllegalArgumentException actual constructor(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    actual constructor() : this(null, null)
    actual constructor(message: String?) : this(message, null)
    actual constructor(cause: Throwable?) : this(null, cause)
}

public actual open class IllegalStateException actual constructor(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    actual constructor() : this(null, null)
    actual constructor(message: String?) : this(message, null)
    actual constructor(cause: Throwable?) : this(null, cause)
}

public actual open class IndexOutOfBoundsException actual constructor(message: String?) : RuntimeException(message) {
    actual constructor() : this(null)
}

public actual open class ConcurrentModificationException actual constructor(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    actual constructor() : this(null, null)
    actual constructor(message: String?) : this(message, null)
    actual constructor(cause: Throwable?) : this(null, cause)
}

public actual open class UnsupportedOperationException actual constructor(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    actual constructor() : this(null, null)
    actual constructor(message: String?) : this(message, null)
    actual constructor(cause: Throwable?) : this(null, cause)
}


public actual open class NumberFormatException actual constructor(message: String?) : IllegalArgumentException(message) {
    actual constructor() : this(null)
}


public actual open class NullPointerException actual constructor(message: String?) : RuntimeException(message) {
    actual constructor() : this(null)
}

public actual open class ClassCastException actual constructor(message: String?) : RuntimeException(message) {
    actual constructor() : this(null)
}

public actual open class AssertionError private constructor(message: String?, cause: Throwable?) : Error(message, cause) {
    actual constructor() : this(null)
    constructor(message: String?) : this(message, null)
    actual constructor(message: Any?) : this(message.toString(), message as? Throwable)
}

public actual open class NoSuchElementException actual constructor(message: String?) : RuntimeException(message) {
    actual constructor() : this(null)
}

@SinceKotlin("1.3")
public actual open class ArithmeticException actual constructor(message: String?) : RuntimeException(message) {
    actual constructor() : this(null)
}

public actual open class NoWhenBranchMatchedException actual constructor(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    actual constructor() : this(null, null)
    actual constructor(message: String?) : this(message, null)
    actual constructor(cause: Throwable?) : this(null, cause)
}

public actual open class UninitializedPropertyAccessException actual constructor(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    actual constructor() : this(null, null)
    actual constructor(message: String?) : this(message, null)
    actual constructor(cause: Throwable?) : this(null, cause)
}

