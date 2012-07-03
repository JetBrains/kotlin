package kotlin

import java.util.Collection
import java.util.ArrayList
import java.util.LinkedList
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.TreeSet
import java.util.SortedSet
import java.util.Comparator
import java.io.PrintWriter
import java.io.PrintStream

/**
 * Add iterated elements to a [[LinkedHashSet]] to preserve insertion order
 */
public inline fun <T> Iterator<T>.toLinkedSet() : LinkedHashSet<T> = toCollection(LinkedHashSet<T>())

/**
 * Add iterated elements to [[SortedSet]] to ensure iteration is in the order of the default comparator
 * for the type
 */
public inline fun <T> Iterator<T>.toSortedSet() : SortedSet<T> = toCollection(TreeSet<T>())

/**
 * Add iterated elements to [[SortedSet]] with the given *comparator* to ensure iteration is in the order of the given comparator
 */
public inline fun <T> Iterator<T>.toSortedSet(comparator: Comparator<T>) : SortedSet<T> = toCollection(TreeSet<T>(comparator))


/**
 * Allows a stack trace to be printed from Kotlin's [[Throwable]]
 */
public inline fun Throwable.printStackTrace(writer: PrintWriter): Unit {
    val jlt = this as java.lang.Throwable
    jlt.printStackTrace(writer)
}

/**
 * Allows a stack trace to be printed from Kotlin's [[Throwable]]
 */
public inline fun Throwable.printStackTrace(stream: PrintStream): Unit {
    val jlt = this as java.lang.Throwable
    jlt.printStackTrace(stream)
}

