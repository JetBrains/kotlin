/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.isFrozen
import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.ExportTypeInfo
import kotlin.native.internal.NativePtrArray

/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
@ExportTypeInfo("theThrowableTypeInfo")
public open class Throwable(open val message: String?, open val cause: Throwable?) {

    constructor(message: String?) : this(message, null)

    constructor(cause: Throwable?) : this(cause?.toString(), cause)

    constructor() : this(null, null)

    @get:ExportForCppRuntime("Kotlin_Throwable_getStackTrace")
    private val stackTrace = getCurrentStackTrace()

    private val stackTraceStrings: Array<String> by lazy {
        getStackTraceStrings(stackTrace).freeze()
    }

    /**
     * Returns an array of stack trace strings representing the stack trace
     * pertaining to this throwable.
     */
    public fun getStackTrace(): Array<String> = stackTraceStrings

    internal fun getStackTraceAddressesInternal(): List<Long> =
            (0 until stackTrace.size).map { index -> stackTrace[index].toLong() }

    /**
     * Prints the [detailed description][Throwable.stackTraceToString] of this throwable to the standard output.
     */
    public fun printStackTrace(): Unit = dumpStackTrace("", "", { println(it) }, mutableSetOf())

    internal fun dumpStackTrace(): String = buildString {
        dumpStackTrace("", "", { appendln(it) }, mutableSetOf())
    }

    private fun Throwable.dumpStackTrace(indent: String, qualifier: String, writeln: (String) -> Unit, visited: MutableSet<Throwable>) {
        this.dumpSelfTrace(indent, qualifier, writeln, visited) || return

        var cause = this.cause
        while (cause != null) {
            // TODO: should skip common stack frames
            cause.dumpSelfTrace(indent, "Caused by: ", writeln, visited)
            cause = cause.cause
        }
    }

    private fun Throwable.dumpSelfTrace(indent: String, qualifier: String, writeln: (String) -> Unit, visited: MutableSet<Throwable>): Boolean {
        if (!visited.add(this)) {
            writeln(indent + qualifier + "[CIRCULAR REFERENCE, SEE ABOVE: $this]")
            return false
        }
        writeln(indent + qualifier + this.toString())
        for (element in stackTraceStrings) {
            writeln("$indent\tat $element")
        }
        val suppressed = suppressedExceptionsList
        if (!suppressed.isNullOrEmpty()) {
            val suppressedIndent = indent + '\t'
            for (s in suppressed) {
                s.dumpStackTrace(suppressedIndent, "Suppressed: ", writeln, visited)
            }
        }
        return true
    }


    /**
     * Returns the short description of this throwable consisting of
     * the exception class name (fully qualified if possible)
     * followed by the exception message if it is not null.
     */
    public override fun toString(): String {
        val kClass = this::class
        val s = kClass.qualifiedName ?: kClass.simpleName ?: "Throwable"
        return if (message != null) s + ": " + message.toString() else s
    }

    internal var suppressedExceptionsList: MutableList<Throwable>? = null
}

@SymbolName("Kotlin_getCurrentStackTrace")
private external fun getCurrentStackTrace(): NativePtrArray

@SymbolName("Kotlin_getStackTraceStrings")
private external fun getStackTraceStrings(stackTrace: NativePtrArray): Array<String>

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
public actual fun Throwable.stackTraceToString(): String = dumpStackTrace()

/**
 * Adds the specified exception to the list of exceptions that were
 * suppressed in order to deliver this exception.
 *
 * Does nothing if this [Throwable] is frozen.
 */
@SinceKotlin("1.4")
public actual fun Throwable.addSuppressed(exception: Throwable) {
    if (this !== exception && !this.isFrozen) {
        val suppressed = suppressedExceptionsList
        when {
            suppressed == null -> suppressedExceptionsList = mutableListOf<Throwable>(exception)
            suppressed.isFrozen -> suppressedExceptionsList = suppressed.toMutableList().apply { add(exception) }
            else -> suppressed.add(exception)
        }
    }
}

/**
 * Returns a list of all exceptions that were suppressed in order to deliver this exception.
 */
@SinceKotlin("1.4")
public actual val Throwable.suppressedExceptions: List<Throwable> get() {
    return this.suppressedExceptionsList ?: emptyList()
}
