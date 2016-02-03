@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ExceptionsKt")
@file:kotlin.jvm.JvmVersion
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
package kotlin

import java.io.PrintStream
import java.io.PrintWriter

/**
 * Prints the stack trace of this throwable to the standard output.
 */
@kotlin.internal.InlineOnly
public inline fun Throwable.printStackTrace(): Unit = (this as java.lang.Throwable).printStackTrace()

/**
 * Prints the stack trace of this throwable to the specified [writer].
 */
@kotlin.internal.InlineOnly
public inline fun Throwable.printStackTrace(writer: PrintWriter): Unit = (this as java.lang.Throwable).printStackTrace(writer)

/**
 * Prints the stack trace of this throwable to the specified [stream].
 */
@kotlin.internal.InlineOnly
public inline fun Throwable.printStackTrace(stream: PrintStream): Unit = (this as java.lang.Throwable).printStackTrace(stream)

/**
 * Returns an array of stack trace elements representing the stack trace
 * pertaining to this throwable.
 */
public val Throwable.stackTrace: Array<StackTraceElement>
    get() = (this as java.lang.Throwable).stackTrace!!
