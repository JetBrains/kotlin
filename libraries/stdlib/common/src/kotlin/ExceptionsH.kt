/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin


public expect open class Error : Throwable {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

public expect open class Exception : Throwable {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

public expect open class RuntimeException : Exception {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

public expect open class IllegalArgumentException : RuntimeException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

public expect open class IllegalStateException : RuntimeException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

public expect open class IndexOutOfBoundsException : RuntimeException {
    public constructor()
    public constructor(message: String?)
}

public expect open class ConcurrentModificationException : RuntimeException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

public expect open class UnsupportedOperationException : RuntimeException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

public expect open class NumberFormatException : IllegalArgumentException {
    public constructor()
    public constructor(message: String?)
}

public expect open class NullPointerException : RuntimeException {
    public constructor()
    public constructor(message: String?)
}

public expect open class ClassCastException : RuntimeException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AssertionError : Error {
    public constructor()
    public constructor(message: Any?)

    @SinceKotlin("1.9")
    public constructor(message: String?, cause: Throwable?)
}

public expect open class NoSuchElementException : RuntimeException {
    public constructor()
    public constructor(message: String?)
}

@SinceKotlin("1.3")
public expect open class ArithmeticException : RuntimeException {
    public constructor()
    public constructor(message: String?)
}

@Deprecated("This exception type is not supposed to be thrown or caught in common code and will be removed from kotlin-stdlib-common soon.", level = DeprecationLevel.ERROR)
public expect open class NoWhenBranchMatchedException : RuntimeException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

@Deprecated("This exception type is not supposed to be thrown or caught in common code and will be removed from kotlin-stdlib-common soon.", level = DeprecationLevel.ERROR)
public expect class UninitializedPropertyAccessException : RuntimeException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

/**
 * Thrown after invocation of a function or property that was expected to return `Nothing`, but returned something instead.
 */
@SinceKotlin("1.4")
@PublishedApi
internal class KotlinNothingValueException : RuntimeException {
    public constructor() : super()
    public constructor(message: String?) : super(message)
    public constructor(message: String?, cause: Throwable?) : super(message, cause)
    public constructor(cause: Throwable?) : super(cause)
}


/**
 * Returns the detailed description of this throwable with its stack trace.
 *
 * The detailed description includes:
 * - the short description (see [Throwable.toString]) of this throwable;
 * - the complete stack trace;
 * - detailed descriptions of the exceptions that were [suppressed][suppressedExceptions] in order to deliver this exception;
 * - the detailed description of each throwable in the [Throwable.cause] chain.
 */
@SinceKotlin("1.4")
public expect fun Throwable.stackTraceToString(): String

/**
 * Prints the [detailed description][Throwable.stackTraceToString] of this throwable to the standard output or standard error output.
 */
@SinceKotlin("1.4")
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public expect fun Throwable.printStackTrace(): Unit

/**
 * When supported by the platform, adds the specified exception to the list of exceptions that were
 * suppressed in order to deliver this exception.
 */
@SinceKotlin("1.4")
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public expect fun Throwable.addSuppressed(exception: Throwable)

/**
 * Returns a list of all exceptions that were suppressed in order to deliver this exception.
 *
 * The list can be empty:
 * - if no exceptions were suppressed;
 * - if the platform doesn't support suppressed exceptions;
 * - if this [Throwable] instance has disabled the suppression.
 */
@SinceKotlin("1.4")
public expect val Throwable.suppressedExceptions: List<Throwable>
