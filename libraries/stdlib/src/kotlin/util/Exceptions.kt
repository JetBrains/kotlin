@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ExceptionsKt")
@file:kotlin.jvm.JvmVersion
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
 * When supported by the platform adds the specified exception to the list of exceptions that were
 * suppressed in order to deliver this exception.
 */
public fun Throwable.addSuppressed(exception: Throwable) = IMPLEMENTATIONS.addSuppressed(this, exception)