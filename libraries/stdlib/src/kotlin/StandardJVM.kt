package kotlin

import java.io.PrintWriter
import java.io.PrintStream

/**
 * Allows a stack trace to be printed from Kotlin's [[Throwable]]
 */
public fun Throwable.printStackTrace(writer: PrintWriter): Unit {
    val jlt = this as java.lang.Throwable
    jlt.printStackTrace(writer)
}

/**
 * Allows a stack trace to be printed from Kotlin's [[Throwable]]
 */
public fun Throwable.printStackTrace(stream: PrintStream): Unit {
    val jlt = this as java.lang.Throwable
    jlt.printStackTrace(stream)
}

/**
 * Returns the stack trace
 */

public fun Throwable.getStackTrace(): Array<StackTraceElement> {
    val jlt = this as java.lang.Throwable
    return jlt.getStackTrace()!!
}

