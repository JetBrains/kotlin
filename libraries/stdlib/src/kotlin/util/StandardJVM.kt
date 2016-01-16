@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ExceptionsKt")
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
package kotlin

import java.io.PrintStream
import java.io.PrintWriter

/**
 * Prints the stack trace of this throwable to the standard output.
 */
public fun Throwable.printStackTrace(): Unit {
    val jlt = this as java.lang.Throwable
    jlt.printStackTrace()
}

/**
 * Prints the stack trace of this throwable to the specified [writer].
 */
public fun Throwable.printStackTrace(writer: PrintWriter): Unit {
    val jlt = this as java.lang.Throwable
    jlt.printStackTrace(writer)
}

/**
 * Prints the stack trace of this throwable to the specified [stream].
 */
public fun Throwable.printStackTrace(stream: PrintStream): Unit {
    val jlt = this as java.lang.Throwable
    jlt.printStackTrace(stream)
}

/**
 * Returns an array of stack trace elements representing the stack trace
 * pertaining to this throwable.
 */
public val Throwable.stackTrace: Array<StackTraceElement>
    get() = (this as java.lang.Throwable).stackTrace!!
