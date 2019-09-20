/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

import kotlin.native.concurrent.freeze
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
     * Prints the stack trace of this throwable to the standard output.
     */
    public fun printStackTrace(): Unit = dumpStackTrace { println(it) }

    internal fun dumpStackTrace(): String = buildString {
        dumpStackTrace { appendln(it) }
    }

    private inline fun writeStackTraceElements(throwable: Throwable, writeln: (String) -> Unit) {
        for (element in throwable.stackTraceStrings) {
            writeln("        at $element")
        }
    }

    private inline fun dumpStackTrace(crossinline writeln: (String) -> Unit) {
        writeln(this@Throwable.toString())

        writeStackTraceElements(this, writeln)

        var cause = this.cause
        while (cause != null) {
            // TODO: should skip common stack frames
            writeln("Caused by: $cause")
            writeStackTraceElements(cause, writeln)
            cause = cause.cause
        }
    }

    public override fun toString(): String {
        val kClass = this::class
        val s = kClass.qualifiedName ?: kClass.simpleName ?: "Throwable"
        return if (message != null) s + ": " + message.toString() else s
    }
}

@SymbolName("Kotlin_getCurrentStackTrace")
private external fun getCurrentStackTrace(): NativePtrArray

@SymbolName("Kotlin_getStackTraceStrings")
private external fun getStackTraceStrings(stackTrace: NativePtrArray): Array<String>
