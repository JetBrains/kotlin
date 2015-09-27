@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ExceptionsKt")
package kotlin

import java.io.PrintStream
import java.io.PrintWriter

/**
 * Allows a stack trace to be printed from Kotlin's [Throwable].
 */
public fun Throwable.printStackTrace(writer: PrintWriter): Unit {
    val jlt = this as java.lang.Throwable
    jlt.printStackTrace(writer)
}

/**
 * Allows a stack trace to be printed from Kotlin's [Throwable].
 */
public fun Throwable.printStackTrace(stream: PrintStream): Unit {
    val jlt = this as java.lang.Throwable
    jlt.printStackTrace(stream)
}

/**
 * Returns the list of stack trace elements in a Kotlin stack trace.
 */
public fun Throwable.getStackTrace(): Array<StackTraceElement> {
    val jlt = this as java.lang.Throwable
    return jlt.getStackTrace()!!
}

