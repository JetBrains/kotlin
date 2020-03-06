/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ExceptionsKt")
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
package kotlin

import java.io.PrintStream
import java.io.PrintWriter
import kotlin.internal.*

/**
 * Prints the stack trace of this throwable to the standard output.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // to be used when no member available
@kotlin.internal.InlineOnly
public inline fun Throwable.printStackTrace(): Unit = (this as java.lang.Throwable).printStackTrace()

/**
 * Prints the stack trace of this throwable to the specified [writer].
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // to be used when no member available
@kotlin.internal.InlineOnly
public inline fun Throwable.printStackTrace(writer: PrintWriter): Unit = (this as java.lang.Throwable).printStackTrace(writer)

/**
 * Prints the stack trace of this throwable to the specified [stream].
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // to be used when no member available
@kotlin.internal.InlineOnly
public inline fun Throwable.printStackTrace(stream: PrintStream): Unit = (this as java.lang.Throwable).printStackTrace(stream)

/**
 * Returns an array of stack trace elements representing the stack trace
 * pertaining to this throwable.
 */
@Suppress("ConflictingExtensionProperty")
public val Throwable.stackTrace: Array<StackTraceElement>
    get() = (this as java.lang.Throwable).stackTrace!!

/**
 * When supported by the platform, adds the specified exception to the list of exceptions that were
 * suppressed in order to deliver this exception.
 */
@SinceKotlin("1.1")
public actual fun Throwable.addSuppressed(exception: Throwable) {
    if (this !== exception)
        IMPLEMENTATIONS.addSuppressed(this, exception)
}

/**
 * Returns a list of all exceptions that were suppressed in order to deliver this exception.
 *
 * The list can be empty:
 * - if no exceptions were suppressed;
 * - if the platform doesn't support suppressed exceptions;
 * - if this [Throwable] instance has disabled the suppression.
 */
@SinceKotlin("1.4")
public actual val Throwable.suppressedExceptions: List<Throwable>
    get() = IMPLEMENTATIONS.getSuppressed(this)