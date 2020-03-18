/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

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
public actual fun Throwable.toStringWithTrace(): String = buildString { dumpStackTraceTo("", "", this) }

/**
 * Adds the specified exception to the list of exceptions that were
 * suppressed in order to deliver this exception.
 */
@SinceKotlin("1.4")
public actual fun Throwable.addSuppressed(exception: Throwable) {
    if (this !== exception) {
        val suppressed = this.asDynamic()._suppressed.unsafeCast<MutableList<Throwable>?>()
        if (suppressed == null) {
            this.asDynamic()._suppressed = mutableListOf(exception)
        } else {
            suppressed.add(exception)
        }
    }
}

/**
 * Returns a list of all exceptions that were suppressed in order to deliver this exception.
 */
@SinceKotlin("1.4")
public actual val Throwable.suppressedExceptions: List<Throwable>
    get() {
        return this.asDynamic()._suppressed?.unsafeCast<List<Throwable>>() ?: emptyList()
    }


private fun Throwable.dumpStackTraceTo(indent: String, qualifier: String, target: StringBuilder) {
    this.dumpSelfTrace(indent, qualifier, target)

    var cause = this.cause
    while (cause != null) {
        // TODO: should skip common stack frames
        cause.dumpSelfTrace(indent, "Caused by: ", target)
        cause = cause.cause
    }
}

private fun Throwable.dumpSelfTrace(indent: String, qualifier: String, target: StringBuilder) {
    target.append(indent).append(qualifier)
    val stack = this.asDynamic().stack as String?
    if (stack != null) {
        if (indent.isNotEmpty()) {
            // indent stack, but avoid indenting exception message lines
            val messageLines = 1 + (message?.count { c -> c == '\n' } ?: 0)
            stack.lineSequence().forEachIndexed { index: Int, line: String ->
                if (index >= messageLines) target.append(indent)
                target.append(line).append("\n")
            }
        } else {
            target.append(stack).append("\n")
        }
    } else {
        target.append(this.toString()).append("\n")
    }

    val suppressed = suppressedExceptions
    if (suppressed.isNotEmpty()) {
        val suppressedIndent = indent + '\t'
        for (s in suppressed) {
            s.dumpStackTraceTo(suppressedIndent, "Suppressed: ", target)
        }
    }
}
