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
public actual fun Throwable.toStringWithTrace(): String = ExceptionTraceBuilder().buildFor(this)

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


private class ExceptionTraceBuilder {
    private val target = StringBuilder()
    private val visited = arrayOf<Throwable>()
    private var topStack: String = ""
    private var topStackStart: Int = 0

    fun buildFor(exception: Throwable): String {
        exception.dumpFullTrace("", "")
        return target.toString()
    }

    private fun hasSeen(exception: Throwable): Boolean = visited.any { it === exception }

    private fun Throwable.dumpFullTrace(indent: String, qualifier: String) {
        this.dumpSelfTrace(indent, qualifier) || return

        var cause = this.cause
        while (cause != null) {
            cause.dumpSelfTrace(indent, "Caused by: ") || return
            cause = cause.cause
        }
    }

    private fun Throwable.dumpSelfTrace(indent: String, qualifier: String): Boolean {
        target.append(indent).append(qualifier)
        if (hasSeen(this)) {
            target.append("[CIRCULAR REFERENCE, SEE ABOVE: ").append(this).append("]\n")
            return false
        }
        visited.asDynamic().push(this)

        var stack = this.asDynamic().stack as String?
        if (stack != null) {
            if (topStack.isEmpty()) {
                topStack = stack
                topStackStart = this.toString().length
            } else {
                stack = dropCommonFrames(stack, this.toString().length)
            }
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
            val suppressedIndent = indent + "    "
            for (s in suppressed) {
                s.dumpFullTrace(suppressedIndent, "Suppressed: ")
            }
        }
        return true
    }

    private fun dropCommonFrames(stack: String, stackStart: Int): String {
        var commonFrames: Int = 0
        var lastBreak: Int = 0
        var preLastBreak: Int = 0
        for (pos in 0 until minOf(topStack.length - topStackStart, stack.length - stackStart)) {
            val c = stack[stack.lastIndex - pos]
            if (c != topStack[topStack.lastIndex - pos]) break
            if (c == '\n') {
                commonFrames += 1
                preLastBreak = lastBreak
                lastBreak = pos
            }
        }
        if (commonFrames <= 1) return stack
        while (preLastBreak > 0 && stack[stack.lastIndex - (preLastBreak - 1)] == ' ')
            preLastBreak -= 1

        // leave 1 common frame to ease matching with the top exception stack
        return stack.dropLast(preLastBreak) + "... and ${commonFrames - 1} more common stack frames skipped"
    }
}