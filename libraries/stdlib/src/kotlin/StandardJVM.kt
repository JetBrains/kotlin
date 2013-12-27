package kotlin

import java.util.ArrayList
import java.util.LinkedList
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.TreeSet
import java.util.SortedSet
import java.util.Comparator
import java.io.PrintWriter
import java.io.PrintStream
import java.util.concurrent.Callable

/**
 * Add iterated elements to a [[LinkedHashSet]] to preserve insertion order
 */
public fun <T> Iterator<T>.toLinkedSet() : LinkedHashSet<T> = toCollection(LinkedHashSet<T>())

/**
 * Add iterated elements to [[SortedSet]] with the given *comparator* to ensure iteration is in the order of the given comparator
 */
public fun <T> Iterator<T>.toSortedSet(comparator: Comparator<T>) : SortedSet<T> = toCollection(TreeSet<T>(comparator))


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

public fun Throwable.getStackTrace() : Array<StackTraceElement> {
    val jlt = this as java.lang.Throwable
    return jlt.getStackTrace()!!
}

/**
 * A helper method for creating a [[Callable]] from a function
 */
deprecated("Use SAM constructor: Callable(...)")
public /*inline*/ fun <T> callable(action: ()-> T): Callable<T> {
    return object: Callable<T> {
        public override fun call() = action()
    }
}

/**
 * A helper method for creating a [[Runnable]] from a function
 */
deprecated("Use SAM constructor: Runnable(...)")
public /*inline*/ fun runnable(action: ()-> Unit): Runnable {
    return object: Runnable {
        public override fun run() {
            action()
        }
    }
}
