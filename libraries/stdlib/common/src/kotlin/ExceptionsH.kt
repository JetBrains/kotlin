/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.RequireKotlin


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
@Suppress("EXPECT_ACTUAL_IR_INCOMPATIBILITY") // internal in Native and Wasm
public expect open class NoWhenBranchMatchedException : RuntimeException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

@Deprecated("This exception type is not supposed to be thrown or caught in common code and will be removed from kotlin-stdlib-common soon.", level = DeprecationLevel.ERROR)
@Suppress("EXPECT_ACTUAL_IR_INCOMPATIBILITY") // internal in Native and Wasm
public expect class UninitializedPropertyAccessException : RuntimeException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

/**
 * Thrown when on attempt to initialize a `lateinit val` property that was already initialized.
 *
 * Refer to [KEEP-0445](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0455-lateinit-val.md)
 * for additional information about `lateinit val`s and its runtime behavior.
 *
 * ### Implementation details
 * Visibility of the exception can be relaxed in the future, depending on the feedback.
 * It is a published API as the generated code will depend on it.
 */
@PublishedApi
@SinceKotlin("2.5")
@RequireKotlin(
    version = "2.5.20",
    versionKind = COMPILER_VERSION,
    message = "InitializedPropertyAssignmentException was introduced in Kotlin 2.5.20"
)
internal class InitializedPropertyAssignmentException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
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
public expect fun Throwable.printStackTrace(): Unit

/**
 * When supported by the platform, adds the specified exception to the list of exceptions that were
 * suppressed in order to deliver this exception.
 */
@SinceKotlin("1.4")
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
